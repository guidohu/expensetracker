package com.hungerbuehler.expensetracker.data

import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val categoryDao: CategoryDao,
    private val expenseDao: ExpenseDao,
) {
    val categories: Flow<List<Category>> = categoryDao.getAll()
    val expenses: Flow<List<ExpenseWithCategory>> = expenseDao.getAllWithCategory()

    suspend fun addCategory(name: String, color: Int): Long =
        categoryDao.insert(Category(name = name, color = color))

    suspend fun deleteCategory(category: Category) = categoryDao.delete(category)

    suspend fun expenseCountFor(categoryId: Long): Int = categoryDao.expenseCountFor(categoryId)

    suspend fun addExpense(amount: Double, categoryId: Long, note: String, date: Long): Long =
        expenseDao.insert(Expense(amount = amount, categoryId = categoryId, note = note, date = date))

    suspend fun deleteExpense(expense: ExpenseWithCategory) = expenseDao.delete(
        Expense(id = expense.id, amount = expense.amount, categoryId = expense.categoryId, note = expense.note, date = expense.date)
    )
}
