package com.github.guidohu.expensetracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.guidohu.expensetracker.data.Mood

/** Row of selectable mood chips. Tapping the already-selected one clears it — mood is always optional. */
@Composable
fun MoodPicker(
    selected: Mood?,
    onSelect: (Mood?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        items(Mood.entries) { mood ->
            FilterChip(
                selected = mood == selected,
                onClick = { onSelect(if (mood == selected) null else mood) },
                label = { Text("${mood.emoji} ${mood.label}", style = MaterialTheme.typography.labelLarge) },
            )
        }
    }
}
