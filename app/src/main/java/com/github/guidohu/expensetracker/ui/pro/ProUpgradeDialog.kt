package com.github.guidohu.expensetracker.ui.pro

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.guidohu.expensetracker.data.AppContainer
import com.github.guidohu.expensetracker.data.ProFeatures
import com.github.guidohu.expensetracker.data.ProPurchaseStatus
import com.github.guidohu.expensetracker.data.RestoreResult
import kotlinx.coroutines.launch

/**
 * The Pro pack upsell. [reason] says which limit the user just hit (null when opened from
 * Settings). For a Pro user it shows what's included and why they have it instead — which is also
 * what appears the moment a purchase (or a pending payment) completes while it's open.
 */
@Composable
fun ProUpgradeDialog(
    container: AppContainer,
    reason: String?,
    onDismiss: () -> Unit,
) {
    val purchases = container.proPurchases
    val isPro by container.userPreferences.isPro.collectAsState()
    val product by purchases.product.collectAsState()
    val status by purchases.status.collectAsState()
    val activity = LocalContext.current.findActivity()
    val scope = rememberCoroutineScope()
    var restoring by remember { mutableStateOf(false) }
    var restoreMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        purchases.clearStatus()
        if (product == null) purchases.refresh()
    }

    if (isPro) {
        val isProPurchased by container.userPreferences.isProPurchased.collectAsState()
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Filled.WorkspacePremium, contentDescription = null) },
            title = { Text("Pro unlocked") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        when {
                            isProPurchased -> "Thank you for supporting Expense Tracker! These are all yours:"
                            container.userPreferences.isLegacyPro ->
                                "You've used Expense Tracker since before Pro existed, so Pro is yours for free. Thank you for being here from the start!"
                            else -> "Pro is switched on for this debug build."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    ProFeatureList()
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.WorkspacePremium, contentDescription = null) },
        title = { Text("Expense Tracker Pro") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                reason?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                ProFeatureList()
                Text(
                    "One-time purchase. No subscription, no ads, and your data still never leaves your device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                when (val s = status) {
                    ProPurchaseStatus.Pending -> Text(
                        "Your payment is pending. Pro unlocks automatically once Google Play confirms it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    is ProPurchaseStatus.Failed -> Text(
                        s.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    ProPurchaseStatus.Idle -> Unit
                }
                restoreMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(
                    enabled = !restoring,
                    onClick = {
                        restoring = true
                        restoreMessage = null
                        scope.launch {
                            restoreMessage = when (purchases.restore()) {
                                RestoreResult.RESTORED -> null
                                RestoreResult.NOT_FOUND -> "No previous Pro purchase found for this Google account."
                                RestoreResult.FAILED -> "Couldn't reach Google Play. Check your connection and try again."
                            }
                            restoring = false
                        }
                    },
                    modifier = Modifier.padding(top = 4.dp),
                ) { Text(if (restoring) "Checking…" else "Restore purchase") }
            }
        },
        confirmButton = {
            Button(
                enabled = activity != null && status != ProPurchaseStatus.Pending,
                onClick = { activity?.let(purchases::purchase) },
            ) {
                Text(product?.let { "Unlock for ${it.formattedPrice}" } ?: "Unlock Pro")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } },
    )
}

@Composable
private fun ProFeatureList() {
    ProFeatures.forEach { feature ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Text(feature, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

/**
 * A tappable "N of M used" hint for free users, shown at the end of a limited list so the limit
 * isn't a surprise. Hidden for Pro users by the caller.
 */
@Composable
fun FreeLimitBanner(used: Int, limit: Int, noun: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.WorkspacePremium,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    "${minOf(used, limit)} of $limit free $noun used",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    "Unlock Pro for unlimited $noun",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
