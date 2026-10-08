package com.github.guidohu.expensetracker.ui.stats

import com.github.guidohu.expensetracker.data.ExpenseWithCategory
import com.github.guidohu.expensetracker.data.Mood
import com.github.guidohu.expensetracker.data.WishlistPriority
import com.github.guidohu.expensetracker.data.moodOrNull
import com.github.guidohu.expensetracker.data.wishlistPriorityOrDefault
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

/** The time filters offered under "Historic Stats". Each one decides the donut range and the bar chart's span + resolution. */
enum class StatsPeriod(val label: String) {
    TODAY("Today"),
    THIS_WEEK("This week"),
    LAST_WEEK("Last week"),
    THIS_MONTH("This month"),
    LAST_MONTH("Last month"),
    THIS_YEAR("This year"),
    LAST_6_MONTHS("Last 6 months"),
    LAST_12_MONTHS("Last 12 months"),
    LAST_YEAR("Last year"),
}

/** What the donut + bar chart pair splits spending by — one carousel page each. */
enum class StatsDimension(val title: String) {
    CATEGORY("category"),
    MOOD("mood"),
    PRIORITY("priority"),
}

data class GroupTotal(
    val key: String,
    val name: String,
    val color: Int,
    /** Shown in place of the colour dot in the legend (moods). */
    val emoji: String?,
    val total: Double,
    val fraction: Float,
)

data class BucketSegment(val color: Int, val value: Double)

data class BarBucket(
    val label: String,
    val total: Double,
    val highlighted: Boolean,
    /** One entry per group with spending in this bucket, in the same order as [DimensionStats.groups]. */
    val segments: List<BucketSegment>,
)

data class DimensionStats(
    val dimension: StatsDimension,
    val groups: List<GroupTotal>,
    val bars: List<BarBucket>,
)

data class HistoricStats(
    val period: StatsPeriod,
    /** The span the donut (and [total]) covers. */
    val rangeStart: LocalDate,
    val rangeEnd: LocalDate,
    val total: Double,
    val average: Double,
    /** "day" or "month" — the unit [average] is per. */
    val averageUnit: String,
    val dimensions: List<DimensionStats>,
)

private class Bucket(val start: LocalDate, val end: LocalDate, val label: String, val highlighted: Boolean) {
    val startDay = start.toEpochDay()
    val endDay = end.toEpochDay()
}

private class Plan(
    val rangeStart: LocalDate,
    val rangeEnd: LocalDate,
    val buckets: List<Bucket>,
    val monthly: Boolean,
)

private class Group(val key: String, val name: String, val color: Int, val emoji: String?)

private val NoMoodGroup = Group("none", "No mood", 0xFFB0BEC5.toInt(), "➖")

private val moodColors = mapOf(
    Mood.HAPPY to 0xFF66BB6A.toInt(),
    Mood.EXCITED to 0xFFFFCA28.toInt(),
    Mood.NEUTRAL to 0xFF90A4AE.toInt(),
    Mood.STRESSED to 0xFFFF7043.toInt(),
    Mood.REGRET to 0xFFAB47BC.toInt(),
    Mood.SAD to 0xFF42A5F5.toInt(),
)

private fun groupOf(dimension: StatsDimension, expense: ExpenseWithCategory): Group = when (dimension) {
    StatsDimension.CATEGORY ->
        Group(expense.categoryId.toString(), expense.categoryName, expense.categoryColor, null)
    StatsDimension.MOOD -> moodOrNull(expense.mood)
        ?.let { Group(it.name, it.label, moodColors.getValue(it), it.emoji) }
        ?: NoMoodGroup
    StatsDimension.PRIORITY -> when (wishlistPriorityOrDefault(expense.priority)) {
        WishlistPriority.NEED -> Group("NEED", "Needs", 0xFF26A69A.toInt(), null)
        WishlistPriority.WANT -> Group("WANT", "Wants", 0xFFEC407A.toInt(), null)
    }
}

private fun dailyBuckets(from: LocalDate, to: LocalDate, today: LocalDate, locale: Locale): List<Bucket> {
    val days = ChronoUnit.DAYS.between(from, to).toInt() + 1
    return (0 until days).map { offset ->
        val date = from.plusDays(offset.toLong())
        // A week-sized chart has room to name every day; a month-sized one only labels every fifth.
        val label = if (days <= 7) {
            date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
        } else if (date.dayOfMonth == 1 || date.dayOfMonth % 5 == 0) {
            date.dayOfMonth.toString()
        } else {
            ""
        }
        Bucket(date, date, label, highlighted = date == today)
    }
}

private fun monthlyBuckets(from: YearMonth, to: YearMonth, today: LocalDate, locale: Locale): List<Bucket> {
    val months = ChronoUnit.MONTHS.between(from, to).toInt() + 1
    return (0 until months).map { offset ->
        val ym = from.plusMonths(offset.toLong())
        Bucket(
            ym.atDay(1), ym.atEndOfMonth(), ym.month.getDisplayName(TextStyle.SHORT, locale),
            highlighted = ym == YearMonth.from(today),
        )
    }
}

private fun planFor(period: StatsPeriod, today: LocalDate, firstDayOfWeek: DayOfWeek, locale: Locale): Plan {
    val weekStart = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    val thisMonth = YearMonth.from(today)

    fun dailyPlan(from: LocalDate, to: LocalDate, rangeStart: LocalDate = from, rangeEnd: LocalDate = to) =
        Plan(rangeStart, rangeEnd, dailyBuckets(from, to, today, locale), monthly = false)

    fun monthlyPlan(from: YearMonth, to: YearMonth) =
        Plan(from.atDay(1), to.atEndOfMonth(), monthlyBuckets(from, to, today, locale), monthly = true)

    return when (period) {
        StatsPeriod.TODAY -> dailyPlan(today.minusDays(6), today, rangeStart = today, rangeEnd = today)
        StatsPeriod.THIS_WEEK -> dailyPlan(weekStart, weekStart.plusDays(6))
        StatsPeriod.LAST_WEEK -> dailyPlan(weekStart.minusDays(7), weekStart.minusDays(1))
        StatsPeriod.THIS_MONTH -> dailyPlan(thisMonth.atDay(1), thisMonth.atEndOfMonth())
        StatsPeriod.LAST_MONTH -> {
            val last = thisMonth.minusMonths(1)
            dailyPlan(last.atDay(1), last.atEndOfMonth())
        }
        StatsPeriod.THIS_YEAR -> monthlyPlan(YearMonth.of(today.year, 1), YearMonth.of(today.year, 12))
        StatsPeriod.LAST_6_MONTHS -> monthlyPlan(thisMonth.minusMonths(5), thisMonth)
        StatsPeriod.LAST_12_MONTHS -> monthlyPlan(thisMonth.minusMonths(11), thisMonth)
        StatsPeriod.LAST_YEAR -> monthlyPlan(YearMonth.of(today.year - 1, 1), YearMonth.of(today.year - 1, 12))
    }
}

/**
 * Builds everything the "Historic Stats" section shows for [period]: the donut breakdown and the stacked
 * bar chart for each of [StatsDimension], all measured in the default currency.
 *
 * The donut covers the whole bucket span (e.g. all of this month, including days still to come) except for
 * [StatsPeriod.TODAY], whose bars look back a week while the donut covers just today.
 */
fun buildHistoricStats(
    expenses: List<ExpenseWithCategory>,
    period: StatsPeriod,
    today: LocalDate = LocalDate.now(),
    firstDayOfWeek: DayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek,
    locale: Locale = Locale.getDefault(),
): HistoricStats {
    val plan = planFor(period, today, firstDayOfWeek, locale)
    val rangeStartDay = plan.rangeStart.toEpochDay()
    val rangeEndDay = plan.rangeEnd.toEpochDay()

    val inRange = expenses.filter { it.date in rangeStartDay..rangeEndDay }
    val total = inRange.sumOf { it.amountInDefaultCurrency }

    val inBuckets = expenses.mapNotNull { expense ->
        val index = plan.buckets.indexOfFirst { expense.date in it.startDay..it.endDay }
        if (index >= 0) expense to index else null
    }

    val dimensions = StatsDimension.entries.map { dimension ->
        val groups = inRange
            .groupBy { groupOf(dimension, it).key }
            .map { (_, items) ->
                val group = groupOf(dimension, items.first())
                val groupTotal = items.sumOf { it.amountInDefaultCurrency }
                GroupTotal(
                    key = group.key, name = group.name, color = group.color, emoji = group.emoji,
                    total = groupTotal,
                    fraction = if (total > 0) (groupTotal / total).toFloat() else 0f,
                )
            }
            .sortedByDescending { it.total }

        // The bars are stacked in the donut's group order; groups that only appear outside the donut
        // range (the 7-day bars of "Today") follow after.
        val order = groups.map { it.key }
        val perBucket = inBuckets.groupBy({ it.second }, { it.first })
        val bars = plan.buckets.mapIndexed { index, bucket ->
            val segments = perBucket[index].orEmpty()
                .groupBy { groupOf(dimension, it).key }
                .map { (key, items) ->
                    key to BucketSegment(groupOf(dimension, items.first()).color, items.sumOf { it.amountInDefaultCurrency })
                }
                .sortedBy { (key, _) -> order.indexOf(key).let { if (it < 0) Int.MAX_VALUE else it } }
                .map { it.second }
            BarBucket(bucket.label, segments.sumOf { it.value }, bucket.highlighted, segments)
        }
        DimensionStats(dimension, groups, bars)
    }

    // Average over the part of the range that has actually happened (a month in progress isn't padded with
    // days to come), always counting at least one unit.
    val elapsedEnd = minOf(plan.rangeEnd, today)
    val units = if (plan.monthly) {
        ChronoUnit.MONTHS.between(YearMonth.from(plan.rangeStart), YearMonth.from(elapsedEnd)) + 1
    } else {
        ChronoUnit.DAYS.between(plan.rangeStart, elapsedEnd) + 1
    }.coerceAtLeast(1)

    return HistoricStats(
        period = period,
        rangeStart = plan.rangeStart,
        rangeEnd = plan.rangeEnd,
        total = total,
        average = total / units,
        averageUnit = if (plan.monthly) "month" else "day",
        dimensions = dimensions,
    )
}
