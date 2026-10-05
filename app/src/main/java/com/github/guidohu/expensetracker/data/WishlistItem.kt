package com.github.guidohu.expensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wishlist_items")
data class WishlistItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** The short "what" — e.g. "Noise-cancelling headphones". */
    val title: String,
    /** Optional target price. Null if the user didn't set one. */
    val price: Double?,
    /** Currency for [price]. Null iff [price] is null. */
    val currencyCode: String?,
    /** Optional free-form note. */
    val note: String,
    /** Optional link to the product page. */
    val url: String?,
    /** Open Graph title fetched from [url], if any. */
    val previewTitle: String?,
    /** Open Graph description fetched from [url], if any. */
    val previewDescription: String?,
    /** Open Graph image URL fetched from [url], if any. */
    val previewImageUrl: String?,
    /** Epoch day (LocalDate.toEpochDay()) the item was added. */
    val createdAt: Long,
    /** [WishlistPriority.name] — "NEED" or "WANT". */
    val priority: String,
    /** [Mood.name], or null if not set. */
    val mood: String?,
)
