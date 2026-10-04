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
    }
}
