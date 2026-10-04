package com.hungerbuehler.expensetracker.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hungerbuehler.expensetracker.data.Category
import com.hungerbuehler.expensetracker.data.ExpenseRepository
import com.hungerbuehler.expensetracker.data.ExpenseWithCategory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExpensesViewModel(private val repository: ExpenseRepository) : ViewModel() {

    val expenses: StateFlow<List<ExpenseWithCategory>> = repository.expenses.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val categories: StateFlow<List<Category>> = repository.categories.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    fun addExpense(amount: Double, categoryId: Long, note: String, date: Long) {
        viewModelScope.launch {
            repository.addExpense(amount, categoryId, note, date)
        }
    }

    fun deleteExpense(expense: ExpenseWithCategory) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }
}
