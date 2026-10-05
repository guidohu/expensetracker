package com.github.guidohu.expensetracker.ui.wishlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.github.guidohu.expensetracker.data.AppContainer
import com.github.guidohu.expensetracker.data.WishlistItem
import com.github.guidohu.expensetracker.data.moodOrNull
import com.github.guidohu.expensetracker.data.wishlistPriorityOrDefault
import com.github.guidohu.expensetracker.ui.SimpleViewModelFactory
import com.github.guidohu.expensetracker.ui.components.EmptyState
import com.github.guidohu.expensetracker.ui.components.SearchableTopAppBar
import com.github.guidohu.expensetracker.ui.theme.AppCard
import com.github.guidohu.expensetracker.util.formatCurrency
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(
    container: AppContainer,
    prefillUrl: String? = null,
    onPrefillConsumed: () -> Unit = {},
) {
    val viewModel: WishlistViewModel = viewModel(
        factory = SimpleViewModelFactory {
            WishlistViewModel(container.wishlistRepository, container.userPreferences, container.urlPreviewService)
        }
    )
    val allItems by viewModel.items.collectAsState()
    val items by viewModel.filteredItems.collectAsState()
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val previewState by viewModel.previewState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var addSheetInitialUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var editingItem by remember { mutableStateOf<WishlistItem?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(prefillUrl) {
        if (prefillUrl != null) {
            addSheetInitialUrl = prefillUrl
            viewModel.seedExistingPreview(null)
            viewModel.onUrlChanged(prefillUrl)
            showAddSheet = true
            onPrefillConsumed()
        }
    }

    Scaffold(
        topBar = {
            SearchableTopAppBar(
                title = "Wishlist",
                isSearching = isSearching,
                query = searchQuery,
                onQueryChange = viewModel::setSearchQuery,
                onSearchToggle = { isSearching = it },
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                addSheetInitialUrl = null
                viewModel.seedExistingPreview(null)
                showAddSheet = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Add wishlist item")
            }
        }
    ) { padding ->
        when {
            allItems.isEmpty() -> EmptyState(
                icon = Icons.Filled.CardGiftcard,
                title = "Your wishlist is empty",
                body = "Tap the + button to add something you want to buy.",
                actionLabel = "Add item",
                onAction = { addSheetInitialUrl = null; viewModel.seedExistingPreview(null); showAddSheet = true },
                modifier = Modifier.padding(padding),
            )
            items.isEmpty() -> EmptyState(
                icon = Icons.Filled.CardGiftcard,
                title = "No matching items",
                body = "Try a different search term.",
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 8.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    end = 8.dp,
                    bottom = 96.dp,
                ),
            ) {
                items(items, key = { it.id }) { item ->
                    WishlistRow(
                        item = item,
                        defaultCurrency = defaultCurrency,
                        onClick = {
                            viewModel.seedExistingPreview(item)
                            editingItem = item
                        },
                        onDelete = {
                            viewModel.deleteItem(item)
                            coroutineScope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Deleted ${item.title}",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short,
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.restoreItem(item)
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        AddWishlistSheet(
            defaultCurrency = defaultCurrency,
            isSaving = isSaving,
            previewState = previewState,
            onUrlChanged = viewModel::onUrlChanged,
            initialUrl = addSheetInitialUrl,
            onDismiss = { showAddSheet = false },
            onConfirm = { title, price, currencyCode, note, url, priority, mood ->
                viewModel.addItem(title, price, currencyCode, note, url, priority, mood) {
                    showAddSheet = false
                }
            },
        )
    }

    editingItem?.let { item ->
        AddWishlistSheet(
            defaultCurrency = defaultCurrency,
            isSaving = isSaving,
            previewState = previewState,
            onUrlChanged = viewModel::onUrlChanged,
            existing = item,
            onDismiss = { editingItem = null },
            onConfirm = { title, price, currencyCode, note, url, priority, mood ->
                viewModel.updateItem(item, title, price, currencyCode, note, url, priority, mood) {
                    editingItem = null
                }
            },
        )
    }
}

@Composable
private fun WishlistRow(item: WishlistItem, defaultCurrency: String, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        onClick = onClick,
        colors = AppCard.colors,
        elevation = AppCard.elevation,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (item.previewImageUrl != null) {
                AsyncImage(
                    model = item.previewImageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (item.url != null) Icons.Filled.Link else Icons.Filled.CardGiftcard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    moodOrNull(item.mood)?.let { mood ->
                        Text(mood.emoji, modifier = Modifier.padding(start = 6.dp))
                    }
                }
                val subtitle = wishlistPriorityOrDefault(item.priority).label +
                    (item.previewTitle ?: item.note).takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (item.price != null && item.currencyCode != null) {
                Text(
                    formatCurrency(item.price, item.currencyCode),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete item", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
