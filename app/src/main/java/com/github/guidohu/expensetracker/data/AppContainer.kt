package com.github.guidohu.expensetracker.data

/** Bundles the app's few singletons so screens take one param instead of three-plus — no DI framework needed for this. */
data class AppContainer(
    val repository: ExpenseRepository,
    val wishlistRepository: WishlistRepository,
    val userPreferences: UserPreferences,
    val exchangeRateService: ExchangeRateService,
    val urlPreviewService: UrlPreviewService,
)
