package com.github.guidohu.expensetracker

import com.github.guidohu.expensetracker.data.Category
import com.github.guidohu.expensetracker.data.CategoryDao
import com.github.guidohu.expensetracker.data.Expense
import com.github.guidohu.expensetracker.data.ExpenseDao
import com.github.guidohu.expensetracker.data.ExpenseWithCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory CategoryDao for screenshot tests — avoids needing a real SQLite/Room setup. */
class FakeCategoryDao(initial: List<Category> = emptyList()) : CategoryDao {
    val state = MutableStateFlow(initial.sortedBy { it.name })
    private val expenseCounts = MutableStateFlow<Map<Long, Int>>(emptyMap())

    fun setExpenseCount(categoryId: Long, count: Int) {
        expenseCounts.value = expenseCounts.value + (categoryId to count)
    }

    override fun getAll(): Flow<List<Category>> = state

    override suspend fun insert(category: Category): Long {
        val newId = (state.value.maxOfOrNull { it.id } ?: 0L) + 1
        state.value = (state.value + category.copy(id = newId)).sortedBy { it.name }
        return newId
    }

    override suspend fun update(category: Category) {
        state.value = state.value.map { if (it.id == category.id) category else it }.sortedBy { it.name }
    }

    override suspend fun delete(category: Category) {
        state.value = state.value.filterNot { it.id == category.id }
    }

    override suspend fun expenseCountFor(categoryId: Long): Int = expenseCounts.value[categoryId] ?: 0
}

/**
 * In-memory ExpenseDao for screenshot tests. Static snapshots only render the initial state,
 * so insert/delete just need to satisfy the interface, not reconstruct category display fields.
 */
class FakeExpenseDao(initial: List<ExpenseWithCategory> = emptyList()) : ExpenseDao {
    val state = MutableStateFlow(initial.sortedWith(compareByDescending<ExpenseWithCategory> { it.date }.thenByDescending { it.id }))

    override fun getAllWithCategory(): Flow<List<ExpenseWithCategory>> = state

    override suspend fun insert(expense: Expense): Long {
        val newId = (state.value.maxOfOrNull { it.id } ?: 0L) + 1
        state.value = state.value + ExpenseWithCategory(
            id = newId,
            amount = expense.amount,
            currencyCode = expense.currencyCode,
            exchangeRate = expense.exchangeRate,
            title = expense.title,
            notes = expense.notes,
            date = expense.date,
            categoryId = expense.categoryId,
            categoryName = "",
            categoryColor = 0xFF888888.toInt(),
        )
        return newId
    }

    override suspend fun delete(expense: Expense) {
        state.value = state.value.filterNot { it.id == expense.id }
    }
}
