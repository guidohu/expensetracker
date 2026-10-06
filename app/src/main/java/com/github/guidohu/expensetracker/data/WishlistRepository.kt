package com.github.guidohu.expensetracker.data

import kotlinx.coroutines.flow.Flow

class WishlistRepository(private val wishlistDao: WishlistDao) {
    val items: Flow<List<WishlistItem>> = wishlistDao.getAll()

    suspend fun addItem(
        title: String,
        price: Double?,
        currencyCode: String?,
        note: String,
        url: String?,
        previewTitle: String?,
        previewDescription: String?,
        previewImageUrl: String?,
        createdAt: Long,
        priority: WishlistPriority,
        mood: Mood?,
        categoryId: Long?,
    ): Long = wishlistDao.insert(
        WishlistItem(
            title = title,
            price = price,
            currencyCode = currencyCode,
            note = note,
            url = url,
            previewTitle = previewTitle,
            previewDescription = previewDescription,
            previewImageUrl = previewImageUrl,
            createdAt = createdAt,
            priority = priority.name,
            mood = mood?.name,
            categoryId = categoryId,
        )
    )

    suspend fun updateItem(
        id: Long,
        title: String,
        price: Double?,
        currencyCode: String?,
        note: String,
        url: String?,
        previewTitle: String?,
        previewDescription: String?,
        previewImageUrl: String?,
        createdAt: Long,
        priority: WishlistPriority,
        mood: Mood?,
        categoryId: Long?,
    ) = wishlistDao.update(
        WishlistItem(
            id = id,
            title = title,
            price = price,
            currencyCode = currencyCode,
            note = note,
            url = url,
            previewTitle = previewTitle,
            previewDescription = previewDescription,
            previewImageUrl = previewImageUrl,
            createdAt = createdAt,
            priority = priority.name,
            mood = mood?.name,
            categoryId = categoryId,
        )
    )

    suspend fun deleteItem(item: WishlistItem) = wishlistDao.delete(item)
}
