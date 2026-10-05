package com.github.guidohu.expensetracker.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** TopAppBar that swaps its title for a search field — shared by the Expenses and Wishlist lists. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableTopAppBar(
    title: String,
    isSearching: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchToggle: (Boolean) -> Unit,
) {
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
                IconButton(onClick = { onSearchToggle(true) }) {
                    Icon(Icons.Filled.Search, contentDescription = "Search")
                }
            }
        },
    )
}
