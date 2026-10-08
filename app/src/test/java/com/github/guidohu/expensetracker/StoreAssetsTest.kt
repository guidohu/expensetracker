package com.github.guidohu.expensetracker

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.github.guidohu.expensetracker.data.AppContainer
import com.github.guidohu.expensetracker.data.ExchangeRateService
import com.github.guidohu.expensetracker.data.UrlPreviewService
import com.github.guidohu.expensetracker.data.UserPreferences
import com.github.guidohu.expensetracker.ui.categories.CategoriesScreen
import com.github.guidohu.expensetracker.ui.expenses.AddExpenseSheetContent
import com.github.guidohu.expensetracker.ui.expenses.ExpensesScreen
import com.github.guidohu.expensetracker.ui.settings.SettingsScreen
import com.github.guidohu.expensetracker.ui.stats.StatsScreen
import com.github.guidohu.expensetracker.ui.theme.ExpenseTrackerTheme
import com.github.guidohu.expensetracker.ui.wishlist.WishlistScreen
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Renders Play Store listing assets (icon, phone screenshots) at real resolutions without
 * transparency, separate from [ScreenshotTest]'s small renders used for day-to-day UI verification.
 * See the known-limitation note on [ScreenshotTest] — currently fails against compileSdk 36.
 * The assets already produced in play-store-assets/ remain valid; rerun once Paparazzi catches up.
 */
class StoreAssetsTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig(
            screenWidth = 1080,
            screenHeight = 2340,
            xdpi = 480,
            ydpi = 480,
            density = Density.XXHIGH,
        ),
        theme = "android:Theme.Material.Light.NoActionBar",
    )

    private fun container(wishlistItems: List<com.github.guidohu.expensetracker.data.WishlistItem> = emptyList()) = UserPreferences(paparazzi.context).let { prefs ->
        AppContainer(
            repository = repositoryOf(sampleCategories, sampleExpenses),
            wishlistRepository = wishlistRepositoryOf(wishlistItems),
            userPreferences = prefs,
            exchangeRateService = ExchangeRateService(),
            urlPreviewService = UrlPreviewService(),
            backupManager = backupManagerOf(prefs),
        )
    }

    private fun screenshot(content: @androidx.compose.runtime.Composable () -> Unit) {
        val composeView = ComposeView(paparazzi.context).apply {
            setContent {
                val viewModelStoreOwner = remember { object : ViewModelStoreOwner {
                    override val viewModelStore = ViewModelStore()
                } }
                val activityResultRegistryOwner = remember { fakeActivityResultRegistryOwner() }
                CompositionLocalProvider(
                    LocalViewModelStoreOwner provides viewModelStoreOwner,
                    androidx.activity.compose.LocalActivityResultRegistryOwner provides activityResultRegistryOwner,
                ) {
                    ExpenseTrackerTheme(darkTheme = false, dynamicColor = false) {
                        androidx.compose.material3.Surface { content() }
                    }
                }
            }
        }
        paparazzi.snapshot(composeView)
    }

    @Test
    fun shot_expenses() = screenshot {
        ExpensesScreen(container())
    }

    @Test
    fun shot_stats() = screenshot {
        StatsScreen(container())
    }

    @Test
    fun shot_wishlist() = screenshot {
        WishlistScreen(container(sampleWishlistItems))
    }

    @Test
    fun shot_categories() = screenshot {
        CategoriesScreen(container())
    }

    @Test
    fun shot_settings() = screenshot {
        val c = container()
        c.userPreferences.setMonthlyBudget(500.0)
        SettingsScreen(c)
    }

    @Test
    fun shot_addExpense() = screenshot {
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

    @Test
    fun icon_playStore() {
        val composeView = ComposeView(paparazzi.context).apply {
            setContent {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0B6479))) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        paparazzi.unsafeUpdateConfig(
            deviceConfig = DeviceConfig(
                screenWidth = 512,
                screenHeight = 512,
                xdpi = 160,
                ydpi = 160,
                density = Density.MEDIUM,
            ),
        )
        paparazzi.snapshot(composeView)
    }
}
