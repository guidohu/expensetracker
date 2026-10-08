package com.github.guidohu.expensetracker

import android.app.Application
import androidx.room.withTransaction
import com.github.guidohu.expensetracker.data.AppContainer
import com.github.guidohu.expensetracker.data.AppDatabase
import com.github.guidohu.expensetracker.data.BackupManager
import com.github.guidohu.expensetracker.data.ExchangeRateService
import com.github.guidohu.expensetracker.data.ExpenseRepository
import com.github.guidohu.expensetracker.data.UrlPreviewService
import com.github.guidohu.expensetracker.data.UserPreferences
import com.github.guidohu.expensetracker.data.WishlistRepository
import com.github.guidohu.expensetracker.notifications.NotificationHelper
import com.github.guidohu.expensetracker.notifications.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class ExpenseTrackerApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    val container: AppContainer by lazy {
        val db = AppDatabase.getInstance(this, applicationScope)
        val userPreferences = UserPreferences(this)
        AppContainer(
            repository = ExpenseRepository(db.categoryDao(), db.expenseDao()),
            wishlistRepository = WishlistRepository(db.wishlistDao()),
            userPreferences = userPreferences,
            exchangeRateService = ExchangeRateService(),
            urlPreviewService = UrlPreviewService(),
            backupManager = BackupManager(
                db.categoryDao(), db.expenseDao(), db.wishlistDao(), userPreferences,
                inTransaction = { block -> db.withTransaction { block() } },
            ),
        )
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        // Alarms don't survive reboot, app update, or a backup restore onto a new phone — the
        // prefs that drive them do, so re-arm from current prefs every time the app starts.
        ReminderScheduler.rescheduleAllFromPrefs(this, container.userPreferences)
    }
}
