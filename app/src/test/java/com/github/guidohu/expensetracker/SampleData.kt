package com.github.guidohu.expensetracker

import com.github.guidohu.expensetracker.data.BackupManager
import com.github.guidohu.expensetracker.data.Category
import com.github.guidohu.expensetracker.data.ExpenseRepository
import com.github.guidohu.expensetracker.data.ExpenseWithCategory
import com.github.guidohu.expensetracker.data.BackupSettings
import com.github.guidohu.expensetracker.data.WishlistItem
import com.github.guidohu.expensetracker.data.WishlistRepository
import java.time.LocalDate

/** Sample data shared by the screenshot tests and the Play Store asset renders. */
val food = Category(1, "Food", 0xFFFF7043.toInt())
val transport = Category(2, "Transport", 0xFF42A5F5.toInt())
val shopping = Category(3, "Shopping", 0xFFAB47BC.toInt())
val bills = Category(4, "Bills", 0xFF5C6BC0.toInt())
val entertainment = Category(5, "Entertainment", 0xFFEC407A.toInt())
val sampleCategories = listOf(food, transport, shopping, bills, entertainment)

private fun expense(id: Long, category: Category, amount: Double, title: String, daysAgo: Long, notes: String = ""): ExpenseWithCategory =
    ExpenseWithCategory(
        id = id,
        amount = amount,
        currencyCode = "USD",
        exchangeRate = 1.0,
        title = title,
        notes = notes,
        date = LocalDate.now().minusDays(daysAgo).toEpochDay(),
        categoryId = category.id,
        categoryName = category.name,
        categoryColor = category.color,
    )

private fun monthExpense(id: Long, category: Category, amount: Double, monthsAgo: Long): ExpenseWithCategory =
    ExpenseWithCategory(
        id = id,
        amount = amount,
        currencyCode = "USD",
        exchangeRate = 1.0,
        title = category.name,
        notes = "",
        date = LocalDate.now().minusMonths(monthsAgo).withDayOfMonth(10).toEpochDay(),
        categoryId = category.id,
        categoryName = category.name,
        categoryColor = category.color,
    )

val sampleExpenses = listOf(
    expense(1, food, 12.50, "Lunch", 0),
    expense(2, transport, 4.20, "Bus ticket", 0),
    expense(3, food, 38.90, "Groceries", 1, notes = "Weekly shop"),
    expense(4, entertainment, 15.00, "Movie", 1),
    expense(5, shopping, 64.00, "New shoes", 3),
    expense(6, bills, 89.00, "Phone bill", 5),
    expense(7, food, 22.30, "Dinner out", 6),
) + (1..5L).flatMap { monthsAgo ->
    listOf(
        monthExpense(100 + monthsAgo, food, 180.0 + monthsAgo * 15, monthsAgo),
        monthExpense(200 + monthsAgo, transport, 60.0 + monthsAgo * 5, monthsAgo),
        monthExpense(300 + monthsAgo, bills, 95.0, monthsAgo),
    )
}

fun repositoryOf(categories: List<Category>, expenses: List<ExpenseWithCategory>): ExpenseRepository =
    ExpenseRepository(FakeCategoryDao(categories), FakeExpenseDao(expenses))

fun backupManagerOf(userPreferences: BackupSettings): BackupManager =
    BackupManager(FakeCategoryDao(), FakeExpenseDao(), FakeWishlistDao(), userPreferences, inTransaction = { it() })

fun wishlistRepositoryOf(items: List<WishlistItem> = emptyList()): WishlistRepository =
    WishlistRepository(FakeWishlistDao(items))

val sampleWishlistItems = listOf(
    WishlistItem(
        id = 1,
        title = "Noise-cancelling headphones",
        price = 249.0,
        currencyCode = "USD",
        note = "Wait for a Black Friday deal",
        url = "https://example.com/headphones",
        previewTitle = "Premium Wireless Headphones",
        previewDescription = "Industry-leading noise cancellation with up to 30 hours of battery life.",
        previewImageUrl = null,
        createdAt = LocalDate.now().minusDays(2).toEpochDay(),
        priority = "WANT",
        mood = "EXCITED",
    ),
    WishlistItem(
        id = 2,
        title = "New laptop charger",
        price = 45.0,
        currencyCode = "USD",
        note = "Old one frayed at the cable",
        url = null,
        previewTitle = null,
        previewDescription = null,
        previewImageUrl = null,
        createdAt = LocalDate.now().minusDays(5).toEpochDay(),
        priority = "NEED",
        mood = "STRESSED",
    ),
    WishlistItem(
        id = 3,
        title = "Weekend hiking boots",
        price = null,
        currencyCode = null,
        note = "",
        url = null,
        previewTitle = null,
        previewDescription = null,
        previewImageUrl = null,
        createdAt = LocalDate.now().minusDays(10).toEpochDay(),
        priority = "WANT",
        mood = null,
    ),
)
