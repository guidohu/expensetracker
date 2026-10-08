package com.github.guidohu.expensetracker

import com.github.guidohu.expensetracker.data.ExpenseWithCategory
import com.github.guidohu.expensetracker.ui.stats.StatsDimension
import com.github.guidohu.expensetracker.ui.stats.StatsPeriod
import com.github.guidohu.expensetracker.ui.stats.buildHistoricStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

class HistoricStatsTest {

    // Thursday; the week runs Mon 5 Oct – Sun 11 Oct.
    private val today = LocalDate.of(2026, 10, 8)

    private fun expense(
        date: LocalDate,
        amount: Double,
        category: String = "Food",
        mood: String? = null,
        priority: String = "WANT",
    ) = ExpenseWithCategory(
        id = 0, amount = amount, currencyCode = "USD", exchangeRate = 1.0, title = "x", notes = "",
        date = date.toEpochDay(), categoryId = category.hashCode().toLong(), categoryName = category,
        categoryColor = 0, mood = mood, priority = priority,
    )

    private fun stats(expenses: List<ExpenseWithCategory>, period: StatsPeriod) =
        buildHistoricStats(expenses, period, today, DayOfWeek.MONDAY, Locale.ENGLISH)

    private val expenses = listOf(
        expense(today, 10.0, "Food", mood = "HAPPY", priority = "NEED"),
        expense(today.minusDays(3), 20.0, "Transport"),            // Mon 5 Oct: this week
        expense(today.minusDays(7), 40.0, "Food", mood = "SAD"),   // Thu 1 Oct: this month, last week
        expense(LocalDate.of(2026, 9, 15), 100.0, "Bills"),        // last month
        expense(LocalDate.of(2026, 3, 1), 200.0, "Bills"),         // this year only
        expense(LocalDate.of(2025, 11, 30), 400.0, "Food"),        // last year, and within the last 12 months
    )

    @Test fun today_donutIsTodayOnly_barsAreLastSevenDays() {
        val s = stats(expenses, StatsPeriod.TODAY)
        assertEquals(10.0, s.total, 0.0)
        val bars = s.dimensions.first().bars
        assertEquals(7, bars.size)
        assertEquals(10.0, bars.last().total, 0.0)
        assertEquals(20.0, bars[3].total, 0.0) // 3 days ago
        assertTrue(bars.last().highlighted)
        assertEquals("Thu", bars.last().label)
    }

    @Test fun thisWeek_showsAllSevenDaysEvenIfFuture() {
        val s = stats(expenses, StatsPeriod.THIS_WEEK)
        assertEquals(30.0, s.total, 0.0)
        val bars = s.dimensions.first().bars
        assertEquals(listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"), bars.map { it.label })
        assertEquals(0.0, bars.last().total, 0.0)
    }

    @Test fun lastWeek_coversPreviousMondayToSunday() {
        val s = stats(expenses, StatsPeriod.LAST_WEEK)
        assertEquals(LocalDate.of(2026, 9, 28), s.rangeStart)
        assertEquals(LocalDate.of(2026, 10, 4), s.rangeEnd)
        assertEquals(40.0, s.total, 0.0)
    }

    @Test fun thisMonth_hasOneBarPerDayOfMonth() {
        val s = stats(expenses, StatsPeriod.THIS_MONTH)
        assertEquals(70.0, s.total, 0.0)
        assertEquals(31, s.dimensions.first().bars.size)
        assertEquals("day", s.averageUnit)
        assertEquals(70.0 / 8, s.average, 1e-9) // 8 days elapsed
    }

    @Test fun lastMonth_hasThirtyDays() {
        val s = stats(expenses, StatsPeriod.LAST_MONTH)
        assertEquals(100.0, s.total, 0.0)
        assertEquals(30, s.dimensions.first().bars.size)
    }

    @Test fun thisYear_hasTwelveMonthlyBars() {
        val s = stats(expenses, StatsPeriod.THIS_YEAR)
        assertEquals(370.0, s.total, 0.0)
        val bars = s.dimensions.first().bars
        assertEquals(12, bars.size)
        assertEquals("Mar", bars[2].label)
        assertEquals(200.0, bars[2].total, 0.0)
        assertTrue(bars[9].highlighted)
        assertEquals("month", s.averageUnit)
        assertEquals(370.0 / 10, s.average, 1e-9)
    }

    @Test fun lastSixAndTwelveMonths_endAtCurrentMonth() {
        val six = stats(expenses, StatsPeriod.LAST_6_MONTHS)
        assertEquals(listOf("May", "Jun", "Jul", "Aug", "Sep", "Oct"), six.dimensions.first().bars.map { it.label })
        assertEquals(170.0, six.total, 0.0) // Mar is out of range

        val twelve = stats(expenses, StatsPeriod.LAST_12_MONTHS)
        assertEquals(12, twelve.dimensions.first().bars.size)
        assertEquals(770.0, twelve.total, 0.0) // Nov 2025 is within 12 months
    }

    @Test fun lastYear_isThePreviousCalendarYear() {
        val s = stats(expenses, StatsPeriod.LAST_YEAR)
        assertEquals(400.0, s.total, 0.0)
        assertEquals(12, s.dimensions.first().bars.size)
        assertEquals(400.0, s.dimensions.first().bars[10].total, 0.0)
    }

    @Test fun dimensions_groupByCategoryMoodAndPriority() {
        val s = stats(expenses, StatsPeriod.THIS_MONTH)
        val byCategory = s.dimensions.first { it.dimension == StatsDimension.CATEGORY }.groups
        assertEquals(listOf("Food", "Transport"), byCategory.map { it.name }) // 50 vs 20
        assertEquals(50f / 70f, byCategory[0].fraction, 1e-6f)

        val byMood = s.dimensions.first { it.dimension == StatsDimension.MOOD }.groups
        assertEquals(setOf("Happy", "Sad", "No mood"), byMood.map { it.name }.toSet())

        val byPriority = s.dimensions.first { it.dimension == StatsDimension.PRIORITY }.groups
        assertEquals(listOf("Wants", "Needs"), byPriority.map { it.name })
    }

    @Test fun stackedBar_segmentsSumToBucketTotal() {
        val s = stats(expenses + expense(today, 5.0, "Transport"), StatsPeriod.THIS_MONTH)
        val todayBar = s.dimensions.first().bars[7]
        assertEquals(15.0, todayBar.total, 0.0)
        assertEquals(2, todayBar.segments.size)
        assertEquals(15.0, todayBar.segments.sumOf { it.value }, 0.0)
    }

    @Test fun noExpenses_inRange_givesEmptyDonutButFullBars() {
        val s = stats(emptyList(), StatsPeriod.LAST_WEEK)
        assertEquals(0.0, s.total, 0.0)
        assertTrue(s.dimensions.all { it.groups.isEmpty() && it.bars.size == 7 })
    }
}
