package com.github.guidohu.expensetracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query(
        """
        SELECT e.id AS id, e.amount AS amount, e.currencyCode AS currencyCode, e.exchangeRate AS exchangeRate,
               e.title AS title, e.notes AS notes, e.date AS date,
               e.categoryId AS categoryId, c.name AS categoryName, c.color AS categoryColor
        FROM expenses e
        INNER JOIN categories c ON c.id = e.categoryId
        ORDER BY e.date DESC, e.id DESC
        """
    )
    fun getAllWithCategory(): Flow<List<ExpenseWithCategory>>

    @Insert
    suspend fun insert(expense: Expense): Long

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)
}
