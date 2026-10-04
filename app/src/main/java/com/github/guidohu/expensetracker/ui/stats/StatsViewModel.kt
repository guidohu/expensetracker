package com.github.guidohu.expensetracker.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.guidohu.expensetracker.data.ExpenseRepository
import com.github.guidohu.expensetracker.data.ExpenseWithCategory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
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
    val totalThisMonth: Double = 0.0,
    val totalAllTime: Double = 0.0,
    val categoryTotalsThisMonth: List<CategoryTotal> = emptyList(),
    val monthlyTotals: List<MonthTotal> = emptyList(),
    val isEmpty: Boolean = true,
)

class StatsViewModel(repository: ExpenseRepository) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = repository.expenses
        .map { expenses -> buildUiState(expenses) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

    private fun buildUiState(expenses: List<ExpenseWithCategory>): StatsUiState {
        if (expenses.isEmpty()) return StatsUiState()

        val currentMonth = YearMonth.now()
        val thisMonthExpenses = expenses.filter {
            YearMonth.from(LocalDate.ofEpochDay(it.date)) == currentMonth
        }

        val totalThisMonth = thisMonthExpenses.sumOf { it.amount }
        val totalAllTime = expenses.sumOf { it.amount }

        val categoryTotals = thisMonthExpenses
            .groupBy { it.categoryId }
            .map { (categoryId, items) ->
                val total = items.sumOf { it.amount }
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
                .sumOf { it.amount }
            MonthTotal(
                yearMonth = ym,
                label = ym.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                total = total,
            )
        }

        return StatsUiState(
            totalThisMonth = totalThisMonth,
            totalAllTime = totalAllTime,
            categoryTotalsThisMonth = categoryTotals,
            monthlyTotals = monthlyTotals,
            isEmpty = false,
        )
    }
}
