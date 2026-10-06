package com.github.guidohu.expensetracker.ui.wishlist

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
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.github.guidohu.expensetracker.data.Category
import com.github.guidohu.expensetracker.data.Mood
import com.github.guidohu.expensetracker.data.WishlistItem
import com.github.guidohu.expensetracker.data.WishlistPriority
import com.github.guidohu.expensetracker.data.moodOrNull
import com.github.guidohu.expensetracker.data.toComposeColor
import com.github.guidohu.expensetracker.data.wishlistPriorityOrDefault
import com.github.guidohu.expensetracker.ui.components.CurrencyPickerDialog
import com.github.guidohu.expensetracker.ui.components.MoodPicker
import com.github.guidohu.expensetracker.ui.theme.AppCard
import com.github.guidohu.expensetracker.util.currencySymbol

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWishlistSheet(
    defaultCurrency: String,
    isSaving: Boolean,
    previewState: PreviewState,
    categories: List<Category>,
    onUrlChanged: (String) -> Unit,
    existing: WishlistItem? = null,
    initialUrl: String? = null,
    onDismiss: () -> Unit,
    onMoveToExpense: (() -> Unit)? = null,
    onConfirm: (
        title: String,
        price: Double?,
        currencyCode: String?,
        note: String,
        url: String?,
        priority: WishlistPriority,
        mood: Mood?,
        categoryId: Long?,
    ) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var priceText by rememberSaveable { mutableStateOf(existing?.price?.let(::formatAmountForEditing) ?: "") }
    var currencyCode by rememberSaveable { mutableStateOf(existing?.currencyCode ?: defaultCurrency) }
    var note by rememberSaveable { mutableStateOf(existing?.note ?: "") }
    var urlText by rememberSaveable { mutableStateOf(existing?.url ?: initialUrl ?: "") }
    var priority by rememberSaveable { mutableStateOf(wishlistPriorityOrDefault(existing?.priority)) }
    var selectedMood by rememberSaveable { mutableStateOf(moodOrNull(existing?.mood)) }
    var selectedCategoryId by rememberSaveable { mutableStateOf(existing?.categoryId) }
    var showCurrencyPicker by rememberSaveable { mutableStateOf(false) }

    val isValid = title.isNotBlank()
    val priceValue = priceText.toDoubleOrNull()?.takeIf { it > 0.0 }

    // Once the link preview resolves, default the title to the page's title — but only while the
    // user hasn't typed one themselves, so this never clobbers a manual entry or an edited item.
    LaunchedEffect(previewState) {
        val loadedTitle = (previewState as? PreviewState.Loaded)?.preview?.title
        if (loadedTitle != null && title.isBlank()) {
            title = loadedTitle
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 12.dp),
        ) {
            Text(
                if (existing == null) "Add to wishlist" else "Edit wishlist item",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("What") },
                placeholder = { Text("e.g. Noise-cancelling headphones") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 16.dp),
            ) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { input -> if (input.count { it == '.' } <= 1) priceText = input },
                    label = { Text("Price (optional)") },
                    leadingIcon = { Text(currencySymbol(currencyCode), style = MaterialTheme.typography.headlineSmall) },
                    textStyle = MaterialTheme.typography.headlineSmall,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                AssistChip(
                    onClick = { showCurrencyPicker = true },
                    label = { Text(currencyCode) },
                    modifier = Modifier.padding(start = 10.dp),
                )
            }

            Text(
                "Priority",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 20.dp, bottom = 10.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WishlistPriority.entries.forEach { option ->
                    FilterChip(
                        selected = priority == option,
                        onClick = { priority = option },
                        label = { Text(option.label) },
                    )
                }
            }

            if (categories.isNotEmpty()) {
                Text(
                    "Category (optional)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 20.dp, bottom = 10.dp),
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories, key = { it.id }) { category ->
                        FilterChip(
                            selected = category.id == selectedCategoryId,
                            onClick = {
                                selectedCategoryId = if (selectedCategoryId == category.id) null else category.id
                            },
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
                value = urlText,
                onValueChange = { urlText = it; onUrlChanged(it) },
                label = { Text("Link (optional)") },
                placeholder = { Text("https://…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            )

            LinkPreviewCard(previewState, modifier = Modifier.padding(top = 10.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                placeholder = { Text("Any extra detail") },
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            )

            Text(
                "Mood (optional)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 20.dp, bottom = 10.dp),
            )
            MoodPicker(selected = selectedMood, onSelect = { selectedMood = it })

            Button(
                onClick = {
                    onConfirm(
                        title.trim(),
                        priceValue,
                        priceValue?.let { currencyCode },
                        note.trim(),
                        urlText.trim().takeIf { it.isNotBlank() },
                        priority,
                        selectedMood,
                        selectedCategoryId,
                    )
                },
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
                    Text(if (existing == null) "Save to wishlist" else "Save changes")
                }
            }

            if (existing != null && onMoveToExpense != null) {
                OutlinedButton(
                    onClick = onMoveToExpense,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                ) {
                    Icon(Icons.Filled.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Move to expense", modifier = Modifier.padding(start = 8.dp))
                }
            }
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

@Composable
private fun LinkPreviewCard(state: PreviewState, modifier: Modifier = Modifier) {
    when (state) {
        is PreviewState.Idle -> Unit
        is PreviewState.Loading -> Card(colors = AppCard.colors, elevation = AppCard.elevation, modifier = modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(
                    "Fetching preview…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
        }
        is PreviewState.Failed -> Text(
            "No preview available for this link.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        is PreviewState.Loaded -> Card(colors = AppCard.colors, elevation = AppCard.elevation, modifier = modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (state.preview.imageUrl != null) {
                    AsyncImage(
                        model = state.preview.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                    )
                }
                Column(modifier = Modifier.padding(start = if (state.preview.imageUrl != null) 12.dp else 0.dp)) {
                    if (state.preview.title != null) {
                        Text(
                            state.preview.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (state.preview.description != null) {
                        Text(
                            state.preview.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

private fun formatAmountForEditing(amount: Double): String =
    if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()
