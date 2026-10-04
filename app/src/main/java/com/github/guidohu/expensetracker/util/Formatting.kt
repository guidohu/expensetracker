package com.github.guidohu.expensetracker.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

/** Formats [amount] as currency in [currencyCode] (an explicit app setting, independent of device locale). */
fun formatCurrency(amount: Double, currencyCode: String): String =
    NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
        runCatching { currency = Currency.getInstance(currencyCode) }
    }.format(amount)

fun currencySymbol(currencyCode: String): String =
    runCatching { Currency.getInstance(currencyCode).getSymbol(Locale.getDefault()) }.getOrDefault(currencyCode)

private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

fun formatEpochDay(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(dateFormatter)

fun formatEpochDayRelative(epochDay: Long): String {
    val date = LocalDate.ofEpochDay(epochDay)
    val today = LocalDate.now()
    return when {
        date == today -> "Today"
        date == today.minusDays(1) -> "Yesterday"
        date.year == today.year -> date.format(DateTimeFormatter.ofPattern("MMM d"))
        else -> date.format(dateFormatter)
    }
}
