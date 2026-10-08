package com.github.guidohu.expensetracker.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.guidohu.expensetracker.data.ExpenseRepository
import com.github.guidohu.expensetracker.data.ExpenseWithCategory
import com.github.guidohu.expensetracker.data.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

data class StatsUiState(
    val defaultCurrency: String = "USD",
    val totalThisMonth: Double = 0.0,
    val totalAllTime: Double = 0.0,
    val historic: HistoricStats? = null,
    val monthlyBudget: Double? = null,
    val isEmpty: Boolean = true,
)

class StatsViewModel(
    repository: ExpenseRepository,
    userPreferences: UserPreferences,
) : ViewModel() {

    private val period = MutableStateFlow(StatsPeriod.THIS_MONTH)

    val uiState: StateFlow<StatsUiState> = combine(
        repository.expenses, userPreferences.defaultCurrency, userPreferences.monthlyBudget, period,
    ) { expenses, defaultCurrency, budget, selectedPeriod ->
        buildUiState(expenses, defaultCurrency, budget, selectedPeriod)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

    fun selectPeriod(newPeriod: StatsPeriod) {
        period.value = newPeriod
    }

    private fun buildUiState(
        expenses: List<ExpenseWithCategory>,
        defaultCurrency: String,
        budget: Double?,
        period: StatsPeriod,
    ): StatsUiState {
        if (expenses.isEmpty()) return StatsUiState(defaultCurrency = defaultCurrency, monthlyBudget = budget)

        val currentMonth = YearMonth.now()
        val totalThisMonth = expenses
            .filter { YearMonth.from(LocalDate.ofEpochDay(it.date)) == currentMonth }
            .sumOf { it.amountInDefaultCurrency }

        return StatsUiState(
            defaultCurrency = defaultCurrency,
            totalThisMonth = totalThisMonth,
            totalAllTime = expenses.sumOf { it.amountInDefaultCurrency },
            historic = buildHistoricStats(expenses, period),
            monthlyBudget = budget,
            isEmpty = false,
        )
    }
}
