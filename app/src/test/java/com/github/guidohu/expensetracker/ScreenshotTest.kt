package com.github.guidohu.expensetracker

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.github.guidohu.expensetracker.data.AppContainer
import com.github.guidohu.expensetracker.data.Category
import com.github.guidohu.expensetracker.data.ExchangeRateService
import com.github.guidohu.expensetracker.data.ExpenseWithCategory
import com.github.guidohu.expensetracker.data.UrlPreviewService
import com.github.guidohu.expensetracker.data.UserPreferences
import com.github.guidohu.expensetracker.data.WishlistItem
import com.github.guidohu.expensetracker.ui.categories.CategoriesScreen
import com.github.guidohu.expensetracker.ui.expenses.AddExpenseSheetContent
import com.github.guidohu.expensetracker.ui.expenses.ExpensesScreen
import com.github.guidohu.expensetracker.ui.onboarding.OnboardingScreen
import com.github.guidohu.expensetracker.ui.settings.SettingsScreen
import com.github.guidohu.expensetracker.ui.stats.StatsScreen
import com.github.guidohu.expensetracker.ui.theme.ExpenseTrackerTheme
import com.github.guidohu.expensetracker.ui.wishlist.WishlistScreen
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * KNOWN LIMITATION: Paparazzi 1.3.4 doesn't recognize compileSdk 36's platform resources (fails
 * with `UninitializedPropertyAccessException: sessionParamsBuilder`). Paparazzi 2.0.0-alpha fixes
 * this but needs JDK 21 and a newer AGP than this project's 8.7.2 — not worth the cascading
 * upgrade. This does not affect the actual app build (assembleRelease/bundleRelease are fine at
 * compileSdk 36 always).
 *
 * To actually run/record these locally:
 * 1. `export ANDROID_HOME=<sdk root>` (and `ANDROID_SDK_ROOT`) before invoking Gradle — Paparazzi
 *    reads these env vars directly at test runtime, not Gradle's `local.properties`/`sdk.dir`.
 * 2. Temporarily set `compileSdk`/`targetSdk` to 35 (or whatever's installed) in app/build.gradle.kts.
 * 3. `./gradlew recordPaparazziDebug` to (re)write goldens under app/src/test/snapshots, or
 *    `./gradlew testDebugUnitTest`/`verifyPaparazziDebug` to just verify against existing ones.
 * 4. Revert compileSdk/targetSdk back to 36 before building a release.
 */
class ScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6,
        theme = "android:Theme.Material.Light.NoActionBar",
    )

    private fun containerOf(
        categories: List<Category>,
        expenses: List<ExpenseWithCategory>,
        wishlistItems: List<WishlistItem> = emptyList(),
    ): AppContainer =
        AppContainer(
            repository = repositoryOf(categories, expenses),
            wishlistRepository = wishlistRepositoryOf(wishlistItems),
            userPreferences = UserPreferences(paparazzi.context),
            exchangeRateService = ExchangeRateService(),
            urlPreviewService = UrlPreviewService(),
        )

    private fun snapshot(
        darkTheme: Boolean = false,
        offsetMillis: Long = 0,
        content: @androidx.compose.runtime.Composable () -> Unit,
    ) {
        val composeView = androidx.compose.ui.platform.ComposeView(paparazzi.context).apply {
            setContent {
                val viewModelStoreOwner = remember { object : ViewModelStoreOwner {
                    override val viewModelStore = ViewModelStore()
                } }
                val activityResultRegistryOwner = remember { fakeActivityResultRegistryOwner() }
                CompositionLocalProvider(
                    LocalViewModelStoreOwner provides viewModelStoreOwner,
                    androidx.activity.compose.LocalActivityResultRegistryOwner provides activityResultRegistryOwner,
                ) {
                    ExpenseTrackerTheme(darkTheme = darkTheme, dynamicColor = false) {
                        androidx.compose.material3.Surface { content() }
                    }
                }
            }
        }
        paparazzi.snapshot(composeView, offsetMillis = offsetMillis)
    }

    @Test
    fun expenses_populated() = snapshot {
        ExpensesScreen(containerOf(sampleCategories, sampleExpenses))
    }

    @Test
    fun expenses_populated_dark() = snapshot(darkTheme = true) {
        ExpensesScreen(containerOf(sampleCategories, sampleExpenses))
    }

    @Test
    fun expenses_emptyNoExpenses() = snapshot {
        ExpensesScreen(containerOf(sampleCategories, emptyList()))
    }

    @Test
    fun expenses_emptyNoCategories() = snapshot {
        ExpensesScreen(containerOf(emptyList(), emptyList()))
    }

    @Test
    fun wishlist_populated() = snapshot {
        WishlistScreen(containerOf(sampleCategories, sampleExpenses, sampleWishlistItems))
    }

    @Test
    fun wishlist_populated_dark() = snapshot(darkTheme = true) {
        WishlistScreen(containerOf(sampleCategories, sampleExpenses, sampleWishlistItems))
    }

    @Test
    fun wishlist_empty() = snapshot {
        WishlistScreen(containerOf(sampleCategories, sampleExpenses, emptyList()))
    }

    @Test
    fun categories_populated() = snapshot {
        CategoriesScreen(containerOf(sampleCategories, sampleExpenses))
    }

    @Test
    fun categories_empty() = snapshot {
        CategoriesScreen(containerOf(emptyList(), emptyList()))
    }

    @Test
    fun stats_populated() = snapshot {
        StatsScreen(containerOf(sampleCategories, sampleExpenses))
    }

    @Test
    fun stats_populated_dark() = snapshot(darkTheme = true) {
        StatsScreen(containerOf(sampleCategories, sampleExpenses))
    }

    @Test
    fun stats_empty() = snapshot {
        StatsScreen(containerOf(sampleCategories, emptyList()))
    }

    @Test
    fun stats_withBudget() = snapshot {
        val container = containerOf(sampleCategories, sampleExpenses)
        container.userPreferences.setMonthlyBudget(150.0)
        StatsScreen(container)
    }

    @Test
    fun settings_screen() = snapshot {
        val container = containerOf(sampleCategories, sampleExpenses)
        container.userPreferences.setMonthlyBudget(500.0)
        SettingsScreen(container)
    }

    @Test
    fun onboarding_welcome() = snapshot {
        OnboardingScreen(onComplete = {})
    }

    @Test
    fun onboarding_currency() = snapshot {
        com.github.guidohu.expensetracker.ui.onboarding.CurrencyStep(selected = "USD", onSelect = {}, onFinish = {})
    }

    @Test
    fun addExpenseSheetContent() = snapshot {
        AddExpenseSheetContent(
            amountText = "24.50",
            onAmountChange = {},
            currencyCode = "USD",
            onCurrencyClick = {},
            title = "Lunch with Sam",
            onTitleChange = {},
            categories = sampleCategories,
            selectedCategoryId = food.id,
            onCategorySelect = {},
            notes = "",
            onNotesChange = {},
            selectedDate = LocalDate.now().toEpochDay(),
            onDateClick = {},
            isValid = true,
            isSaving = false,
            onSave = {},
        )
    }
}
