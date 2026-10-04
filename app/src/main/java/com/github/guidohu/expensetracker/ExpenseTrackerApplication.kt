package com.github.guidohu.expensetracker

import android.app.Application
import com.github.guidohu.expensetracker.data.AppContainer
import com.github.guidohu.expensetracker.data.AppDatabase
import com.github.guidohu.expensetracker.data.ExchangeRateService
import com.github.guidohu.expensetracker.data.ExpenseRepository
import com.github.guidohu.expensetracker.data.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class ExpenseTrackerApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    val container: AppContainer by lazy {
        val db = AppDatabase.getInstance(this, applicationScope)
        AppContainer(
            repository = ExpenseRepository(db.categoryDao(), db.expenseDao()),
            userPreferences = UserPreferences(this),
            exchangeRateService = ExchangeRateService(),
        )
    }
}
