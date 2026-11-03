package com.github.guidohu.expensetracker.data

import android.content.Context
import com.github.guidohu.expensetracker.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/**
 * Small app settings (default currency, onboarding status). Backed by SharedPreferences directly
 * rather than Jetpack DataStore — a single string and a boolean don't need a reactive-storage
 * library; the StateFlow here gives screens the reactivity they need.
 */
class UserPreferences(context: Context) : BackupSettings {
    private val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    init {
        // Grandfathering, decided once on the first launch of the first version with a Pro pack:
        // anyone already past onboarding installed before Pro existed and had every feature, so
        // they keep them. A fresh install gets here before onboarding, so it isn't grandfathered.
        if (!prefs.contains(KEY_LEGACY_PRO)) {
            prefs.edit().putBoolean(KEY_LEGACY_PRO, prefs.getBoolean(KEY_ONBOARDED, false)).apply()
        }
    }

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

    /** Epoch day of the most recent expense as of the last no-spend-streak check — lets the
     * streak check tell "still the same streak" apart from "a new expense reset it". */
    var lastStreakAnchorDate: Long?
        get() = if (prefs.contains(KEY_STREAK_ANCHOR_DATE)) prefs.getLong(KEY_STREAK_ANCHOR_DATE, 0L) else null
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_STREAK_ANCHOR_DATE) else putLong(KEY_STREAK_ANCHOR_DATE, value)
            }.apply()
        }

    /** The largest no-spend-streak milestone (in days) already notified for the current streak. */
    var lastStreakMilestoneNotified: Int
        get() = prefs.getInt(KEY_STREAK_MILESTONE_NOTIFIED, 0)
        set(value) { prefs.edit().putInt(KEY_STREAK_MILESTONE_NOTIFIED, value).apply() }

    // --- Pro entitlement. Deliberately left out of exportBackupSettings: a backup is a user-editable
    // zip of CSVs, so carrying these in it would let anyone unlock Pro by editing a file.

    /** True if this user was grandfathered into Pro for having installed before the Pro pack existed. */
    val isLegacyPro: Boolean = prefs.getBoolean(KEY_LEGACY_PRO, false)

    private val _isProPurchased = MutableStateFlow(prefs.getBoolean(KEY_PRO_PURCHASED, false))
    /** Whether Google Play last confirmed a Pro pack purchase — cached so Pro keeps working offline. */
    val isProPurchased: StateFlow<Boolean> = _isProPurchased

    private val _debugProOverride = MutableStateFlow(
        if (BuildConfig.DEBUG && prefs.contains(KEY_DEBUG_PRO_OVERRIDE)) prefs.getBoolean(KEY_DEBUG_PRO_OVERRIDE, false) else null
    )
    /** Debug builds only: forces Pro on (true) or off (false) regardless of purchase; null means "real state". */
    val debugProOverride: StateFlow<Boolean?> = _debugProOverride

    private val _isPro = MutableStateFlow(computeIsPro())
    /** The one flag every Pro-gated feature checks. */
    val isPro: StateFlow<Boolean> = _isPro

    private fun computeIsPro(): Boolean =
        _debugProOverride.value ?: (_isProPurchased.value || isLegacyPro)

    fun setProPurchased(purchased: Boolean) {
        if (purchased == _isProPurchased.value) return
        prefs.edit().putBoolean(KEY_PRO_PURCHASED, purchased).apply()
        _isProPurchased.value = purchased
        _isPro.value = computeIsPro()
    }

    fun setDebugProOverride(value: Boolean?) {
        if (!BuildConfig.DEBUG) return
        prefs.edit().apply {
            if (value == null) remove(KEY_DEBUG_PRO_OVERRIDE) else putBoolean(KEY_DEBUG_PRO_OVERRIDE, value)
        }.apply()
        _debugProOverride.value = value
        _isPro.value = computeIsPro()
    }

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

    /** The user-chosen settings worth carrying in a backup, as strings. Excludes transient bookkeeping (streak anchors). */
    override fun exportBackupSettings(): Map<String, String> = buildMap {
        put(KEY_CURRENCY, defaultCurrency.value)
        monthlyBudget.value?.let { put(KEY_BUDGET, it.toString()) }
        put(KEY_REMINDER_ENABLED, dailyReminderEnabled.value.toString())
        put(KEY_REMINDER_HOUR, dailyReminderHour.value.toString())
        put(KEY_REMINDER_MINUTE, dailyReminderMinute.value.toString())
        put(KEY_BUDGET_CONGRATS_ENABLED, budgetCongratsEnabled.value.toString())
    }

    /** Applies settings from [exportBackupSettings]; unknown or malformed entries are skipped so an older/newer backup still restores. */
    override fun restoreBackupSettings(settings: Map<String, String>) {
        settings[KEY_CURRENCY]?.takeIf { it.isNotBlank() }?.let { setDefaultCurrency(it) }
        // A backup without a budget means "no budget", so clear rather than keep the current one.
        setMonthlyBudget(settings[KEY_BUDGET]?.toDoubleOrNull())
        settings[KEY_REMINDER_ENABLED]?.toBooleanStrictOrNull()?.let { setDailyReminderEnabled(it) }
        val hour = settings[KEY_REMINDER_HOUR]?.toIntOrNull()?.takeIf { it in 0..23 }
        val minute = settings[KEY_REMINDER_MINUTE]?.toIntOrNull()?.takeIf { it in 0..59 }
        if (hour != null && minute != null) setDailyReminderTime(hour, minute)
        settings[KEY_BUDGET_CONGRATS_ENABLED]?.toBooleanStrictOrNull()?.let { setBudgetCongratsEnabled(it) }
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
        private const val KEY_STREAK_ANCHOR_DATE = "streak_anchor_date"
        private const val KEY_STREAK_MILESTONE_NOTIFIED = "streak_milestone_notified"
        private const val KEY_LEGACY_PRO = "legacy_pro"
        private const val KEY_PRO_PURCHASED = "pro_purchased"
        private const val KEY_DEBUG_PRO_OVERRIDE = "debug_pro_override"
    }
}
