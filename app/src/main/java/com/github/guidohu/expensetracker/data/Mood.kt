package com.github.guidohu.expensetracker.data

/** How the user felt about an expense or wishlist entry. Stored by [name] — optional on both. */
enum class Mood(val emoji: String, val label: String) {
    HAPPY("😊", "Happy"),
    EXCITED("🤩", "Excited"),
    NEUTRAL("😐", "Neutral"),
    STRESSED("😣", "Stressed"),
    REGRET("😕", "Regret"),
    SAD("😢", "Sad"),
}

fun moodOrNull(name: String?): Mood? = name?.let { stored -> Mood.entries.firstOrNull { it.name == stored } }
