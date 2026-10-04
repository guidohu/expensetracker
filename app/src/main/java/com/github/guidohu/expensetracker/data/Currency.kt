package com.github.guidohu.expensetracker.data

/** A curated set of common ISO 4217 currencies — enough for a personal expense tracker. */
data class AppCurrency(val code: String, val symbol: String, val displayName: String)

val SupportedCurrencies: List<AppCurrency> = listOf(
    AppCurrency("USD", "$", "US Dollar"),
    AppCurrency("EUR", "€", "Euro"),
    AppCurrency("GBP", "£", "British Pound"),
    AppCurrency("CHF", "Fr", "Swiss Franc"),
    AppCurrency("JPY", "¥", "Japanese Yen"),
    AppCurrency("CAD", "$", "Canadian Dollar"),
    AppCurrency("AUD", "$", "Australian Dollar"),
    AppCurrency("NZD", "$", "New Zealand Dollar"),
    AppCurrency("CNY", "¥", "Chinese Yuan"),
    AppCurrency("HKD", "$", "Hong Kong Dollar"),
    AppCurrency("SGD", "$", "Singapore Dollar"),
    AppCurrency("INR", "₹", "Indian Rupee"),
    AppCurrency("KRW", "₩", "South Korean Won"),
    AppCurrency("SEK", "kr", "Swedish Krona"),
    AppCurrency("NOK", "kr", "Norwegian Krone"),
    AppCurrency("DKK", "kr", "Danish Krone"),
    AppCurrency("PLN", "zł", "Polish Zloty"),
    AppCurrency("CZK", "Kč", "Czech Koruna"),
    AppCurrency("HUF", "Ft", "Hungarian Forint"),
    AppCurrency("RON", "lei", "Romanian Leu"),
    AppCurrency("TRY", "₺", "Turkish Lira"),
    AppCurrency("ZAR", "R", "South African Rand"),
    AppCurrency("MXN", "$", "Mexican Peso"),
    AppCurrency("BRL", "R$", "Brazilian Real"),
    AppCurrency("ILS", "₪", "Israeli Shekel"),
    AppCurrency("AED", "د.إ", "UAE Dirham"),
    AppCurrency("THB", "฿", "Thai Baht"),
    AppCurrency("IDR", "Rp", "Indonesian Rupiah"),
    AppCurrency("MYR", "RM", "Malaysian Ringgit"),
    AppCurrency("PHP", "₱", "Philippine Peso"),
)

fun currencyFor(code: String): AppCurrency =
    SupportedCurrencies.firstOrNull { it.code == code } ?: AppCurrency(code, code, code)
