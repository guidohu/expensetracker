package com.github.guidohu.expensetracker.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.github.guidohu.expensetracker.data.Category
import com.github.guidohu.expensetracker.data.CategoryColorPalette
import com.github.guidohu.expensetracker.data.toComposeColor
import com.github.guidohu.expensetracker.ui.components.SpectrumColorPickerDialog

/** Add/edit dialog for a category — [existing] null means "add new", non-null means "edit". */
@Composable
fun CategoryDialog(
    existing: Category?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, color: Int) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var selectedColor by rememberSaveable { mutableStateOf(existing?.color ?: CategoryColorPalette.first()) }
    var showSpectrumPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New category" else "Edit category") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Color",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                )
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(40.dp),
                    modifier = Modifier.fillMaxWidth().height(96.dp),
                ) {
                    items(CategoryColorPalette) { colorInt ->
                        val isSelected = colorInt == selectedColor
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colorInt.toComposeColor())
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape,
                                )
                                .clickable { selectedColor = colorInt }
                        )
                    }
                    item {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .padding(4.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .border(width = 1.dp, color = MaterialTheme.colorScheme.outline, shape = CircleShape)
                                .clickable { showSpectrumPicker = true }
                        ) {
                            Icon(Icons.Filled.Colorize, contentDescription = "Custom color", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name, selectedColor) },
            ) { Text(if (existing == null) "Add" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )

    if (showSpectrumPicker) {
        SpectrumColorPickerDialog(
            initialColor = selectedColor,
            onDismiss = { showSpectrumPicker = false },
            onColorSelected = { selectedColor = it; showSpectrumPicker = false },
        )
    }
}
