package com.github.guidohu.expensetracker

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.github.guidohu.expensetracker.data.Category
import com.github.guidohu.expensetracker.data.ExpenseRepository
import com.github.guidohu.expensetracker.data.ExpenseWithCategory
import com.github.guidohu.expensetracker.ui.categories.CategoriesScreen
import com.github.guidohu.expensetracker.ui.expenses.AddExpenseSheetContent
import com.github.guidohu.expensetracker.ui.expenses.ExpensesScreen
import com.github.guidohu.expensetracker.ui.stats.StatsScreen
import com.github.guidohu.expensetracker.ui.theme.ExpenseTrackerTheme
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

private val food = Category(1, "Food", 0xFFFF7043.toInt())
private val transport = Category(2, "Transport", 0xFF42A5F5.toInt())
private val shopping = Category(3, "Shopping", 0xFFAB47BC.toInt())
private val bills = Category(4, "Bills", 0xFF5C6BC0.toInt())
private val entertainment = Category(5, "Entertainment", 0xFFEC407A.toInt())
private val sampleCategories = listOf(food, transport, shopping, bills, entertainment)

private fun expense(id: Long, category: Category, amount: Double, note: String, daysAgo: Long): ExpenseWithCategory =
    ExpenseWithCategory(
        id = id,
        amount = amount,
        note = note,
        date = LocalDate.now().minusDays(daysAgo).toEpochDay(),
        categoryId = category.id,
        categoryName = category.name,
        categoryColor = category.color,
    )

private fun monthExpense(id: Long, category: Category, amount: Double, monthsAgo: Long): ExpenseWithCategory =
    ExpenseWithCategory(
        id = id,
        amount = amount,
        note = "",
        date = LocalDate.now().minusMonths(monthsAgo).withDayOfMonth(10).toEpochDay(),
        categoryId = category.id,
        categoryName = category.name,
        categoryColor = category.color,
    )

private val sampleExpenses = listOf(
    expense(1, food, 12.50, "Lunch", 0),
    expense(2, transport, 4.20, "Bus ticket", 0),
    expense(3, food, 38.90, "Groceries", 1),
    expense(4, entertainment, 15.00, "Movie", 1),
    expense(5, shopping, 64.00, "New shoes", 3),
    expense(6, bills, 89.00, "Phone bill", 5),
    expense(7, food, 22.30, "Dinner out", 6),
) + (1..5L).flatMap { monthsAgo ->
    listOf(
        monthExpense(100 + monthsAgo, food, 180.0 + monthsAgo * 15, monthsAgo),
        monthExpense(200 + monthsAgo, transport, 60.0 + monthsAgo * 5, monthsAgo),
        monthExpense(300 + monthsAgo, bills, 95.0, monthsAgo),
    )
}

private fun repositoryOf(categories: List<Category>, expenses: List<ExpenseWithCategory>): ExpenseRepository =
    ExpenseRepository(FakeCategoryDao(categories), FakeExpenseDao(expenses))

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
