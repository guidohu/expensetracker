package com.github.guidohu.expensetracker.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.guidohu.expensetracker.data.AppContainer
import com.github.guidohu.expensetracker.data.currencyFor
import com.github.guidohu.expensetracker.ui.SimpleViewModelFactory
import com.github.guidohu.expensetracker.ui.components.CurrencyPickerDialog
import com.github.guidohu.expensetracker.util.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(container: AppContainer) {
    val viewModel: SettingsViewModel = viewModel(
        factory = SimpleViewModelFactory { SettingsViewModel(container.userPreferences) }
    )
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()
    val monthlyBudget by viewModel.monthlyBudget.collectAsState()
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(modifier = Modifier.fillMaxWidth().padding(padding).padding(16.dp)) {
            SettingsRow(
                icon = Icons.Filled.AttachMoney,
                title = "Default currency",
                value = "${defaultCurrency} — ${currencyFor(defaultCurrency).displayName}",
                onClick = { showCurrencyPicker = true },
            )
            SettingsRow(
                icon = Icons.Filled.Savings,
                title = "Monthly budget",
                value = monthlyBudget?.let { formatCurrency(it, defaultCurrency) } ?: "Not set",
                onClick = { showBudgetDialog = true },
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }

    if (showCurrencyPicker) {
        CurrencyPickerDialog(
            onDismiss = { showCurrencyPicker = false },
            onSelect = { viewModel.setDefaultCurrency(it.code); showCurrencyPicker = false },
        )
    }

    if (showBudgetDialog) {
        BudgetDialog(
            initialValue = monthlyBudget,
            currencyCode = defaultCurrency,
            onDismiss = { showBudgetDialog = false },
            onSave = { viewModel.setMonthlyBudget(it); showBudgetDialog = false },
        )
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun BudgetDialog(
    initialValue: Double?,
    currencyCode: String,
    onDismiss: () -> Unit,
    onSave: (Double?) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initialValue?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Monthly budget") },
        text = {
            Column {
                Text(
                    "Set a target to track your spending against each month, in $currencyCode.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { input -> if (input.count { it == '.' } <= 1) text = input },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text.toDoubleOrNull()?.takeIf { it > 0 }) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = { onSave(null) }) { Text("Clear") }
        },
    )
}
