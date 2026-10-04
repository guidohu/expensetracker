package com.github.guidohu.expensetracker.ui.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.github.guidohu.expensetracker.data.Category
import com.github.guidohu.expensetracker.data.toComposeColor
import com.github.guidohu.expensetracker.util.currencySymbol
import com.github.guidohu.expensetracker.util.formatEpochDayRelative
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseSheet(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, categoryId: Long, note: String, date: Long) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var amountText by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var selectedCategoryId by rememberSaveable { mutableStateOf(categories.firstOrNull()?.id) }
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val amountValue = amountText.toDoubleOrNull()
    val isValid = amountValue != null && amountValue > 0.0 && selectedCategoryId != null

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        AddExpenseSheetContent(
            amountText = amountText,
            onAmountChange = { input -> if (input.count { it == '.' } <= 1) amountText = input },
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onCategorySelect = { selectedCategoryId = it },
            note = note,
            onNoteChange = { note = it },
            selectedDate = selectedDate,
            onDateClick = { showDatePicker = true },
            isValid = isValid,
            onSave = {
                onConfirm(requireNotNull(amountValue), requireNotNull(selectedCategoryId), note.trim(), selectedDate)
            },
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.ofEpochDay(selectedDate)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
                            .toLocalDate().toEpochDay()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/** Stateless body of the add-expense sheet, split out from [AddExpenseSheet] so it can be previewed/tested
 * without the surrounding ModalBottomSheet (which renders in a separate platform window). */
@Composable
fun AddExpenseSheetContent(
    amountText: String,
    onAmountChange: (String) -> Unit,
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelect: (Long) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    selectedDate: Long,
    onDateClick: () -> Unit,
    isValid: Boolean,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 12.dp),
    ) {
        Text("Add expense", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

        OutlinedTextField(
            value = amountText,
            onValueChange = onAmountChange,
            label = { Text("Amount") },
            leadingIcon = { Text(currencySymbol(), style = MaterialTheme.typography.headlineSmall) },
            textStyle = MaterialTheme.typography.headlineSmall,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )

        Text(
            "Category",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 20.dp, bottom = 10.dp),
        )
        if (categories.isEmpty()) {
            Text("Add a category first from the Categories tab.")
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories, key = { it.id }) { category ->
                    FilterChip(
                        selected = category.id == selectedCategoryId,
                        onClick = { onCategorySelect(category.id) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(category.color.toComposeColor())
                            )
                        },
                        label = { Text(category.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = category.color.toComposeColor().copy(alpha = 0.22f),
                        ),
                    )
                }
            }
        }

        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            label = { Text("Note (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        )

        Surface(
            onClick = onDateClick,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Icon(
                    Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    formatEpochDayRelative(selectedDate),
                    modifier = Modifier.padding(start = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        Button(
            onClick = onSave,
            enabled = isValid,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        ) {
            Text("Save expense")
        }
    }
}
