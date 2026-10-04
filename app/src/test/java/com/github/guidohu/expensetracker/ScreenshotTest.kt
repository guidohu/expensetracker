package com.github.guidohu.expensetracker

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.github.guidohu.expensetracker.ui.categories.CategoriesScreen
import com.github.guidohu.expensetracker.ui.expenses.AddExpenseSheetContent
import com.github.guidohu.expensetracker.ui.expenses.ExpensesScreen
import com.github.guidohu.expensetracker.ui.stats.StatsScreen
import com.github.guidohu.expensetracker.ui.theme.ExpenseTrackerTheme
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class ScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6,
        theme = "android:Theme.Material.Light.NoActionBar",
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
        ExpensesScreen(repositoryOf(sampleCategories, sampleExpenses))
    }

    @Test
    fun expenses_populated_dark() = snapshot(darkTheme = true) {
        ExpensesScreen(repositoryOf(sampleCategories, sampleExpenses))
    }

    @Test
    fun expenses_emptyNoExpenses() = snapshot {
        ExpensesScreen(repositoryOf(sampleCategories, emptyList()))
    }

    @Test
    fun expenses_emptyNoCategories() = snapshot {
        ExpensesScreen(repositoryOf(emptyList(), emptyList()))
    }

    @Test
    fun categories_populated() = snapshot {
        CategoriesScreen(repositoryOf(sampleCategories, sampleExpenses))
    }

    @Test
    fun categories_empty() = snapshot {
        CategoriesScreen(repositoryOf(emptyList(), emptyList()))
    }

    @Test
    fun stats_populated() = snapshot {
        StatsScreen(repositoryOf(sampleCategories, sampleExpenses))
    }

    @Test
    fun stats_populated_dark() = snapshot(darkTheme = true) {
        StatsScreen(repositoryOf(sampleCategories, sampleExpenses))
    }

    @Test
    fun stats_empty() = snapshot {
        StatsScreen(repositoryOf(sampleCategories, emptyList()))
    }

    @Test
    fun addExpenseSheetContent() = snapshot {
        AddExpenseSheetContent(
            amountText = "24.50",
            onAmountChange = {},
            categories = sampleCategories,
            selectedCategoryId = food.id,
            onCategorySelect = {},
            note = "Lunch with Sam",
            onNoteChange = {},
            selectedDate = LocalDate.now().toEpochDay(),
            onDateClick = {},
            isValid = true,
            onSave = {},
        )
    }
}
