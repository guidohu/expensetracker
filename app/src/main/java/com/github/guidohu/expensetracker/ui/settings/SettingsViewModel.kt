package com.github.guidohu.expensetracker.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import com.github.guidohu.expensetracker.data.UserPreferences
import com.github.guidohu.expensetracker.notifications.ReminderScheduler
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(private val userPreferences: UserPreferences) : ViewModel() {

    val defaultCurrency: StateFlow<String> = userPreferences.defaultCurrency
    val monthlyBudget: StateFlow<Double?> = userPreferences.monthlyBudget
    val dailyReminderEnabled: StateFlow<Boolean> = userPreferences.dailyReminderEnabled
    val dailyReminderHour: StateFlow<Int> = userPreferences.dailyReminderHour
    val dailyReminderMinute: StateFlow<Int> = userPreferences.dailyReminderMinute
    val budgetCongratsEnabled: StateFlow<Boolean> = userPreferences.budgetCongratsEnabled

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
        } else {
            ReminderScheduler.cancelBudgetCheck(context)
        }
    }
}
