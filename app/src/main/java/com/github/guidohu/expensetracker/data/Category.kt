package com.github.guidohu.expensetracker.data

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int,
)

/** Preset palette offered when creating a category, chosen for contrast in both themes. */
val CategoryColorPalette: List<Int> = listOf(
    0xFFEF5350.toInt(), // red
    0xFFFF7043.toInt(), // orange
    0xFFFFCA28.toInt(), // amber
    0xFF9CCC65.toInt(), // light green
    0xFF26A69A.toInt(), // teal
    0xFF42A5F5.toInt(), // blue
    0xFF5C6BC0.toInt(), // indigo
    0xFFAB47BC.toInt(), // purple
    0xFFEC407A.toInt(), // pink
    0xFF8D6E63.toInt(), // brown
    0xFF78909C.toInt(), // blue grey
    0xFF26C6DA.toInt(), // cyan
)

/** Seeded on a fresh install — exactly [FreeTier.CATEGORY_LIMIT] of them, so a free user starts at the limit rather than over it. */
val DefaultCategorySeed: List<Category> = listOf(
    Category(name = "Food", color = 0xFFFF7043.toInt()),
    Category(name = "Shopping", color = 0xFFAB47BC.toInt()),
    Category(name = "Other", color = 0xFF78909C.toInt()),
)

fun Int.toComposeColor(): Color = Color(this)
