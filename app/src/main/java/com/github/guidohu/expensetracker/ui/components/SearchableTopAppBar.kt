package com.github.guidohu.expensetracker.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.github.guidohu.expensetracker.data.SortField

/** TopAppBar that swaps its title for a search field, and offers a sort menu — shared by the Expenses and Wishlist lists. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableTopAppBar(
    title: String,
    isSearching: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchToggle: (Boolean) -> Unit,
    sortField: SortField,
    onSortFieldSelect: (SortField) -> Unit,
    sortAscending: Boolean,
    onSortAscendingChange: (Boolean) -> Unit,
    sortLabel: (SortField) -> String,
) {
    var showSortMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            if (isSearching) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Search") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(title)
            }
        },
        actions = {
            if (isSearching) {
                IconButton(onClick = {
                    onSearchToggle(false)
                    onQueryChange("")
                }) {
                    Icon(Icons.Filled.Close, contentDescription = "Close search")
                }
            } else {
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                }
                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                    SortField.entries.forEach { field ->
                        DropdownMenuItem(
                            text = { Text(sortLabel(field)) },
                            leadingIcon = {
                                if (field == sortField) {
                                    Icon(Icons.Filled.Check, contentDescription = null)
                                }
                            },
                            onClick = {
                                onSortFieldSelect(field)
                                showSortMenu = false
                            },
                        )
                    }
                    HorizontalDivider()
                    listOf(true to "Ascending", false to "Descending").forEach { (ascending, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            leadingIcon = {
                                if (ascending == sortAscending) {
                                    Icon(Icons.Filled.Check, contentDescription = null)
                                }
                            },
                            onClick = {
                                onSortAscendingChange(ascending)
                                showSortMenu = false
                            },
                        )
                    }
                }
                IconButton(onClick = { onSearchToggle(true) }) {
                    Icon(Icons.Filled.Search, contentDescription = "Search")
                }
            }
        },
    )
}

/** Contextual TopAppBar shown while multi-select is active — replaces [SearchableTopAppBar]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopAppBar(
    selectedCount: Int,
    onCancel: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    TopAppBar(
        title = { Text("$selectedCount selected") },
        navigationIcon = {
            IconButton(onClick = onCancel) {
                Icon(Icons.Filled.Close, contentDescription = "Cancel selection")
            }
        },
        actions = actions,
    )
}
