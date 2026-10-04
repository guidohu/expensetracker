package com.github.guidohu.expensetracker.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

/**
 * Looks up historical exchange rates via the Frankfurter API (frankfurter.app) — free, no API key,
 * backed by European Central Bank reference rates. This is the only network call this app makes:
 * it sends just the two currency codes and a date, nothing about the user or their expenses.
 */
class ExchangeRateService {

    /** Returns the rate to multiply a [from]-currency amount by to get a [to]-currency amount, or null on failure. */
    suspend fun fetchRate(from: String, to: String, date: LocalDate): Double? {
        if (from == to) return 1.0
        return withContext(Dispatchers.IO) {
            runCatching {
                val url = URL("https://api.frankfurter.app/$date?from=$from&to=$to")
                (url.openConnection() as HttpURLConnection).run {
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    requestMethod = "GET"
                    inputStream.bufferedReader().use { it.readText() }.also { disconnect() }
                }
            }.mapCatching { body ->
                JSONObject(body).getJSONObject("rates").getDouble(to)
            }.getOrNull()
        }
    }
}
