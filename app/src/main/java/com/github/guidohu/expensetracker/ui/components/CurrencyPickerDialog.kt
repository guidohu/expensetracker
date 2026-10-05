package com.github.guidohu.expensetracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.guidohu.expensetracker.data.AppCurrency
import com.github.guidohu.expensetracker.data.filterCurrencies

@Composable
fun CurrencyPickerDialog(
    currentSelection: String? = null,
    onDismiss: () -> Unit,
    onSelect: (AppCurrency) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) { filterCurrencies(query) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose currency") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(340.dp).padding(top = 8.dp),
                ) {
                    items(filtered, key = { it.code }) { currency ->
                        CurrencyOptionRow(currency, selected = currency.code == currentSelection, onClick = { onSelect(currency) })
                        if (currency != filtered.last()) HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
