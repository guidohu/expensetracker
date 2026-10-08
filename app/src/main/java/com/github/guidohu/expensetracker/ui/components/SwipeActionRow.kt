package com.github.guidohu.expensetracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val DeleteActionWidth = 88.dp
private val SelectBumpDistance = 36.dp

/**
 * Wraps a list row with the shared swipe/select/delete gesture used by Expenses and Wishlist:
 * - Tap opens edit, unless [selectionModeActive] is true, in which case it toggles selection.
 * - Long-press, tapping the selection dot, or swiping right all toggle selection.
 * - Swipe left reveals a red "Delete" button to the right of the content; tapping it deletes
 *   this one entry. Only one row may be revealed at a time: [isRevealed]/[onRevealedChange] let
 *   the caller enforce that by closing whichever other row was previously open. Disabled while
 *   [selectionModeActive].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeActionRow(
    isSelected: Boolean,
    selectionModeActive: Boolean,
    isRevealed: Boolean,
    onRevealedChange: (Boolean) -> Unit,
    onTap: () -> Unit,
    onToggleSelect: () -> Unit,
    onSwipeLeftDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val deleteWidthPx = with(density) { DeleteActionWidth.toPx() }
    val bumpPx = with(density) { SelectBumpDistance.toPx() }

    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    var toggledThisGesture by remember { mutableStateOf(false) }

    LaunchedEffect(selectionModeActive) {
        if (selectionModeActive && offsetX.value != 0f) {
            offsetX.animateTo(0f, animationSpec = spring())
        }
    }

    // Closes this row whenever the caller says some other row became the revealed one.
    LaunchedEffect(isRevealed) {
        if (!isRevealed && offsetX.value != 0f) {
            offsetX.animateTo(0f, animationSpec = spring())
        }
    }

    val draggableState = rememberDraggableState { delta ->
        if (selectionModeActive) return@rememberDraggableState
        val proposed = offsetX.value + delta
        val clamped = if (proposed >= 0f) {
            proposed.coerceIn(0f, bumpPx * 1.5f)
        } else {
            proposed.coerceIn(-deleteWidthPx, 0f)
        }
        coroutineScope.launch { offsetX.snapTo(clamped) }
        if (!toggledThisGesture && clamped >= bumpPx) {
            toggledThisGesture = true
            onToggleSelect()
        }
    }

    // The left-swipe reveal is expressed as real layout width — exactly like the selection dot's
    // AnimatedVisibility — so the content never translates past the edge of the screen and stays
    // fully visible (just narrower), instead of being cropped by sliding off-screen.
    val revealedWidthPx = (-offsetX.value).coerceIn(0f, deleteWidthPx)
    val revealedWidth = with(density) { revealedWidthPx.toDp() }
    // The right-swipe "select" bump is still a momentary translation: it always springs back to 0.
    val bumpOffsetPx = offsetX.value.coerceAtLeast(0f)

    // The row owns its outer margin (rather than the wrapped card carrying it), so the card and the
    // Delete button inside are exactly the same height instead of the button spanning the margin too.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(visible = selectionModeActive) {
            Box(
                modifier = Modifier
                    .padding(start = 4.dp, end = 8.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .let {
                        if (isSelected) it.background(MaterialTheme.colorScheme.primary)
                        else it.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    }
                    .clickable(onClick = onToggleSelect),
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .offset { IntOffset(bumpOffsetPx.roundToInt(), 0) }
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    enabled = !selectionModeActive,
                    onDragStarted = { toggledThisGesture = false },
                    onDragStopped = {
                        coroutineScope.launch {
                            when {
                                offsetX.value < 0f -> {
                                    val target = if (offsetX.value < -deleteWidthPx / 2) -deleteWidthPx else 0f
                                    offsetX.animateTo(target, animationSpec = spring())
                                    onRevealedChange(target != 0f)
                                }
                                offsetX.value > 0f -> offsetX.animateTo(0f, animationSpec = spring())
                            }
                        }
                    },
                )
                .combinedClickable(
                    onLongClick = onToggleSelect,
                    onClick = {
                        when {
                            offsetX.value != 0f -> {
                                coroutineScope.launch { offsetX.animateTo(0f) }
                                onRevealedChange(false)
                            }
                            selectionModeActive -> onToggleSelect()
                            else -> onTap()
                        }
                    },
                ),
        ) {
            content()
        }

        if (revealedWidthPx > 0f) {
            // The clip window grows with the drag, but the button inside is always laid out at
            // its full fixed width (never wraps, never changes height) — only how much of it is
            // visible changes, so the row's own height never jumps mid-reveal.
            Box(
                modifier = Modifier
                    .width(revealedWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .requiredWidth(DeleteActionWidth)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .clickable {
                            coroutineScope.launch { offsetX.animateTo(0f) }
                            onRevealedChange(false)
                            onSwipeLeftDelete()
                        },
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Text(
                        "Delete",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        }
    }
}
