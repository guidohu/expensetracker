package com.github.guidohu.expensetracker.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.guidohu.expensetracker.data.Category
import com.github.guidohu.expensetracker.data.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoriesViewModel(private val repository: ExpenseRepository) : ViewModel() {

    val categories: StateFlow<List<Category>> = repository.categories.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _deleteBlocked = MutableStateFlow<Category?>(null)
    /** Set when the user tried to delete a category that still has expenses, so the UI can confirm. */
    val deleteBlocked: StateFlow<Category?> = _deleteBlocked.asStateFlow()

    fun addCategory(name: String, color: Int) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.addCategory(trimmed, color)
        }
    }

    fun requestDelete(category: Category) {
        viewModelScope.launch {
            if (repository.expenseCountFor(category.id) > 0) {
                _deleteBlocked.value = category
            } else {
                repository.deleteCategory(category)
            }
        }
    }

    fun confirmDeleteWithExpenses() {
        val category = _deleteBlocked.value ?: return
        viewModelScope.launch {
            repository.deleteCategory(category)
            _deleteBlocked.value = null
        }
    }

    fun dismissDeleteBlocked() {
        _deleteBlocked.value = null
    }
}
