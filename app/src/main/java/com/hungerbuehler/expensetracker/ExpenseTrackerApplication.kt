package com.hungerbuehler.expensetracker

import android.app.Application
import com.hungerbuehler.expensetracker.data.AppDatabase
import com.hungerbuehler.expensetracker.data.ExpenseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class ExpenseTrackerApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    val repository: ExpenseRepository by lazy {
        val db = AppDatabase.getInstance(this, applicationScope)
        ExpenseRepository(db.categoryDao(), db.expenseDao())
    }
}
