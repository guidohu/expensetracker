package com.hungerbuehler.expensetracker.ui.expenses

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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hungerbuehler.expensetracker.data.ExpenseRepository
import com.hungerbuehler.expensetracker.data.ExpenseWithCategory
import com.hungerbuehler.expensetracker.data.toComposeColor
import com.hungerbuehler.expensetracker.ui.RepositoryViewModelFactory
import com.hungerbuehler.expensetracker.ui.components.EmptyState
import com.hungerbuehler.expensetracker.util.formatCurrency
import com.hungerbuehler.expensetracker.util.formatEpochDayRelative
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    repository: ExpenseRepository,
    onNavigateToCategories: () -> Unit = {},
) {
    val viewModel: ExpensesViewModel = viewModel(
        factory = RepositoryViewModelFactory(repository) { ExpensesViewModel(it) }
    )
    val expenses by viewModel.expenses.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Expenses") }) },
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
            AnimatedVisibility(visible = categories.isNotEmpty()) {
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
                onAction = onNavigateToCategories,
                modifier = Modifier.padding(padding),
            )
            expenses.isEmpty() -> EmptyState(
                icon = Icons.Filled.Receipt,
                title = "No expenses yet",
                body = "Tap the + button to log your first expense.",
                actionLabel = "Add expense",
                onAction = { showAddSheet = true },
                modifier = Modifier.padding(padding),
            )
            else -> {
                val grouped = remember(expenses) { expenses.groupBy { it.date } }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        top = padding.calculateTopPadding(),
                        end = 8.dp,
                        bottom = 96.dp,
                    ),
                ) {
                    grouped.forEach { (date, dayExpenses) ->
                        item(key = "header_$date") {
                            Text(
                                text = formatEpochDayRelative(date),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                        items(dayExpenses, key = { it.id }) { expense ->
                            ExpenseRow(
                                expense = expense,
                                onDelete = {
                                    viewModel.deleteExpense(expense)
                                    coroutineScope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Deleted ${formatCurrency(expense.amount)} · ${expense.categoryName}",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short,
                                        )
                                        if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                            viewModel.addExpense(
                                                expense.amount, expense.categoryId, expense.note, expense.date
                                            )
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddExpenseSheet(
            categories = categories,
            onDismiss = { showAddSheet = false },
            onConfirm = { amount, categoryId, note, date ->
                viewModel.addExpense(amount, categoryId, note, date)
                showAddSheet = false
            },
        )
    }
}

@Composable
private fun ExpenseRow(expense: ExpenseWithCategory, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
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
                Text(
                    expense.categoryName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                if (expense.note.isNotBlank()) {
                    Text(
                        expense.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                formatCurrency(expense.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete expense",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
