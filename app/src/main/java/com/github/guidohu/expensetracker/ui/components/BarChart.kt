package com.github.guidohu.expensetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** One coloured slice of a stacked bar. */
data class BarSegment(val value: Double, val color: Color)

/**
 * One bar. With [segments] the bar is stacked (first segment at the bottom) and its height still comes
 * from [value]; without them it is a single solid bar.
 */
data class BarEntry(
    val label: String,
    val value: Double,
    val highlighted: Boolean = false,
    val segments: List<BarSegment> = emptyList(),
)

private val barShape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)

/** Above this many bars there is no room to print each bar's value on top of it. */
private const val MaxBarsWithValueLabels = 12

private fun compactValue(value: Double): String = when {
    value >= 10_000 -> "${(value / 1000).roundToInt()}k"
    value >= 1_000 -> "${(value / 100).roundToInt() / 10.0}k".replace(".0k", "k")
    else -> value.roundToInt().toString()
}

/**
 * A minimal vertical bar chart: each bar's height is its value as a fraction of the largest entry.
 * Entries may carry [BarEntry.segments] to stack colours; a blank [BarEntry.label] leaves that tick unnamed.
 */
@Composable
fun BarChart(
    entries: List<BarEntry>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primaryContainer,
    highlightColor: Color = MaterialTheme.colorScheme.primary,
) {
    val maxValue = entries.maxOfOrNull { it.value } ?: 0.0
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val showValues = entries.size <= MaxBarsWithValueLabels
    val barWidthFraction = if (showValues) 0.5f else 0.7f
    // In a stacked chart an empty bucket stays an empty track rather than getting a placeholder sliver.
    val stackedChart = entries.any { it.segments.isNotEmpty() }

    Row(
        modifier = modifier.fillMaxWidth().height(180.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        entries.forEach { entry ->
            val fraction = if (maxValue > 0) (entry.value / maxValue).toFloat().coerceIn(0.02f, 1f) else 0.02f

            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (showValues) {
                    Text(
                        text = if (entry.value > 0) compactValue(entry.value) else "",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (entry.highlighted) FontWeight.Bold else FontWeight.Normal,
                        color = if (entry.highlighted) highlightColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.wrapContentWidth(unbounded = true),
                    )
                }
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 4.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(barWidthFraction)
                            .clip(barShape)
                            .background(trackColor)
                    )
                    if (stackedChart) {
                        if (entry.value > 0) {
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight(fraction)
                                    .fillMaxWidth(barWidthFraction)
                                    .clip(barShape),
                            ) {
                                // Column lays out top-down, the first segment belongs at the bottom.
                                entry.segments.filter { it.value > 0 }.asReversed().forEach { segment ->
                                    Box(
                                        modifier = Modifier
                                            .weight(segment.value.toFloat())
                                            .fillMaxWidth()
                                            .background(segment.color)
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(fraction)
                                .fillMaxWidth(barWidthFraction)
                                .clip(barShape)
                                .background(if (entry.highlighted) highlightColor else barColor)
                        )
                    }
                }
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (entry.highlighted) FontWeight.Bold else FontWeight.Normal,
                    color = if (entry.highlighted) highlightColor else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(top = 6.dp).wrapContentWidth(unbounded = true),
                )
            }
        }
    }
}
