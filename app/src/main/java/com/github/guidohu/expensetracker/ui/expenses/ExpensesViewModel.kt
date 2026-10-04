package com.github.guidohu.expensetracker.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.guidohu.expensetracker.data.Category
import com.github.guidohu.expensetracker.data.ExchangeRateService
import com.github.guidohu.expensetracker.data.ExpenseRepository
import com.github.guidohu.expensetracker.data.ExpenseWithCategory
import com.github.guidohu.expensetracker.data.UserPreferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class ExpensesViewModel(
    private val repository: ExpenseRepository,
    private val userPreferences: UserPreferences,
    private val exchangeRateService: ExchangeRateService,
) : ViewModel() {

    val expenses: StateFlow<List<ExpenseWithCategory>> = repository.expenses.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val categories: StateFlow<List<Category>> = repository.categories.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val defaultCurrency: StateFlow<String> = userPreferences.defaultCurrency

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _events = Channel<String>(Channel.BUFFERED)
    /** One-shot UI messages (snackbar text) — e.g. when an exchange-rate lookup fails. */
    val events: Flow<String> = _events.receiveAsFlow()

    /** Adds a new expense, looking up the historical exchange rate if its currency differs from the default. */
    fun addExpense(
        amount: Double,
        currencyCode: String,
        categoryId: Long,
        title: String,
        notes: String,
        date: Long,
        onComplete: () -> Unit,
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val rate = resolveExchangeRate(currencyCode, date)
            repository.addExpense(amount, currencyCode, rate, categoryId, title.trim(), notes.trim(), date)
            _isSaving.value = false
            onComplete()
        }
    }

    /** Updates an existing expense in place, re-resolving the exchange rate (cheap, and correct if the
     * currency, date, or the app's default currency changed since the expense was first entered). */
    fun updateExpense(
        id: Long,
        amount: Double,
        currencyCode: String,
        categoryId: Long,
        title: String,
        notes: String,
        date: Long,
        onComplete: () -> Unit,
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val rate = resolveExchangeRate(currencyCode, date)
            repository.updateExpense(id, amount, currencyCode, rate, categoryId, title.trim(), notes.trim(), date)
            _isSaving.value = false
            onComplete()
        }
    }

    private suspend fun resolveExchangeRate(currencyCode: String, date: Long): Double {
        val default = userPreferences.defaultCurrency.value
        if (currencyCode == default) return 1.0
        val fetched = exchangeRateService.fetchRate(currencyCode, default, LocalDate.ofEpochDay(date))
        if (fetched == null) {
            _events.send("Couldn't look up the exchange rate — saved at a 1:1 rate for now.")
        }
        return fetched ?: 1.0
    }

    fun deleteExpense(expense: ExpenseWithCategory) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    /** Re-inserts a just-deleted expense exactly as it was (used by the delete Undo action) — no rate re-lookup. */
    fun restoreExpense(expense: ExpenseWithCategory) {
        viewModelScope.launch {
            repository.addExpense(
                amount = expense.amount,
                currencyCode = expense.currencyCode,
                exchangeRate = expense.exchangeRate,
                categoryId = expense.categoryId,
                title = expense.title,
                notes = expense.notes,
                date = expense.date,
            )
        }
    }
}
