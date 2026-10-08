package com.github.guidohu.expensetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.github.guidohu.expensetracker.data.Category
import com.github.guidohu.expensetracker.data.WishlistPriority
import com.github.guidohu.expensetracker.data.toComposeColor

/** Space [OutlinedTextField] reserves above its outline for the floating label (Material3's OutlinedTextFieldTopPadding). */
private val LabelStrip = 8.dp

/**
 * Amount input paired with a currency selector chip, shared by the expense and wishlist sheets.
 *
 * The chip's height must exactly match the text field's visible outline, but [OutlinedTextField]
 * doesn't report an accurate intrinsic height (so `Modifier.height(IntrinsicSize.Min)` undershoots)
 * and measuring it post-hoc via `onSizeChanged` lags a frame behind on first composition. Instead,
 * this uses a custom [Layout]: the field is measured first (picking its own natural height, the same
 * as any other single-line field), then the chip is measured once, forced to the field's visible
 * outline height — deterministic, no timing dependency, no lag.
 */
@Composable
fun AmountCurrencyField(
    amountText: String,
    onAmountChange: (String) -> Unit,
    currencyCode: String,
    onCurrencyClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val spacing = 10.dp
    Layout(
        contents = listOf<@Composable () -> Unit>(
            {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = onAmountChange,
                    label = { Text(label) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
            },
            {
                AssistChip(
                    onClick = onCurrencyClick,
                    label = { Text(currencyCode) },
                )
            },
        ),
        modifier = modifier,
    ) { (fieldMeasurables, chipMeasurables), constraints ->
        val fieldMeasurable = fieldMeasurables.first()
        val chipMeasurable = chipMeasurables.first()
        val spacingPx = spacing.roundToPx()
        val maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else Constraints.Infinity

        // Width-only probe (an intrinsic query, not a real measurement) so the field knows how
        // much space to leave for the chip — a chip's width doesn't depend on its height.
        val chipNaturalWidth = chipMeasurable.maxIntrinsicWidth(Constraints.Infinity)
        val fieldWidth = (maxWidth - chipNaturalWidth - spacingPx).coerceAtLeast(0)

        val fieldPlaceable = fieldMeasurable.measure(Constraints(minWidth = fieldWidth, maxWidth = fieldWidth))

        // OutlinedTextField reserves an invisible strip above its outline for the floating label,
        // so its measured height is taller than the box you actually see. Match the chip to the
        // visible outline (measured height minus that strip) and offset it down by the same amount.
        val labelStripPx = LabelStrip.roundToPx()
        val chipHeight = (fieldPlaceable.height - labelStripPx).coerceAtLeast(0)
        val chipPlaceable = chipMeasurable.measure(Constraints(minHeight = chipHeight, maxHeight = chipHeight))

        layout(fieldPlaceable.width + spacingPx + chipPlaceable.width, fieldPlaceable.height) {
            fieldPlaceable.placeRelative(0, 0)
            chipPlaceable.placeRelative(fieldPlaceable.width + spacingPx, labelStripPx)
        }
    }
}

/** Row of selectable category chips, shared by the expense and wishlist sheets. Reclicking the selected chip clears it. */
@Composable
fun CategoryPicker(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelect: (Long?) -> Unit,
    label: String = "Category",
    modifier: Modifier = Modifier,
) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(bottom = 10.dp),
    )
    if (categories.isEmpty()) {
        Text("Add a category first from the Categories tab.")
    } else {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories, key = { it.id }) { category ->
                FilterChip(
                    selected = category.id == selectedCategoryId,
                    onClick = {
                        onCategorySelect(if (selectedCategoryId == category.id) null else category.id)
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
}

/** "I need" / "I want" chip row, shared by the expense and wishlist sheets. */
@Composable
fun PriorityPicker(
    priority: WishlistPriority,
    onSelect: (WishlistPriority) -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        "Priority",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(bottom = 10.dp),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WishlistPriority.entries.forEach { option ->
            FilterChip(
                selected = priority == option,
                onClick = { onSelect(option) },
                label = { Text(option.label) },
            )
        }
    }
}
