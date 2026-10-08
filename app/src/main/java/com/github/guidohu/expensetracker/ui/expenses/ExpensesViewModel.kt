package com.github.guidohu.expensetracker.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.guidohu.expensetracker.data.Category
import com.github.guidohu.expensetracker.data.ExchangeRateService
import com.github.guidohu.expensetracker.data.ExpenseRepository
import com.github.guidohu.expensetracker.data.ExpenseWithCategory
import com.github.guidohu.expensetracker.data.ListSection
import com.github.guidohu.expensetracker.data.Mood
import com.github.guidohu.expensetracker.data.SortField
import com.github.guidohu.expensetracker.data.UserPreferences
import com.github.guidohu.expensetracker.data.WishlistPriority
import com.github.guidohu.expensetracker.data.defaultAscending
import com.github.guidohu.expensetracker.data.flatSection
import com.github.guidohu.expensetracker.data.groupedSections
import com.github.guidohu.expensetracker.data.moodOrNull
import com.github.guidohu.expensetracker.data.wishlistPriorityOrDefault
import com.github.guidohu.expensetracker.util.formatEpochDayRelative
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class ExpensesViewModel(
    private val repository: ExpenseRepository,
    private val userPreferences: UserPreferences,
    private val exchangeRateService: ExchangeRateService,
) : ViewModel() {

    val expenses: StateFlow<List<ExpenseWithCategory>> = repository.expenses.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val categories: StateFlow<List<Category>> = repository.categories.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val defaultCurrency: StateFlow<String> = userPreferences.defaultCurrency

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortField = MutableStateFlow(SortField.DATE)
    val sortField: StateFlow<SortField> = _sortField.asStateFlow()

    private val _sortAscending = MutableStateFlow(SortField.DATE.defaultAscending())
    val sortAscending: StateFlow<Boolean> = _sortAscending.asStateFlow()

    /** Picking a different field restarts in that field's natural direction; re-picking the same one keeps it. */
    fun setSortField(field: SortField) {
        if (field == _sortField.value) return
        _sortField.value = field
        _sortAscending.value = field.defaultAscending()
    }

    fun setSortAscending(ascending: Boolean) {
        _sortAscending.value = ascending
    }

    /**
     * [expenses] filtered by [searchQuery] (title, notes, category name, mood), then laid out per
     * [sortField]: Date / Category / Mood become headed groups (days, category names, moods — with
     * the newest first inside each group), while Amount / Item name are one flat ordered list.
     */
    val sections: StateFlow<List<ListSection<ExpenseWithCategory>>> =
        combine(expenses, searchQuery, sortField, sortAscending) { items, query, sort, ascending ->
            val trimmed = query.trim()
            val filtered = if (trimmed.isEmpty()) {
                items
            } else {
                items.filter { expense ->
                    expense.title.contains(trimmed, ignoreCase = true) ||
                        expense.notes.contains(trimmed, ignoreCase = true) ||
                        expense.categoryName.contains(trimmed, ignoreCase = true) ||
                        moodOrNull(expense.mood)?.label?.contains(trimmed, ignoreCase = true) == true
                }
            }
            layoutExpenseSections(filtered, sort, ascending)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private val _events = Channel<String>(Channel.BUFFERED)
    /** One-shot UI messages (snackbar text) — e.g. when an exchange-rate lookup fails. */
    val events: Flow<String> = _events.receiveAsFlow()

    /** Adds a new expense, looking up the historical exchange rate if its currency differs from the default. */
    fun addExpense(
        amount: Double,
        currencyCode: String,
        categoryId: Long,
        title: String,
        notes: String,
        date: Long,
        mood: Mood?,
        priority: WishlistPriority,
        url: String?,
        wishlistAddedAt: Long? = null,
        onComplete: () -> Unit,
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val rate = resolveExchangeRate(currencyCode, date)
            repository.addExpense(amount, currencyCode, rate, categoryId, title.trim(), notes.trim(), date, mood, priority, url, wishlistAddedAt)
            _isSaving.value = false
            onComplete()
        }
    }

    /** Updates an existing expense in place, re-resolving the exchange rate (cheap, and correct if the
     * currency, date, or the app's default currency changed since the expense was first entered). */
    fun updateExpense(
        id: Long,
        amount: Double,
        currencyCode: String,
        categoryId: Long,
        title: String,
        notes: String,
        date: Long,
        mood: Mood?,
        priority: WishlistPriority,
        url: String?,
        wishlistAddedAt: Long? = null,
        onComplete: () -> Unit,
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val rate = resolveExchangeRate(currencyCode, date)
            repository.updateExpense(id, amount, currencyCode, rate, categoryId, title.trim(), notes.trim(), date, mood, priority, url, wishlistAddedAt)
            _isSaving.value = false
            onComplete()
        }
    }

    private suspend fun resolveExchangeRate(currencyCode: String, date: Long): Double {
        val default = userPreferences.defaultCurrency.value
        if (currencyCode == default) return 1.0
        val fetched = exchangeRateService.fetchRate(currencyCode, default, LocalDate.ofEpochDay(date))
        if (fetched == null) {
            _events.send("Couldn't look up the exchange rate — saved at a 1:1 rate for now.")
        }
        return fetched ?: 1.0
    }

    fun deleteExpense(expense: ExpenseWithCategory) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    /** Re-inserts a just-deleted expense exactly as it was (used by the delete Undo action) — no rate re-lookup. */
    fun restoreExpense(expense: ExpenseWithCategory) {
        viewModelScope.launch {
            repository.addExpense(
                amount = expense.amount,
                currencyCode = expense.currencyCode,
                exchangeRate = expense.exchangeRate,
                categoryId = expense.categoryId,
                title = expense.title,
                notes = expense.notes,
                date = expense.date,
                mood = moodOrNull(expense.mood),
                priority = wishlistPriorityOrDefault(expense.priority),
                url = expense.url,
                wishlistAddedAt = expense.wishlistAddedAt,
            )
        }
    }

    /** Bulk delete for multi-select. */
    fun deleteExpenses(items: List<ExpenseWithCategory>) {
        viewModelScope.launch {
            items.forEach { repository.deleteExpense(it) }
        }
    }

    /** Bulk Undo for [deleteExpenses]. */
    fun restoreExpenses(items: List<ExpenseWithCategory>) {
        items.forEach { restoreExpense(it) }
    }
}

/** Lays [filtered] out as headed groups (Date / Category / Mood) or one flat list (Amount / Item name). */
internal fun layoutExpenseSections(
    filtered: List<ExpenseWithCategory>,
    sort: SortField,
    ascending: Boolean,
): List<ListSection<ExpenseWithCategory>> {
    val newestFirst = compareByDescending<ExpenseWithCategory> { it.date }.thenByDescending { it.id }
    return when (sort) {
        SortField.DATE -> groupedSections(
            filtered, ascending,
            groupKey = { it.date },
            header = { formatEpochDayRelative(it) },
            keyComparator = naturalOrder(),
            within = newestFirst,
        )
        SortField.CATEGORY -> groupedSections(
            filtered, ascending,
            groupKey = { it.categoryName },
            header = { it },
            keyComparator = String.CASE_INSENSITIVE_ORDER,
            within = newestFirst,
        )
        SortField.MOOD -> groupedSections(
            filtered, ascending,
            groupKey = { it.mood.orEmpty() },
            header = { moodOrNull(it)?.let { mood -> "${mood.emoji} ${mood.label}" } ?: "No mood" },
            keyComparator = compareBy { moodOrNull(it)?.label.orEmpty().lowercase() },
            within = newestFirst,
            pinLast = { moodOrNull(it) == null },
        )
        SortField.AMOUNT -> flatSection(
            filtered, ascending,
            compareBy<ExpenseWithCategory> { it.amountInDefaultCurrency }.thenByDescending { it.date },
        )
        SortField.NAME -> flatSection(
            filtered, ascending,
            compareBy<ExpenseWithCategory> { it.title.lowercase() }.thenByDescending { it.date },
        )
    }
}
