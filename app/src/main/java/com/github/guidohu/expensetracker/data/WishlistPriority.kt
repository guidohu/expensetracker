package com.github.guidohu.expensetracker.data

/** Whether a wishlist entry is a need or a want. Stored by [name]. */
enum class WishlistPriority(val label: String) {
    NEED("I need"),
    WANT("I want"),
}

fun wishlistPriorityOrDefault(name: String?): WishlistPriority =
    WishlistPriority.entries.firstOrNull { it.name == name } ?: WishlistPriority.WANT
