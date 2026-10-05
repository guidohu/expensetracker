package com.github.guidohu.expensetracker.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.guidohu.expensetracker.data.AppContainer
import com.github.guidohu.expensetracker.data.Category
import com.github.guidohu.expensetracker.data.toComposeColor
import com.github.guidohu.expensetracker.ui.SimpleViewModelFactory
import com.github.guidohu.expensetracker.ui.components.EmptyState
import com.github.guidohu.expensetracker.ui.theme.AppCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(container: AppContainer) {
    val viewModel: CategoriesViewModel = viewModel(
        factory = SimpleViewModelFactory { CategoriesViewModel(container.repository) }
    )
    val categories by viewModel.categories.collectAsState()
    val deleteBlocked by viewModel.deleteBlocked.collectAsState()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Categories") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add category")
            }
        }
    ) { padding ->
        if (categories.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Palette,
                title = "No categories yet",
                body = "Create categories like Food or Transport to organize your spending.",
                actionLabel = "Add a category",
                onAction = { showAddDialog = true },
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = padding.calculateTopPadding(), bottom = 96.dp),
            ) {
                items(categories, key = { it.id }) { category ->
                    CategoryRow(
                        category = category,
                        onClick = { editingCategory = category },
                        onDelete = { viewModel.requestDelete(category) },
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        CategoryDialog(
            existing = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, color ->
                viewModel.addCategory(name, color)
                showAddDialog = false
            },
        )
    }

    editingCategory?.let { category ->
        CategoryDialog(
            existing = category,
            onDismiss = { editingCategory = null },
            onConfirm = { name, color ->
                viewModel.updateCategory(category, name, color)
                editingCategory = null
            },
        )
    }

    deleteBlocked?.let { category ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteBlocked,
            title = { Text("Delete \"${category.name}\"?") },
            text = { Text("This category has existing expenses. Deleting it will also delete those expenses — this can't be undone.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDeleteWithExpenses) { Text("Delete anyway") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteBlocked) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun CategoryRow(category: Category, onClick: () -> Unit, onDelete: () -> Unit) {
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
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(category.color.toComposeColor().copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(category.color.toComposeColor())
                )
            }
            Text(
                category.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 16.dp).weight(1f),
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete category",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
