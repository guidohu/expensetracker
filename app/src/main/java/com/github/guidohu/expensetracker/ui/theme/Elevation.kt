package com.github.guidohu.expensetracker.ui.theme

import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/** Material3's default Card has zero shadow elevation and a container color nearly identical to this
 * app's background token, so cards read as flat. A shared elevation step plus an explicitly more
 * contrasting container color give every card the same, deliberate sense of depth. */
object AppElevation {
    val card = 2.dp
    val emphasizedCard = 4.dp
}

/** Standard card container color/elevation combo — noticeably distinct from the page background,
 * unlike Material3's own default `CardDefaults.cardColors()` + zero elevation. */
object AppCard {
    val elevation @Composable get() = CardDefaults.cardElevation(defaultElevation = AppElevation.card)
    val emphasizedElevation @Composable get() = CardDefaults.cardElevation(defaultElevation = AppElevation.emphasizedCard)
    val colors: CardColors
        @Composable get() = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
}
