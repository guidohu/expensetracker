package com.github.guidohu.expensetracker.ui.settings

import androidx.lifecycle.ViewModel
import com.github.guidohu.expensetracker.data.UserPreferences
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(private val userPreferences: UserPreferences) : ViewModel() {

    val defaultCurrency: StateFlow<String> = userPreferences.defaultCurrency
    val monthlyBudget: StateFlow<Double?> = userPreferences.monthlyBudget

    fun setDefaultCurrency(code: String) = userPreferences.setDefaultCurrency(code)

    fun setMonthlyBudget(amount: Double?) = userPreferences.setMonthlyBudget(amount)
}
