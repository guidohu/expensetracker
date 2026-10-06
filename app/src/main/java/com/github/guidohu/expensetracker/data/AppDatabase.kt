package com.github.guidohu.expensetracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Database(entities = [Category::class, Expense::class, WishlistItem::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun wishlistDao(): WishlistDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        /**
         * This app has a real installed base (it's on the Play Store), so schema bumps must carry
         * a real migration — a destructive fallback here would silently wipe every user's expenses.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `expenses` ADD COLUMN `mood` TEXT")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `wishlist_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `price` REAL,
                        `currencyCode` TEXT,
                        `note` TEXT NOT NULL,
                        `url` TEXT,
                        `previewTitle` TEXT,
                        `previewDescription` TEXT,
                        `previewImageUrl` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `priority` TEXT NOT NULL,
                        `mood` TEXT
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `wishlist_items` ADD COLUMN `categoryId` INTEGER REFERENCES `categories`(`id`) ON DELETE SET NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_wishlist_items_categoryId` ON `wishlist_items` (`categoryId`)")
            }
        }

        fun getInstance(context: Context, scope: CoroutineScope): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context, AppDatabase::class.java, "expense_tracker.db")
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            scope.launch {
                                instance?.categoryDao()?.let { dao ->
                                    DefaultCategorySeed.forEach { dao.insert(it) }
                                }
                            }
                        }
                    })
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }
    }
}
