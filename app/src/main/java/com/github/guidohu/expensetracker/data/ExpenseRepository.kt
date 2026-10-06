package com.github.guidohu.expensetracker.data

import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val categoryDao: CategoryDao,
    private val expenseDao: ExpenseDao,
) {
    val categories: Flow<List<Category>> = categoryDao.getAll()
    val expenses: Flow<List<ExpenseWithCategory>> = expenseDao.getAllWithCategory()

    suspend fun addCategory(name: String, color: Int): Long =
        categoryDao.insert(Category(name = name, color = color))

    suspend fun updateCategory(category: Category, name: String, color: Int) =
        categoryDao.update(category.copy(name = name, color = color))

    suspend fun deleteCategory(category: Category) = categoryDao.delete(category)

    suspend fun expenseCountFor(categoryId: Long): Int = categoryDao.expenseCountFor(categoryId)

    suspend fun addExpense(
        amount: Double,
        currencyCode: String,
        exchangeRate: Double,
        categoryId: Long,
        title: String,
        notes: String,
        date: Long,
        mood: Mood?,
    ): Long = expenseDao.insert(
        Expense(
            amount = amount,
            currencyCode = currencyCode,
            exchangeRate = exchangeRate,
            categoryId = categoryId,
            title = title,
            notes = notes,
            date = date,
            mood = mood?.name,
        )
    )

    suspend fun updateExpense(
        id: Long,
        amount: Double,
        currencyCode: String,
        exchangeRate: Double,
        categoryId: Long,
        title: String,
        notes: String,
        date: Long,
        mood: Mood?,
    ) = expenseDao.update(
        Expense(
            id = id,
            amount = amount,
            currencyCode = currencyCode,
            exchangeRate = exchangeRate,
            categoryId = categoryId,
            title = title,
            notes = notes,
            date = date,
            mood = mood?.name,
        )
    )

    suspend fun deleteExpense(expense: ExpenseWithCategory) = expenseDao.delete(
        Expense(
            id = expense.id,
            amount = expense.amount,
            currencyCode = expense.currencyCode,
            exchangeRate = expense.exchangeRate,
            categoryId = expense.categoryId,
            title = expense.title,
            notes = expense.notes,
            date = expense.date,
            mood = expense.mood,
        )
    )

    suspend fun totalInRange(startEpochDay: Long, endEpochDay: Long): Double =
        expenseDao.totalInRange(startEpochDay, endEpochDay)

    suspend fun lastExpenseDate(): Long? = expenseDao.maxDate()
}
