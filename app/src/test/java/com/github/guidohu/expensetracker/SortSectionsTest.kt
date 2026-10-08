package com.github.guidohu.expensetracker

import com.github.guidohu.expensetracker.data.Category
import com.github.guidohu.expensetracker.data.ExpenseWithCategory
import com.github.guidohu.expensetracker.data.SortField
import com.github.guidohu.expensetracker.data.WishlistItem
import com.github.guidohu.expensetracker.ui.expenses.layoutExpenseSections
import com.github.guidohu.expensetracker.ui.wishlist.layoutWishlistSections
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SortSectionsTest {

    private fun expense(id: Long, title: String, cat: String, amount: Double, day: Long, mood: String? = null) =
        ExpenseWithCategory(
            id = id, amount = amount, currencyCode = "USD", exchangeRate = 1.0, title = title, notes = "",
            date = day, categoryId = cat.hashCode().toLong(), categoryName = cat, categoryColor = 0, mood = mood,
        )

    private val expenses = listOf(
        expense(1, "Lunch", "Food", 12.0, day = 10, mood = "HAPPY"),
        expense(2, "Bus", "Transport", 4.0, day = 10),
        expense(3, "Dinner", "Food", 30.0, day = 11, mood = "SAD"),
        expense(4, "apple", "Food", 2.0, day = 9, mood = "HAPPY"),
        expense(5, "Taxi", "Transport", 20.0, day = 11),
    )

    private fun titles(s: List<com.github.guidohu.expensetracker.data.ListSection<ExpenseWithCategory>>) =
        s.map { sec -> sec.header to sec.items.map { it.title } }

    @Test fun date_groupsByDay_newestFirst_byDefault() {
        val s = layoutExpenseSections(expenses, SortField.DATE, ascending = false)
        assertEquals(3, s.size)
        assertEquals(listOf(11L, 10L, 9L), s.map { it.items.first().date })
        assertEquals(listOf("Taxi", "Dinner"), s[0].items.map { it.title }) // newest id first within the day
    }

    @Test fun date_ascending_reversesDays() {
        val s = layoutExpenseSections(expenses, SortField.DATE, ascending = true)
        assertEquals(listOf(9L, 10L, 11L), s.map { it.items.first().date })
    }

    @Test fun category_groupsUnderCategoryNames_newestFirstInside() {
        val s = layoutExpenseSections(expenses, SortField.CATEGORY, ascending = true)
        assertEquals(listOf("Food", "Transport"), s.map { it.header })
        assertEquals(listOf("Dinner", "Lunch", "apple"), s[0].items.map { it.title }) // day 11,10,9
        assertEquals(listOf("Taxi", "Bus"), s[1].items.map { it.title })
    }

    @Test fun category_descending_reversesGroupsOnly() {
        val s = layoutExpenseSections(expenses, SortField.CATEGORY, ascending = false)
        assertEquals(listOf("Transport", "Food"), s.map { it.header })
        assertEquals(listOf("Dinner", "Lunch", "apple"), s[1].items.map { it.title })
    }

    @Test fun mood_groupsByMood_noMoodAlwaysLast() {
        val asc = layoutExpenseSections(expenses, SortField.MOOD, ascending = true)
        assertEquals(listOf("😊 Happy", "😢 Sad", "No mood").map { it.substringAfter(' ') }, asc.map { it.header!!.substringAfter(' ') })
        val desc = layoutExpenseSections(expenses, SortField.MOOD, ascending = false)
        assertEquals("No mood", desc.last().header)
        assertEquals(listOf("Lunch", "apple"), asc.first().items.map { it.title })
    }

    @Test fun amount_isOneFlatList_descendingAndAscending() {
        val desc = layoutExpenseSections(expenses, SortField.AMOUNT, ascending = false)
        assertEquals(1, desc.size); assertNull(desc[0].header)
        assertEquals(listOf("Dinner", "Taxi", "Lunch", "Bus", "apple"), desc[0].items.map { it.title })
        val asc = layoutExpenseSections(expenses, SortField.AMOUNT, ascending = true)
        assertEquals(listOf("apple", "Bus", "Lunch", "Taxi", "Dinner"), asc[0].items.map { it.title })
    }

    @Test fun name_isOneFlatList_caseInsensitive() {
        val asc = layoutExpenseSections(expenses, SortField.NAME, ascending = true)
        assertEquals(1, asc.size); assertNull(asc[0].header)
        assertEquals(listOf("apple", "Bus", "Dinner", "Lunch", "Taxi"), asc[0].items.map { it.title })
        val desc = layoutExpenseSections(expenses, SortField.NAME, ascending = false)
        assertEquals(listOf("Taxi", "Lunch", "Dinner", "Bus", "apple"), desc[0].items.map { it.title })
    }

    @Test fun empty_yieldsNoSections() {
        SortField.entries.forEach { assertEquals(emptyList<Any>(), layoutExpenseSections(emptyList(), it, true)) }
    }

    // ---- wishlist
    private val cats = listOf(Category(1, "Tech", 0), Category(2, "Home", 0))
    private fun wish(id: Long, title: String, price: Double?, added: Long, cat: Long? = null, mood: String? = null) =
        WishlistItem(id, title, price, price?.let { "USD" }, "", null, null, null, null, added, "WANT", mood, cat)
    private val wishes = listOf(
        wish(1, "Phone", 500.0, 10, cat = 1), wish(2, "Lamp", 40.0, 12, cat = 2),
        wish(3, "Mystery", null, 11), wish(4, "Cable", 9.0, 9, cat = 1),
    )

    @Test fun wishlist_category_groups_noCategoryLast() {
        val s = layoutWishlistSections(wishes, SortField.CATEGORY, true, cats)
        assertEquals(listOf("Home", "Tech", "No category"), s.map { it.header })
        assertEquals(listOf("Phone", "Cable"), s[1].items.map { it.title }) // newest first inside
    }

    @Test fun wishlist_amount_unpricedAlwaysTrail() {
        val desc = layoutWishlistSections(wishes, SortField.AMOUNT, false, cats)
        assertEquals(listOf("Phone", "Lamp", "Cable", "Mystery"), desc.single().items.map { it.title })
        val asc = layoutWishlistSections(wishes, SortField.AMOUNT, true, cats)
        assertEquals(listOf("Cable", "Lamp", "Phone", "Mystery"), asc.single().items.map { it.title })
    }

    @Test fun wishlist_date_flatNewestFirst() {
        val s = layoutWishlistSections(wishes, SortField.DATE, false, cats)
        assertNull(s.single().header)
        assertEquals(listOf("Lamp", "Mystery", "Phone", "Cable"), s.single().items.map { it.title })
    }
}
