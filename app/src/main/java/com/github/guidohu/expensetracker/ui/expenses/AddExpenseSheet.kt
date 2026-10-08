package com.github.guidohu.expensetracker.ui.expenses

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.github.guidohu.expensetracker.data.Category
import com.github.guidohu.expensetracker.data.ExpenseWithCategory
import com.github.guidohu.expensetracker.data.Mood
import com.github.guidohu.expensetracker.data.WishlistPriority
import com.github.guidohu.expensetracker.data.moodOrNull
import com.github.guidohu.expensetracker.data.wishlistPriorityOrDefault
import com.github.guidohu.expensetracker.ui.components.AmountCurrencyField
import com.github.guidohu.expensetracker.ui.components.CategoryPicker
import com.github.guidohu.expensetracker.ui.components.CurrencyPickerDialog
import com.github.guidohu.expensetracker.ui.components.MoodPicker
import com.github.guidohu.expensetracker.ui.components.PriorityPicker
import com.github.guidohu.expensetracker.util.formatEpochDayRelative
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Pre-populates a fresh (non-editing) [AddExpenseSheet] — e.g. turning a wishlist item into an expense. */
data class ExpensePrefill(
    val amount: Double?,
    val currencyCode: String?,
    val categoryId: Long?,
    val title: String,
    val notes: String,
    val mood: Mood?,
    val priority: WishlistPriority = WishlistPriority.WANT,
    val url: String? = null,
    val wishlistAddedAt: Long? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseSheet(
    categories: List<Category>,
    defaultCurrency: String,
    isSaving: Boolean,
    existing: ExpenseWithCategory? = null,
    prefill: ExpensePrefill? = null,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onConfirm: (
        amount: Double,
        currencyCode: String,
        categoryId: Long,
        title: String,
        notes: String,
        date: Long,
        mood: Mood?,
        priority: WishlistPriority,
        url: String?,
    ) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var amountText by rememberSaveable {
        mutableStateOf((existing?.amount ?: prefill?.amount)?.let(::formatAmountForEditing) ?: "")
    }
    var title by rememberSaveable { mutableStateOf(existing?.title ?: prefill?.title ?: "") }
    var notes by rememberSaveable { mutableStateOf(existing?.notes ?: prefill?.notes ?: "") }
    var currencyCode by rememberSaveable { mutableStateOf(existing?.currencyCode ?: prefill?.currencyCode ?: defaultCurrency) }
    var selectedCategoryId by rememberSaveable {
        mutableStateOf(existing?.categoryId ?: prefill?.categoryId ?: categories.firstOrNull()?.id)
    }
    var selectedDate by rememberSaveable { mutableStateOf(existing?.date ?: LocalDate.now().toEpochDay()) }
    var selectedMood by rememberSaveable { mutableStateOf(moodOrNull(existing?.mood) ?: prefill?.mood) }
    var priority by rememberSaveable {
        mutableStateOf(
            when {
                existing != null -> wishlistPriorityOrDefault(existing.priority)
                prefill != null -> prefill.priority
                else -> WishlistPriority.WANT
            }
        )
    }
    var urlText by rememberSaveable { mutableStateOf(existing?.url ?: prefill?.url ?: "") }
    val wishlistAddedAt = existing?.wishlistAddedAt ?: prefill?.wishlistAddedAt
    var showDatePicker by remember { mutableStateOf(false) }
    var showCurrencyPicker by remember { mutableStateOf(false) }

    val amountValue = amountText.toDoubleOrNull()
    val isValid = amountValue != null && amountValue > 0.0 && selectedCategoryId != null && title.isNotBlank()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        AddExpenseSheetContent(
            isEditing = existing != null,
            amountText = amountText,
            onAmountChange = { input -> if (input.count { it == '.' } <= 1) amountText = input },
            currencyCode = currencyCode,
            onCurrencyClick = { showCurrencyPicker = true },
            title = title,
            onTitleChange = { title = it },
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onCategorySelect = { selectedCategoryId = it },
            priority = priority,
            onPrioritySelect = { priority = it },
            notes = notes,
            onNotesChange = { notes = it },
            selectedDate = selectedDate,
            onDateClick = { showDatePicker = true },
            selectedMood = selectedMood,
            onMoodSelect = { selectedMood = it },
            urlText = urlText,
            onUrlChange = { urlText = it },
            wishlistAddedAt = wishlistAddedAt,
            isValid = isValid,
            isSaving = isSaving,
            onDelete = onDelete,
            onSave = {
                onConfirm(
                    requireNotNull(amountValue), currencyCode, requireNotNull(selectedCategoryId),
                    title.trim(), notes.trim(), selectedDate, selectedMood,
                    priority, urlText.trim().takeIf { it.isNotBlank() },
                )
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

    if (showCurrencyPicker) {
        CurrencyPickerDialog(
            currentSelection = currencyCode,
            onDismiss = { showCurrencyPicker = false },
            onSelect = { currencyCode = it.code; showCurrencyPicker = false },
        )
    }
}

/** Drops a trailing ".0" so editing a whole-number amount doesn't start with a confusing decimal. */
private fun formatAmountForEditing(amount: Double): String =
    if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()

/** Stateless body of the add-expense sheet, split out from [AddExpenseSheet] so it can be previewed/tested
 * without the surrounding ModalBottomSheet (which renders in a separate platform window). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseSheetContent(
    isEditing: Boolean = false,
    amountText: String,
    onAmountChange: (String) -> Unit,
    currencyCode: String,
    onCurrencyClick: () -> Unit,
    title: String,
    onTitleChange: (String) -> Unit,
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelect: (Long?) -> Unit,
    priority: WishlistPriority = WishlistPriority.WANT,
    onPrioritySelect: (WishlistPriority) -> Unit = {},
    notes: String,
    onNotesChange: (String) -> Unit,
    selectedDate: Long,
    onDateClick: () -> Unit,
    selectedMood: Mood? = null,
    onMoodSelect: (Mood?) -> Unit = {},
    urlText: String = "",
    onUrlChange: (String) -> Unit = {},
    wishlistAddedAt: Long? = null,
    isValid: Boolean,
    isSaving: Boolean,
    onDelete: (() -> Unit)? = null,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 12.dp),
    ) {
        Text(
            if (isEditing) "Edit expense" else "Add expense",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )

        DateRow(
            label = formatEpochDayRelative(selectedDate),
            onClick = onDateClick,
            modifier = Modifier.padding(top = 16.dp),
        )

        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Item") },
            placeholder = { Text("e.g. Socks") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )

        AmountCurrencyField(
            amountText = amountText,
            onAmountChange = onAmountChange,
            currencyCode = currencyCode,
            onCurrencyClick = onCurrencyClick,
            label = "Amount",
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )

        CategoryPicker(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onCategorySelect = onCategorySelect,
            modifier = Modifier.padding(top = 20.dp),
        )

        PriorityPicker(
            priority = priority,
            onSelect = onPrioritySelect,
            modifier = Modifier.padding(top = 20.dp),
        )

        Text(
            "Mood (optional)",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 20.dp, bottom = 10.dp),
        )
        MoodPicker(selected = selectedMood, onSelect = onMoodSelect)

        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Notes (optional)") },
            placeholder = { Text("Any extra detail") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        )

        OutlinedTextField(
            value = urlText,
            onValueChange = onUrlChange,
            label = { Text("URL (optional)") },
            placeholder = { Text("https://…") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        )

        if (wishlistAddedAt != null) {
            DateRow(
                label = "Added to wishlist ${formatEpochDayRelative(wishlistAddedAt)}",
                onClick = null,
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        Button(
            onClick = onSave,
            enabled = isValid && !isSaving,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(if (isEditing) "Save changes" else "Save expense")
            }
        }

        if (isEditing && onDelete != null) {
            OutlinedButton(
                onClick = onDelete,
                enabled = !isSaving,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Delete", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

/** A calendar-icon row used for both the (editable) Date field and the read-only "Added to wishlist on" field. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRow(label: String, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth(),
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
                label,
                modifier = Modifier.padding(start = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
