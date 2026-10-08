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
               e.categoryId AS categoryId, c.name AS categoryName, c.color AS categoryColor, e.mood AS mood,
               e.priority AS priority, e.url AS url, e.wishlistAddedAt AS wishlistAddedAt
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

    @Query("SELECT COALESCE(SUM(amount * exchangeRate), 0.0) FROM expenses WHERE date BETWEEN :startEpochDay AND :endEpochDay")
    suspend fun totalInRange(startEpochDay: Long, endEpochDay: Long): Double

    /** Epoch day of the most recent expense, or null if none have ever been logged. */
    @Query("SELECT MAX(date) FROM expenses")
    suspend fun maxDate(): Long?
}
