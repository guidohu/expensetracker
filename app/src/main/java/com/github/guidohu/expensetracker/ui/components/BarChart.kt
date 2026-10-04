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

data class BarEntry(val label: String, val value: Double, val highlighted: Boolean = false)

private val barShape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)

/** A minimal vertical bar chart: each bar's height is its value as a fraction of the largest entry. */
@Composable
fun BarChart(
    entries: List<BarEntry>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primaryContainer,
    highlightColor: Color = MaterialTheme.colorScheme.primary,
) {
    val maxValue = entries.maxOfOrNull { it.value } ?: 0.0
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

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
                Text(
                    text = if (entry.value > 0) entry.value.toInt().toString() else "",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (entry.highlighted) FontWeight.Bold else FontWeight.Normal,
                    color = if (entry.highlighted) highlightColor else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 4.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.5f)
                            .clip(barShape)
                            .background(trackColor)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(fraction)
                            .fillMaxWidth(0.5f)
                            .clip(barShape)
                            .background(if (entry.highlighted) highlightColor else barColor)
                    )
                }
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (entry.highlighted) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}
