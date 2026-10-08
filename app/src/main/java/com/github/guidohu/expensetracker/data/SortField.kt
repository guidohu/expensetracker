package com.github.guidohu.expensetracker.data

/** Shared sort fields for the Expenses and Wishlist lists — each screen supplies its own display label. */
enum class SortField {
    DATE,
    CATEGORY,
    AMOUNT,
    NAME,
    MOOD,
}

/** The direction a field starts in when first picked: newest / highest first, text A–Z. */
fun SortField.defaultAscending(): Boolean = when (this) {
    SortField.DATE, SortField.AMOUNT -> false
    SortField.CATEGORY, SortField.NAME, SortField.MOOD -> true
}

/** A run of list items under an optional header — null [header] means a flat, headerless list. */
data class ListSection<T>(val key: String, val header: String?, val items: List<T>)

/** One headerless section holding [items] ordered by [comparator] (reversed when descending). */
fun <T> flatSection(items: List<T>, ascending: Boolean, comparator: Comparator<T>): List<ListSection<T>> {
    if (items.isEmpty()) return emptyList()
    val sorted = items.sortedWith(comparator)
    return listOf(ListSection("all", null, if (ascending) sorted else sorted.reversed()))
}

/**
 * Groups [items] under headers. Groups are ordered by [keyComparator] (reversed when descending),
 * except keys matching [pinLast] which always sit at the very end; items inside each group are
 * ordered by [within], independent of the direction.
 */
fun <T, K : Any> groupedSections(
    items: List<T>,
    ascending: Boolean,
    groupKey: (T) -> K,
    header: (K) -> String,
    keyComparator: Comparator<K>,
    within: Comparator<T>,
    pinLast: (K) -> Boolean = { false },
): List<ListSection<T>> {
    val groups = items.groupBy(groupKey)
    val ordered = groups.keys.sortedWith(keyComparator).let { if (ascending) it else it.reversed() }
    val (pinned, regular) = ordered.partition(pinLast)
    return (regular + pinned).map { key ->
        ListSection(key.toString(), header(key), groups.getValue(key).sortedWith(within))
    }
}
