package com.github.guidohu.expensetracker.ui.expenses

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.github.guidohu.expensetracker.data.AppContainer
import com.github.guidohu.expensetracker.data.ExpenseWithCategory
import com.github.guidohu.expensetracker.data.SortField
import com.github.guidohu.expensetracker.data.moodOrNull
import com.github.guidohu.expensetracker.data.toComposeColor
import com.github.guidohu.expensetracker.ui.SimpleViewModelFactory
import com.github.guidohu.expensetracker.ui.categories.CategoryDialog
import com.github.guidohu.expensetracker.ui.components.EmptyState
import com.github.guidohu.expensetracker.ui.components.SearchableTopAppBar
import com.github.guidohu.expensetracker.ui.components.SelectionTopAppBar
import com.github.guidohu.expensetracker.ui.components.SwipeActionRow
import com.github.guidohu.expensetracker.ui.theme.AppCard
import com.github.guidohu.expensetracker.util.formatCurrency
import com.github.guidohu.expensetracker.util.formatEpochDayRelative
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    container: AppContainer,
    autoOpenAddSheet: Boolean = false,
    onAutoOpenConsumed: () -> Unit = {},
) {
    val viewModel: ExpensesViewModel = viewModel(
        factory = SimpleViewModelFactory {
            ExpensesViewModel(container.repository, container.userPreferences, container.exchangeRateService)
        }
    )
    val allExpenses by viewModel.expenses.collectAsState()
    val sections by viewModel.sections.collectAsState()
    val expenses = remember(sections) { sections.flatMap { it.items } }
    val categories by viewModel.categories.collectAsState()
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortField by viewModel.sortField.collectAsState()
    val sortAscending by viewModel.sortAscending.collectAsState()
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var showAddCategoryDialog by rememberSaveable { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<ExpenseWithCategory?>(null) }
    var selectedIds by remember { mutableStateOf(emptySet<Long>()) }
    var revealedId by remember { mutableStateOf<Long?>(null) }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Long)
        }
    }

    LaunchedEffect(autoOpenAddSheet) {
        if (autoOpenAddSheet) {
            showAddSheet = true
            onAutoOpenConsumed()
        }
    }

    Scaffold(
        topBar = {
            if (selectedIds.isNotEmpty()) {
                SelectionTopAppBar(
                    selectedCount = selectedIds.size,
                    onCancel = { selectedIds = emptySet() },
                ) {
                    IconButton(onClick = { showBulkDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete selected")
                    }
                }
            } else {
                SearchableTopAppBar(
                    title = "Expenses",
                    isSearching = isSearching,
                    query = searchQuery,
                    onQueryChange = viewModel::setSearchQuery,
                    onSearchToggle = { isSearching = it },
                    sortField = sortField,
                    onSortFieldSelect = viewModel::setSortField,
                    sortAscending = sortAscending,
                    onSortAscendingChange = viewModel::setSortAscending,
                    sortLabel = ::expenseSortLabel,
                )
            }
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
            AnimatedVisibility(visible = categories.isNotEmpty() && selectedIds.isEmpty()) {
                FloatingActionButton(onClick = { showAddSheet = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add expense")
                }
            }
        }
    ) { padding ->
        when {
            categories.isEmpty() -> EmptyState(
                icon = Icons.Filled.Savings,
                title = "Create a category to get started",
                body = "Categories group your spending so stats actually mean something.",
                actionLabel = "Add a category",
                onAction = { showAddCategoryDialog = true },
                modifier = Modifier.padding(padding),
            )
            allExpenses.isEmpty() -> EmptyState(
                icon = Icons.Filled.Receipt,
                title = "No expenses yet",
                body = "Tap the + button to log your first expense.",
                actionLabel = "Add expense",
                onAction = { showAddSheet = true },
                modifier = Modifier.padding(padding),
            )
            expenses.isEmpty() -> EmptyState(
                icon = Icons.Filled.Receipt,
                title = "No matching expenses",
                body = "Try a different search term.",
                modifier = Modifier.padding(padding),
            )
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        top = padding.calculateTopPadding(),
                        end = 8.dp,
                        bottom = 96.dp,
                    ),
                ) {
                    sections.forEach { section ->
                        section.header?.let { header ->
                            item(key = "header_${section.key}") {
                                Text(
                                    text = header,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                )
                            }
                        }
                        items(section.items, key = { it.id }) { expense ->
                            SwipeActionRow(
                                isSelected = expense.id in selectedIds,
                                selectionModeActive = selectedIds.isNotEmpty(),
                                isRevealed = revealedId == expense.id,
                                onRevealedChange = { revealed -> revealedId = if (revealed) expense.id else null },
                                onTap = { editingExpense = expense },
                                onToggleSelect = {
                                    selectedIds = if (expense.id in selectedIds) {
                                        selectedIds - expense.id
                                    } else {
                                        selectedIds + expense.id
                                    }
                                },
                                onSwipeLeftDelete = {
                                    revealedId = null
                                    viewModel.deleteExpense(expense)
                                    coroutineScope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Deleted ${expense.title} · ${formatCurrency(expense.amount, expense.currencyCode)}",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short,
                                        )
                                        if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                            viewModel.restoreExpense(expense)
                                        }
                                    }
                                },
                            ) {
                                ExpenseRow(expense = expense, defaultCurrency = defaultCurrency)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddExpenseSheet(
            categories = categories,
            defaultCurrency = defaultCurrency,
            isSaving = isSaving,
            onDismiss = { showAddSheet = false },
            onConfirm = { amount, currencyCode, categoryId, title, notes, date, mood, priority, url ->
                viewModel.addExpense(amount, currencyCode, categoryId, title, notes, date, mood, priority, url) {
                    showAddSheet = false
                }
            },
        )
    }

    editingExpense?.let { expense ->
        AddExpenseSheet(
            categories = categories,
            defaultCurrency = defaultCurrency,
            isSaving = isSaving,
            existing = expense,
            onDismiss = { editingExpense = null },
            onDelete = {
                editingExpense = null
                revealedId = null
                viewModel.deleteExpense(expense)
                coroutineScope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "Deleted ${expense.title} · ${formatCurrency(expense.amount, expense.currencyCode)}",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                        viewModel.restoreExpense(expense)
                    }
                }
            },
            onConfirm = { amount, currencyCode, categoryId, title, notes, date, mood, priority, url ->
                viewModel.updateExpense(expense.id, amount, currencyCode, categoryId, title, notes, date, mood, priority, url, expense.wishlistAddedAt) {
                    editingExpense = null
                }
            },
        )
    }

    if (showAddCategoryDialog) {
        CategoryDialog(
            existing = null,
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { name, color ->
                coroutineScope.launch { container.repository.addCategory(name, color) }
                showAddCategoryDialog = false
            },
        )
    }

    if (showBulkDeleteConfirm) {
        val toDelete = expenses.filter { it.id in selectedIds }
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            title = { Text("Delete ${toDelete.size} expenses?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteExpenses(toDelete)
                    selectedIds = emptySet()
                    showBulkDeleteConfirm = false
                    coroutineScope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = "Deleted ${toDelete.size} expenses",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short,
                        )
                        if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                            viewModel.restoreExpenses(toDelete)
                        }
                    }
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

private fun expenseSortLabel(field: SortField): String = when (field) {
    SortField.DATE -> "Date"
    SortField.CATEGORY -> "Category"
    SortField.AMOUNT -> "Amount"
    SortField.NAME -> "Item name"
    SortField.MOOD -> "Mood"
}

@Composable
private fun ExpenseRow(expense: ExpenseWithCategory, defaultCurrency: String) {
    Card(
        colors = AppCard.colors,
        elevation = AppCard.elevation,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(expense.categoryColor.toComposeColor().copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(expense.categoryColor.toComposeColor())
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        expense.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    moodOrNull(expense.mood)?.let { mood ->
                        Text(mood.emoji, modifier = Modifier.padding(start = 6.dp))
                    }
                }
                val subtitle = if (expense.notes.isNotBlank()) {
                    "${expense.categoryName} · ${expense.notes}"
                } else {
                    expense.categoryName
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatCurrency(expense.amountInDefaultCurrency, defaultCurrency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (expense.currencyCode != defaultCurrency) {
                    Text(
                        formatCurrency(expense.amount, expense.currencyCode),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
