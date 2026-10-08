package com.github.guidohu.expensetracker.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.guidohu.expensetracker.data.AppContainer
import com.github.guidohu.expensetracker.data.currencyFor
import com.github.guidohu.expensetracker.ui.SimpleViewModelFactory
import com.github.guidohu.expensetracker.ui.components.CurrencyPickerDialog
import com.github.guidohu.expensetracker.ui.theme.AppCard
import com.github.guidohu.expensetracker.util.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(container: AppContainer) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(
        factory = SimpleViewModelFactory { SettingsViewModel(container.userPreferences, container.backupManager) }
    )
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()
    val monthlyBudget by viewModel.monthlyBudget.collectAsState()
    val dailyReminderEnabled by viewModel.dailyReminderEnabled.collectAsState()
    val dailyReminderHour by viewModel.dailyReminderHour.collectAsState()
    val dailyReminderMinute by viewModel.dailyReminderMinute.collectAsState()
    val budgetCongratsEnabled by viewModel.budgetCongratsEnabled.collectAsState()
    val backupBusy by viewModel.backupBusy.collectAsState()
    val backupMessage by viewModel.backupMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    // The backup file the user picked, held while they confirm it will replace their current data.
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    // Holds the toggle a user just turned on until the permission result comes back.
    var pendingEnable by remember { mutableStateOf<(() -> Unit)?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) pendingEnable?.invoke()
        pendingEnable = null
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> if (uri != null) viewModel.exportBackup(context, uri) }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> pendingRestoreUri = uri }

    LaunchedEffect(backupMessage) {
        backupMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.backupMessageShown()
        }
    }

    fun enableWithPermission(onEnabled: () -> Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingEnable = onEnabled
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onEnabled()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxWidth().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
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
            ToggleSettingsRow(
                icon = Icons.Filled.NotificationsActive,
                title = "Daily reminder",
                value = if (dailyReminderEnabled) {
                    "Every day at %02d:%02d".format(dailyReminderHour, dailyReminderMinute)
                } else {
                    "Off"
                },
                checked = dailyReminderEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        enableWithPermission { viewModel.setDailyReminderEnabled(context, true) }
                    } else {
                        viewModel.setDailyReminderEnabled(context, false)
                    }
                },
                onRowClick = { if (dailyReminderEnabled) showTimePicker = true },
                modifier = Modifier.padding(top = 12.dp),
            )
            ToggleSettingsRow(
                icon = Icons.Filled.EmojiEvents,
                title = "Budget congratulations",
                value = "Notify me when I stay under budget, or go a streak without spending",
                checked = budgetCongratsEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        enableWithPermission { viewModel.setBudgetCongratsEnabled(context, true) }
                    } else {
                        viewModel.setBudgetCongratsEnabled(context, false)
                    }
                },
                onRowClick = null,
                modifier = Modifier.padding(top = 12.dp),
            )
            SettingsRow(
                icon = Icons.Filled.CloudUpload,
                title = "Back up data",
                value = "Save all expenses, categories, wishlist and settings to a file",
                onClick = { exportLauncher.launch(viewModel.suggestedBackupFileName()) },
                enabled = !backupBusy,
                modifier = Modifier.padding(top = 12.dp),
            )
            SettingsRow(
                icon = Icons.Filled.CloudDownload,
                title = "Restore from backup",
                value = "Replace all current data with a previous backup",
                onClick = { restoreLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) },
                enabled = !backupBusy,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }

    pendingRestoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            title = { Text("Restore backup?") },
            text = {
                Text("This replaces all expenses, categories, wishlist items and settings currently in the app with the contents of the backup. This can't be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestoreUri = null
                    viewModel.restoreBackup(context, uri)
                }) { Text("Replace my data") }
            },
            dismissButton = { TextButton(onClick = { pendingRestoreUri = null }) { Text("Cancel") } },
        )
    }

    if (showCurrencyPicker) {
        CurrencyPickerDialog(
            currentSelection = defaultCurrency,
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

    if (showTimePicker) {
        ReminderTimePickerDialog(
            initialHour = dailyReminderHour,
            initialMinute = dailyReminderMinute,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                viewModel.setDailyReminderTime(context, hour, minute)
                showTimePicker = false
            },
        )
    }
}

@Composable
private fun ToggleSettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onRowClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = { onRowClick?.invoke() },
        colors = AppCard.colors,
        elevation = AppCard.elevation,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)
    Dialog(onDismissRequest = onDismiss) {
        Card(colors = AppCard.colors, elevation = AppCard.elevation) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Reminder time", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
                TimePicker(state = state)
                Row(modifier = Modifier.padding(top = 16.dp)) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("Save") }
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        colors = AppCard.colors,
        elevation = AppCard.elevation,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
