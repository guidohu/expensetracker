package com.hungerbuehler.expensetracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hungerbuehler.expensetracker.data.ExpenseRepository

/** Simple factory for ViewModels that take only the repository — avoids pulling in a DI framework. */
class RepositoryViewModelFactory(
    private val repository: ExpenseRepository,
    private val create: (ExpenseRepository) -> ViewModel,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = create(repository) as T
}
