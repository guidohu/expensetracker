package com.github.guidohu.expensetracker.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance()

fun formatCurrency(amount: Double): String = currencyFormat.format(amount)

fun currencySymbol(): String = currencyFormat.currency?.symbol ?: "$"

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
