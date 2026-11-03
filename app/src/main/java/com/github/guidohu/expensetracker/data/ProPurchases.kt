package com.github.guidohu.expensetracker.data

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** The Pro pack as Google Play offers it to this user — price already localized to their store country. */
data class ProProduct(val formattedPrice: String)

sealed interface ProPurchaseStatus {
    data object Idle : ProPurchaseStatus
    /** Paid with a delayed method (e.g. cash at a store) — Pro unlocks once Play confirms payment. */
    data object Pending : ProPurchaseStatus
    data class Failed(val message: String) : ProPurchaseStatus
}

enum class RestoreResult { RESTORED, NOT_FOUND, FAILED }

/**
 * Buying and restoring the Pro pack. The entitlement itself lives in [UserPreferences.isPro] —
 * this only talks to Google Play and writes the outcome there.
 */
interface ProPurchases {
    /** Null until Play has answered with the product (or forever, if Play isn't available). */
    val product: StateFlow<ProProduct?>
    val status: StateFlow<ProPurchaseStatus>

    fun purchase(activity: Activity)

    /** Re-reads owned purchases from Play: restores Pro on a reinstall or new device, revokes it after a refund. */
    fun refresh()

    suspend fun restore(): RestoreResult

    fun clearStatus()
}

/** For previews and screenshot tests, where there's no Google Play to talk to. */
object UnavailableProPurchases : ProPurchases {
    override val product: StateFlow<ProProduct?> = MutableStateFlow(null)
    override val status: StateFlow<ProPurchaseStatus> = MutableStateFlow(ProPurchaseStatus.Idle)
    override fun purchase(activity: Activity) {}
    override fun refresh() {}
    override suspend fun restore() = RestoreResult.FAILED
    override fun clearStatus() {}
}

/**
 * Google Play Billing implementation. Purchases are verified on-device only — there's no backend
 * to verify against, and adding one would break the app's "nothing leaves your device" promise.
 */
class PlayBillingProPurchases(
    context: Context,
    private val userPreferences: UserPreferences,
    private val scope: CoroutineScope,
) : ProPurchases, PurchasesUpdatedListener {

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        // Re-establishes the Play connection on demand if a call is made while it's dropped.
        .enableAutoServiceReconnection()
        .build()

    @Volatile private var productDetails: ProductDetails? = null
    /** Completed once the first connection attempt settles, so early callers can wait for it. */
    private val connected = CompletableDeferred<Boolean>()

    private val _product = MutableStateFlow<ProProduct?>(null)
    override val product: StateFlow<ProProduct?> = _product

    private val _status = MutableStateFlow<ProPurchaseStatus>(ProPurchaseStatus.Idle)
    override val status: StateFlow<ProPurchaseStatus> = _status

    /** Connects to Play and syncs product + owned purchases. Call once, at app start. */
    fun start() {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                val ok = result.responseCode == BillingResponseCode.OK
                connected.complete(ok)
                if (ok) refresh()
            }

            // enableAutoServiceReconnection reconnects on the next call; nothing to do here.
            override fun onBillingServiceDisconnected() {}
        })
    }

    override fun refresh() {
        scope.launch { sync() }
    }

    override suspend fun restore(): RestoreResult {
        if (!sync()) return RestoreResult.FAILED
        return if (userPreferences.isProPurchased.value) RestoreResult.RESTORED else RestoreResult.NOT_FOUND
    }

    override fun clearStatus() {
        if (_status.value is ProPurchaseStatus.Failed) _status.value = ProPurchaseStatus.Idle
    }

    override fun purchase(activity: Activity) {
        val details = productDetails
        if (details == null) {
            _status.value = ProPurchaseStatus.Failed(
                "The Pro pack isn't available right now. Check that you're signed in to Google Play and online, then try again."
            )
            refresh()
            return
        }
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .apply { details.oneTimePurchaseOfferDetailsList?.firstOrNull()?.offerToken?.let { setOfferToken(it) } }
            .build()
        val result = client.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(productParams)).build(),
        )
        if (result.responseCode == BillingResponseCode.ITEM_ALREADY_OWNED) {
            refresh()
        } else if (result.responseCode != BillingResponseCode.OK) {
            _status.value = ProPurchaseStatus.Failed(messageFor(result.responseCode))
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingResponseCode.OK -> scope.launch { handlePurchases(purchases.orEmpty(), isFullList = false) }
            BillingResponseCode.USER_CANCELED -> _status.value = ProPurchaseStatus.Idle
            // Bought before (e.g. on another device) — fetching owned purchases restores it.
            BillingResponseCode.ITEM_ALREADY_OWNED -> refresh()
            else -> _status.value = ProPurchaseStatus.Failed(messageFor(result.responseCode))
        }
    }

    /** Loads the product and owned purchases. Returns false if Play couldn't be reached. */
    private suspend fun sync(): Boolean {
        // Calls made before the first connection settles would just fail, so wait briefly for it.
        // After that, auto-reconnection retries a dropped connection on each call by itself.
        withTimeoutOrNull(10_000) { connected.await() }

        if (productDetails == null) {
            val productResult = client.queryProductDetails(
                QueryProductDetailsParams.newBuilder()
                    .setProductList(
                        listOf(
                            QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(PRO_PRODUCT_ID)
                                .setProductType(BillingClient.ProductType.INAPP)
                                .build()
                        )
                    )
                    .build()
            )
            if (productResult.billingResult.responseCode == BillingResponseCode.OK) {
                val details = productResult.productDetailsList?.firstOrNull { it.productId == PRO_PRODUCT_ID }
                productDetails = details
                _product.value = details?.oneTimePurchaseOfferDetails?.formattedPrice?.let(::ProProduct)
            }
        }

        val purchasesResult = client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        )
        // Offline or Play unavailable: keep the cached entitlement rather than revoking Pro.
        if (purchasesResult.billingResult.responseCode != BillingResponseCode.OK) return false
        handlePurchases(purchasesResult.purchasesList, isFullList = true)
        return true
    }

    /**
     * [isFullList] is true for the complete set of owned purchases (so a missing Pro pack means it
     * was refunded), false for a purchase-update callback, which only reports what just changed.
     */
    private suspend fun handlePurchases(purchases: List<Purchase>, isFullList: Boolean) {
        val pro = purchases.filter { PRO_PRODUCT_ID in it.products }
        val owned = pro.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        if (owned || isFullList) userPreferences.setProPurchased(owned)

        _status.value = when {
            owned -> ProPurchaseStatus.Idle
            pro.any { it.purchaseState == Purchase.PurchaseState.PENDING } -> ProPurchaseStatus.Pending
            _status.value == ProPurchaseStatus.Pending && isFullList -> ProPurchaseStatus.Idle
            else -> _status.value
        }

        // Play refunds a purchase that isn't acknowledged within 3 days. A failed attempt is
        // retried on the next sync, which runs at every app start and resume.
        pro.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }.forEach {
            client.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(it.purchaseToken).build()
            )
        }
    }

    private fun messageFor(responseCode: Int): String = when (responseCode) {
        BillingResponseCode.SERVICE_UNAVAILABLE,
        BillingResponseCode.SERVICE_DISCONNECTED,
        BillingResponseCode.NETWORK_ERROR -> "Couldn't reach Google Play. Check your connection and try again."
        BillingResponseCode.BILLING_UNAVAILABLE -> "Purchases aren't available on this device or Google account."
        BillingResponseCode.ITEM_UNAVAILABLE -> "The Pro pack isn't available in your country yet."
        else -> "The purchase didn't go through (error $responseCode). Please try again."
    }
}
