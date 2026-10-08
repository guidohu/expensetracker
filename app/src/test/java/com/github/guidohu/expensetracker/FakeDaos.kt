package com.github.guidohu.expensetracker

import com.github.guidohu.expensetracker.data.Category
import com.github.guidohu.expensetracker.data.CategoryDao
import com.github.guidohu.expensetracker.data.Expense
import com.github.guidohu.expensetracker.data.ExpenseDao
import com.github.guidohu.expensetracker.data.ExpenseWithCategory
import com.github.guidohu.expensetracker.data.WishlistDao
import com.github.guidohu.expensetracker.data.WishlistItem
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
        val newId = category.id.takeIf { it != 0L } ?: ((state.value.maxOfOrNull { it.id } ?: 0L) + 1)
        state.value = (state.value + category.copy(id = newId)).sortedBy { it.name }
        return newId
    }

    override suspend fun getAllOnce(): List<Category> = state.value.sortedBy { it.id }

    override suspend fun deleteAll() {
        state.value = emptyList()
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
        val newId = expense.id.takeIf { it != 0L } ?: ((state.value.maxOfOrNull { it.id } ?: 0L) + 1)
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
            mood = expense.mood,
            priority = expense.priority,
            url = expense.url,
            wishlistAddedAt = expense.wishlistAddedAt,
        )
        return newId
    }

    override suspend fun getAllOnce(): List<Expense> = state.value.sortedBy { it.id }.map {
        Expense(
            id = it.id, amount = it.amount, currencyCode = it.currencyCode, exchangeRate = it.exchangeRate,
            categoryId = it.categoryId, title = it.title, notes = it.notes, date = it.date, mood = it.mood,
            priority = it.priority, url = it.url, wishlistAddedAt = it.wishlistAddedAt,
        )
    }

    override suspend fun deleteAll() {
        state.value = emptyList()
    }

    override suspend fun update(expense: Expense) {
        val existing = state.value.firstOrNull { it.id == expense.id } ?: return
        state.value = state.value.map {
            if (it.id == expense.id) {
                existing.copy(
                    amount = expense.amount,
                    currencyCode = expense.currencyCode,
                    exchangeRate = expense.exchangeRate,
                    title = expense.title,
                    notes = expense.notes,
                    date = expense.date,
                    categoryId = expense.categoryId,
                    mood = expense.mood,
                )
            } else it
        }
    }

    override suspend fun delete(expense: Expense) {
        state.value = state.value.filterNot { it.id == expense.id }
    }

    override suspend fun totalInRange(startEpochDay: Long, endEpochDay: Long): Double =
        state.value.filter { it.date in startEpochDay..endEpochDay }.sumOf { it.amountInDefaultCurrency }

    override suspend fun maxDate(): Long? = state.value.maxOfOrNull { it.date }
}

/** In-memory WishlistDao for screenshot tests. */
class FakeWishlistDao(initial: List<WishlistItem> = emptyList()) : WishlistDao {
    val state = MutableStateFlow(initial.sortedWith(compareByDescending<WishlistItem> { it.createdAt }.thenByDescending { it.id }))

    override fun getAll(): Flow<List<WishlistItem>> = state

    override suspend fun insert(item: WishlistItem): Long {
        val newId = item.id.takeIf { it != 0L } ?: ((state.value.maxOfOrNull { it.id } ?: 0L) + 1)
        state.value = state.value + item.copy(id = newId)
        return newId
    }

    override suspend fun getAllOnce(): List<WishlistItem> = state.value.sortedBy { it.id }

    override suspend fun deleteAll() {
        state.value = emptyList()
    }

    override suspend fun update(item: WishlistItem) {
        state.value = state.value.map { if (it.id == item.id) item else it }
    }

    override suspend fun delete(item: WishlistItem) {
        state.value = state.value.filterNot { it.id == item.id }
    }
}
