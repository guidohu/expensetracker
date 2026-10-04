package com.github.guidohu.expensetracker.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.guidohu.expensetracker.data.ExpenseRepository
import com.github.guidohu.expensetracker.data.ExpenseWithCategory
import com.github.guidohu.expensetracker.data.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

data class CategoryTotal(
    val categoryId: Long,
    val name: String,
    val color: Int,
    val total: Double,
    val fraction: Float,
)

data class MonthTotal(
    val yearMonth: YearMonth,
    val label: String,
    val total: Double,
)

data class StatsUiState(
    val defaultCurrency: String = "USD",
    val totalThisMonth: Double = 0.0,
    val totalAllTime: Double = 0.0,
    val categoryTotalsThisMonth: List<CategoryTotal> = emptyList(),
    val monthlyTotals: List<MonthTotal> = emptyList(),
    val monthlyBudget: Double? = null,
    val isEmpty: Boolean = true,
)

class StatsViewModel(
    repository: ExpenseRepository,
    userPreferences: UserPreferences,
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = combine(
        repository.expenses, userPreferences.defaultCurrency, userPreferences.monthlyBudget,
    ) { expenses, defaultCurrency, budget ->
        buildUiState(expenses, defaultCurrency, budget)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

    private fun buildUiState(expenses: List<ExpenseWithCategory>, defaultCurrency: String, budget: Double?): StatsUiState {
        if (expenses.isEmpty()) return StatsUiState(defaultCurrency = defaultCurrency, monthlyBudget = budget)

        val currentMonth = YearMonth.now()
        val thisMonthExpenses = expenses.filter {
            YearMonth.from(LocalDate.ofEpochDay(it.date)) == currentMonth
        }

        val totalThisMonth = thisMonthExpenses.sumOf { it.amountInDefaultCurrency }
        val totalAllTime = expenses.sumOf { it.amountInDefaultCurrency }

        val categoryTotals = thisMonthExpenses
            .groupBy { it.categoryId }
            .map { (categoryId, items) ->
                val total = items.sumOf { it.amountInDefaultCurrency }
                CategoryTotal(
                    categoryId = categoryId,
                    name = items.first().categoryName,
                    color = items.first().categoryColor,
                    total = total,
                    fraction = if (totalThisMonth > 0) (total / totalThisMonth).toFloat() else 0f,
                )
            }
            .sortedByDescending { it.total }

        val months = (5 downTo 0).map { currentMonth.minusMonths(it.toLong()) }
        val monthlyTotals = months.map { ym ->
            val total = expenses.filter { YearMonth.from(LocalDate.ofEpochDay(it.date)) == ym }
                .sumOf { it.amountInDefaultCurrency }
            MonthTotal(
                yearMonth = ym,
                label = ym.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                total = total,
            )
        }

        return StatsUiState(
            defaultCurrency = defaultCurrency,
            totalThisMonth = totalThisMonth,
            totalAllTime = totalAllTime,
            categoryTotalsThisMonth = categoryTotals,
            monthlyTotals = monthlyTotals,
            monthlyBudget = budget,
            isEmpty = false,
        )
    }
}
