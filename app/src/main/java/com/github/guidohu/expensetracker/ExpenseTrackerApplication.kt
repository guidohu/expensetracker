package com.github.guidohu.expensetracker

import android.app.Application
import com.github.guidohu.expensetracker.data.AppDatabase
import com.github.guidohu.expensetracker.data.ExpenseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class ExpenseTrackerApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    val repository: ExpenseRepository by lazy {
        val db = AppDatabase.getInstance(this, applicationScope)
        ExpenseRepository(db.categoryDao(), db.expenseDao())
    }
}
