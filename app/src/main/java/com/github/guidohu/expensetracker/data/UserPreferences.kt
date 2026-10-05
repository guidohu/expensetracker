package com.github.guidohu.expensetracker.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/**
 * Small app settings (default currency, onboarding status). Backed by SharedPreferences directly
 * rather than Jetpack DataStore — a single string and a boolean don't need a reactive-storage
 * library; the StateFlow here gives screens the reactivity they need.
 */
class UserPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val defaultCurrencyFallback: String =
        runCatching { java.util.Currency.getInstance(Locale.getDefault()).currencyCode }.getOrDefault("USD")

    private val _defaultCurrency = MutableStateFlow(prefs.getString(KEY_CURRENCY, null) ?: defaultCurrencyFallback)
    val defaultCurrency: StateFlow<String> = _defaultCurrency

    private val _monthlyBudget = MutableStateFlow(
        if (prefs.contains(KEY_BUDGET)) prefs.getFloat(KEY_BUDGET, 0f).toDouble() else null
    )
    /** The user's monthly spending target in their default currency, or null if unset. */
    val monthlyBudget: StateFlow<Double?> = _monthlyBudget

    val hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDED, false)

    private val _dailyReminderEnabled = MutableStateFlow(prefs.getBoolean(KEY_REMINDER_ENABLED, false))
    val dailyReminderEnabled: StateFlow<Boolean> = _dailyReminderEnabled

    private val _dailyReminderHour = MutableStateFlow(prefs.getInt(KEY_REMINDER_HOUR, 20))
    val dailyReminderHour: StateFlow<Int> = _dailyReminderHour

    private val _dailyReminderMinute = MutableStateFlow(prefs.getInt(KEY_REMINDER_MINUTE, 0))
    val dailyReminderMinute: StateFlow<Int> = _dailyReminderMinute

    private val _budgetCongratsEnabled = MutableStateFlow(prefs.getBoolean(KEY_BUDGET_CONGRATS_ENABLED, true))
    val budgetCongratsEnabled: StateFlow<Boolean> = _budgetCongratsEnabled

    /** "YYYY-MM" of the last month a budget-congrats notification was sent for — dedupes re-sends. */
    var lastBudgetCongratsMonth: String?
        get() = prefs.getString(KEY_LAST_BUDGET_CONGRATS_MONTH, null)
        set(value) { prefs.edit().putString(KEY_LAST_BUDGET_CONGRATS_MONTH, value).apply() }

    fun setDailyReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REMINDER_ENABLED, enabled).apply()
        _dailyReminderEnabled.value = enabled
    }

    fun setDailyReminderTime(hour: Int, minute: Int) {
        prefs.edit().putInt(KEY_REMINDER_HOUR, hour).putInt(KEY_REMINDER_MINUTE, minute).apply()
        _dailyReminderHour.value = hour
        _dailyReminderMinute.value = minute
    }

    fun setBudgetCongratsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BUDGET_CONGRATS_ENABLED, enabled).apply()
        _budgetCongratsEnabled.value = enabled
    }

    fun setDefaultCurrency(code: String) {
        prefs.edit().putString(KEY_CURRENCY, code).apply()
        _defaultCurrency.value = code
    }

    fun setMonthlyBudget(amount: Double?) {
        prefs.edit().apply {
            if (amount == null) remove(KEY_BUDGET) else putFloat(KEY_BUDGET, amount.toFloat())
        }.apply()
        _monthlyBudget.value = amount
    }

    fun completeOnboarding(currencyCode: String) {
        prefs.edit().putBoolean(KEY_ONBOARDED, true).apply()
        setDefaultCurrency(currencyCode)
    }

    companion object {
        private const val KEY_CURRENCY = "default_currency"
        private const val KEY_ONBOARDED = "has_onboarded"
        private const val KEY_BUDGET = "monthly_budget"
        private const val KEY_REMINDER_ENABLED = "daily_reminder_enabled"
        private const val KEY_REMINDER_HOUR = "daily_reminder_hour"
        private const val KEY_REMINDER_MINUTE = "daily_reminder_minute"
        private const val KEY_BUDGET_CONGRATS_ENABLED = "budget_congrats_enabled"
        private const val KEY_LAST_BUDGET_CONGRATS_MONTH = "last_budget_congrats_month"
    }
}
