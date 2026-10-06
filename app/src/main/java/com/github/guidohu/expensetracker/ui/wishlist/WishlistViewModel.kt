package com.github.guidohu.expensetracker.ui.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.guidohu.expensetracker.data.Category
import com.github.guidohu.expensetracker.data.ExchangeRateService
import com.github.guidohu.expensetracker.data.ExpenseRepository
import com.github.guidohu.expensetracker.data.Mood
import com.github.guidohu.expensetracker.data.UrlPreview
import com.github.guidohu.expensetracker.data.UrlPreviewService
import com.github.guidohu.expensetracker.data.UserPreferences
import com.github.guidohu.expensetracker.data.WishlistItem
import com.github.guidohu.expensetracker.data.WishlistPriority
import com.github.guidohu.expensetracker.data.WishlistRepository
import com.github.guidohu.expensetracker.data.moodOrNull
import com.github.guidohu.expensetracker.data.wishlistPriorityOrDefault
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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

sealed interface PreviewState {
    data object Idle : PreviewState
    data object Loading : PreviewState
    data class Loaded(val preview: UrlPreview) : PreviewState
    data object Failed : PreviewState
}

class WishlistViewModel(
    private val repository: WishlistRepository,
    userPreferences: UserPreferences,
    private val urlPreviewService: UrlPreviewService,
    private val expenseRepository: ExpenseRepository,
    private val exchangeRateService: ExchangeRateService,
) : ViewModel() {

    val items: StateFlow<List<WishlistItem>> = repository.items.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val defaultCurrency: StateFlow<String> = userPreferences.defaultCurrency

    val categories: StateFlow<List<Category>> = expenseRepository.categories.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _events = Channel<String>(Channel.BUFFERED)
    /** One-shot UI messages (snackbar text) — e.g. when an exchange-rate lookup fails. */
    val events: Flow<String> = _events.receiveAsFlow()

    /** [items] filtered by [searchQuery] against title, note, link preview title, mood, and priority. */
    val filteredItems: StateFlow<List<WishlistItem>> = combine(items, searchQuery) { list, query ->
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@combine list
        list.filter { item ->
            item.title.contains(trimmed, ignoreCase = true) ||
                item.note.contains(trimmed, ignoreCase = true) ||
                item.previewTitle?.contains(trimmed, ignoreCase = true) == true ||
                moodOrNull(item.mood)?.label?.contains(trimmed, ignoreCase = true) == true ||
                wishlistPriorityOrDefault(item.priority).label.contains(trimmed, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private val _previewState = MutableStateFlow<PreviewState>(PreviewState.Idle)
    val previewState: StateFlow<PreviewState> = _previewState.asStateFlow()
    private var previewJob: Job? = null

    /** Called once when opening the sheet for an existing item, so an unchanged URL doesn't need a re-fetch. */
    fun seedExistingPreview(item: WishlistItem?) {
        previewJob?.cancel()
        _previewState.value = if (item?.previewTitle != null || item?.previewDescription != null || item?.previewImageUrl != null) {
            PreviewState.Loaded(UrlPreview(item.previewTitle, item.previewDescription, item.previewImageUrl))
        } else {
            PreviewState.Idle
        }
    }

    /** Debounces the fetch so it only fires once the user pauses typing — the WhatsApp-style preview. */
    fun onUrlChanged(url: String) {
        previewJob?.cancel()
        val trimmed = url.trim()
        if (trimmed.isEmpty() || !looksLikeUrl(trimmed)) {
            _previewState.value = PreviewState.Idle
            return
        }
        previewJob = viewModelScope.launch {
            delay(600)
            _previewState.value = PreviewState.Loading
            val preview = urlPreviewService.fetch(normalizeUrl(trimmed))
            _previewState.value = if (preview != null) PreviewState.Loaded(preview) else PreviewState.Failed
        }
    }

    fun addItem(
        title: String,
        price: Double?,
        currencyCode: String?,
        note: String,
        url: String?,
        priority: WishlistPriority,
        mood: Mood?,
        categoryId: Long?,
        onComplete: () -> Unit,
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val preview = (previewState.value as? PreviewState.Loaded)?.preview
            repository.addItem(
                title = title.trim(),
                price = price,
                currencyCode = currencyCode,
                note = note.trim(),
                url = url?.trim()?.takeIf { it.isNotBlank() },
                previewTitle = preview?.title,
                previewDescription = preview?.description,
                previewImageUrl = preview?.imageUrl,
                createdAt = LocalDate.now().toEpochDay(),
                priority = priority,
                mood = mood,
                categoryId = categoryId,
            )
            _isSaving.value = false
            onComplete()
        }
    }

    fun updateItem(
        existing: WishlistItem,
        title: String,
        price: Double?,
        currencyCode: String?,
        note: String,
        url: String?,
        priority: WishlistPriority,
        mood: Mood?,
        categoryId: Long?,
        onComplete: () -> Unit,
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val preview = (previewState.value as? PreviewState.Loaded)?.preview
            repository.updateItem(
                id = existing.id,
                title = title.trim(),
                price = price,
                currencyCode = currencyCode,
                note = note.trim(),
                url = url?.trim()?.takeIf { it.isNotBlank() },
                previewTitle = preview?.title,
                previewDescription = preview?.description,
                previewImageUrl = preview?.imageUrl,
                createdAt = existing.createdAt,
                priority = priority,
                mood = mood,
                categoryId = categoryId,
            )
            _isSaving.value = false
            onComplete()
        }
    }

    fun deleteItem(item: WishlistItem) {
        viewModelScope.launch { repository.deleteItem(item) }
    }

    /** Re-inserts a just-deleted item exactly as it was (used by the delete Undo action). */
    fun restoreItem(item: WishlistItem) {
        viewModelScope.launch {
            repository.addItem(
                title = item.title,
                price = item.price,
                currencyCode = item.currencyCode,
                note = item.note,
                url = item.url,
                previewTitle = item.previewTitle,
                previewDescription = item.previewDescription,
                previewImageUrl = item.previewImageUrl,
                createdAt = item.createdAt,
                priority = wishlistPriorityOrDefault(item.priority),
                mood = moodOrNull(item.mood),
                categoryId = item.categoryId,
            )
        }
    }

    /** Turns a wishlist entry into a real expense — looking up the exchange rate like
     * [com.github.guidohu.expensetracker.ui.expenses.ExpensesViewModel.addExpense] does — then
     * removes it from the wishlist. */
    fun moveToExpense(
        item: WishlistItem,
        amount: Double,
        currencyCode: String,
        categoryId: Long,
        title: String,
        notes: String,
        date: Long,
        mood: Mood?,
        onComplete: () -> Unit,
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val default = defaultCurrency.value
            val rate = if (currencyCode == default) {
                1.0
            } else {
                val fetched = exchangeRateService.fetchRate(currencyCode, default, LocalDate.ofEpochDay(date))
                if (fetched == null) _events.send("Couldn't look up the exchange rate — saved at a 1:1 rate for now.")
                fetched ?: 1.0
            }
            expenseRepository.addExpense(amount, currencyCode, rate, categoryId, title.trim(), notes.trim(), date, mood)
            repository.deleteItem(item)
            _isSaving.value = false
            onComplete()
        }
    }
}

private fun looksLikeUrl(text: String): Boolean = text.contains('.') && !text.contains(' ')

private fun normalizeUrl(text: String): String =
    if (text.startsWith("http://", ignoreCase = true) || text.startsWith("https://", ignoreCase = true)) {
        text
    } else {
        "https://$text"
    }
