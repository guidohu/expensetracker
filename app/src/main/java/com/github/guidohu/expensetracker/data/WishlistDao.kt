package com.github.guidohu.expensetracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WishlistDao {
    @Query("SELECT * FROM wishlist_items ORDER BY createdAt DESC, id DESC")
    fun getAll(): Flow<List<WishlistItem>>

    @Query("SELECT * FROM wishlist_items ORDER BY id ASC")
    suspend fun getAllOnce(): List<WishlistItem>

    @Query("DELETE FROM wishlist_items")
    suspend fun deleteAll()

    @Insert
    suspend fun insert(item: WishlistItem): Long

    @Update
    suspend fun update(item: WishlistItem)

    @Delete
    suspend fun delete(item: WishlistItem)
}
