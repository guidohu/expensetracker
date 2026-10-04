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
import com.github.guidohu.expensetracker.data.UserPreferences
import com.github.guidohu.expensetracker.ui.categories.CategoriesScreen
import com.github.guidohu.expensetracker.ui.expenses.AddExpenseSheetContent
import com.github.guidohu.expensetracker.ui.expenses.ExpensesScreen
import com.github.guidohu.expensetracker.ui.stats.StatsScreen
import com.github.guidohu.expensetracker.ui.theme.ExpenseTrackerTheme
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * KNOWN LIMITATION: Paparazzi 1.3.4 doesn't yet recognize compileSdk 36's platform resources
 * (fails with `UninitializedPropertyAccessException: sessionParamsBuilder`). Paparazzi 2.0.0-alpha
 * fixes this but requires JDK 21 and a newer AGP than this project's 8.7.2 — upgrading both cascaded
 * into more risk than was worth taking on here. These tests will fail until either Paparazzi ships a
 * stable compileSdk-36-compatible release, or compileSdk/AGP are deliberately upgraded together.
 * This does not affect the actual app build (assembleRelease/bundleRelease are unaffected).
 */
class ScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6,
        theme = "android:Theme.Material.Light.NoActionBar",
    )

    private fun containerOf(categories: List<Category>, expenses: List<ExpenseWithCategory>): AppContainer =
        AppContainer(
            repository = repositoryOf(categories, expenses),
            userPreferences = UserPreferences(paparazzi.context),
            exchangeRateService = ExchangeRateService(),
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
                CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
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
