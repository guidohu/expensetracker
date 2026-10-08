package com.github.guidohu.expensetracker.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.guidohu.expensetracker.data.BackupException
import com.github.guidohu.expensetracker.data.BackupManager
import com.github.guidohu.expensetracker.data.UserPreferences
import com.github.guidohu.expensetracker.notifications.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class SettingsViewModel(
    private val userPreferences: UserPreferences,
    private val backupManager: BackupManager,
) : ViewModel() {

    val defaultCurrency: StateFlow<String> = userPreferences.defaultCurrency
    val monthlyBudget: StateFlow<Double?> = userPreferences.monthlyBudget
    val dailyReminderEnabled: StateFlow<Boolean> = userPreferences.dailyReminderEnabled
    val dailyReminderHour: StateFlow<Int> = userPreferences.dailyReminderHour
    val dailyReminderMinute: StateFlow<Int> = userPreferences.dailyReminderMinute
    val budgetCongratsEnabled: StateFlow<Boolean> = userPreferences.budgetCongratsEnabled

    private val _backupBusy = MutableStateFlow(false)
    /** True while a backup or restore is running, so the UI can block starting another. */
    val backupBusy: StateFlow<Boolean> = _backupBusy

    private val _backupMessage = MutableStateFlow<String?>(null)
    /** One-shot outcome of the last backup/restore for the UI to show; clear with [backupMessageShown]. */
    val backupMessage: StateFlow<String?> = _backupMessage

    fun backupMessageShown() { _backupMessage.value = null }

    fun suggestedBackupFileName(): String = "expense-tracker-backup-${LocalDate.now()}.zip"

    fun exportBackup(context: Context, uri: Uri) = runBackupTask("Backup failed") {
        val summary = withContext(Dispatchers.IO) {
            val out = context.contentResolver.openOutputStream(uri, "wt")
                ?: throw BackupException("Couldn't write to the chosen location.")
            out.use { backupManager.export(it) }
        }
        "Backed up ${summary.expenses} expenses, ${summary.categories} categories and ${summary.wishlistItems} wishlist items"
    }

    fun restoreBackup(context: Context, uri: Uri) = runBackupTask("Restore failed") {
        val summary = withContext(Dispatchers.IO) {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw BackupException("Couldn't open the chosen file.")
            input.use { backupManager.restore(it) }
        }
        // Alarms follow the restored reminder/congrats settings.
        ReminderScheduler.rescheduleAllFromPrefs(context.applicationContext, userPreferences)
        "Restored ${summary.expenses} expenses, ${summary.categories} categories and ${summary.wishlistItems} wishlist items"
    }

    private fun runBackupTask(failurePrefix: String, task: suspend () -> String) {
        if (_backupBusy.value) return
        _backupBusy.value = true
        viewModelScope.launch {
            _backupMessage.value = try {
                task()
            } catch (e: BackupException) {
                "$failurePrefix: ${e.message}"
            } catch (e: android.database.SQLException) {
                "$failurePrefix: the database rejected the data (${e.message})"
            } catch (e: java.io.IOException) {
                "$failurePrefix: ${e.message ?: "couldn't read or write the file"}"
            } finally {
                _backupBusy.value = false
            }
        }
    }

    fun setDefaultCurrency(code: String) = userPreferences.setDefaultCurrency(code)

    fun setMonthlyBudget(amount: Double?) = userPreferences.setMonthlyBudget(amount)

    fun setDailyReminderEnabled(context: Context, enabled: Boolean) {
        userPreferences.setDailyReminderEnabled(enabled)
        if (enabled) {
            ReminderScheduler.scheduleDailyReminder(context, dailyReminderHour.value, dailyReminderMinute.value)
        } else {
            ReminderScheduler.cancelDailyReminder(context)
        }
    }

    fun setDailyReminderTime(context: Context, hour: Int, minute: Int) {
        userPreferences.setDailyReminderTime(hour, minute)
        if (dailyReminderEnabled.value) {
            ReminderScheduler.scheduleDailyReminder(context, hour, minute)
        }
    }

    fun setBudgetCongratsEnabled(context: Context, enabled: Boolean) {
        userPreferences.setBudgetCongratsEnabled(enabled)
        if (enabled) {
            ReminderScheduler.scheduleBudgetCheck(context)
            ReminderScheduler.scheduleStreakCheck(context)
        } else {
            ReminderScheduler.cancelBudgetCheck(context)
            ReminderScheduler.cancelStreakCheck(context)
        }
    }
}
