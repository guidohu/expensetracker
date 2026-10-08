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
    /** Amount in [currencyCode] — the currency the user actually paid in. */
    val amount: Double,
    val currencyCode: String,
    /** Multiply [amount] by this to get the value in the app's default currency at entry time. 1.0 if same currency. */
    val exchangeRate: Double,
    val categoryId: Long,
    /** The short "what" — e.g. "Socks". The primary label for the expense. */
    val title: String,
    /** Optional free-form extra detail, secondary to [title]. */
    val notes: String,
    /** Epoch day (LocalDate.toEpochDay()), so it sorts and buckets without timezone concerns. */
    val date: Long,
    /** [Mood.name], or null if not set. */
    val mood: String? = null,
    /** [WishlistPriority.name] — whether this was a need or a want. */
    val priority: String = WishlistPriority.WANT.name,
    val url: String? = null,
    /** Epoch day this expense's source wishlist item was originally added, or null if entered directly. */
    val wishlistAddedAt: Long? = null,
)

/** Expense joined with its category's display info, for list rows. */
data class ExpenseWithCategory(
    val id: Long,
    val amount: Double,
    val currencyCode: String,
    val exchangeRate: Double,
    val title: String,
    val notes: String,
    val date: Long,
    val categoryId: Long,
    val categoryName: String,
    val categoryColor: Int,
    val mood: String? = null,
    val priority: String = WishlistPriority.WANT.name,
    val url: String? = null,
    val wishlistAddedAt: Long? = null,
) {
    /** The value of this expense normalized into the default currency active when it was entered. */
    val amountInDefaultCurrency: Double get() = amount * exchangeRate
}
