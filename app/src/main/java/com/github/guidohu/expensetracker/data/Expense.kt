package com.github.guidohu.expensetracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("categoryId")],
)
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val categoryId: Long,
    val note: String,
    /** Epoch day (LocalDate.toEpochDay()), so it sorts and buckets without timezone concerns. */
    val date: Long,
)

/** Expense joined with its category's display info, for list rows. */
data class ExpenseWithCategory(
    val id: Long,
    val amount: Double,
    val note: String,
    val date: Long,
    val categoryId: Long,
    val categoryName: String,
    val categoryColor: Int,
)
