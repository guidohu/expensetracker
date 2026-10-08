package com.github.guidohu.expensetracker.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.guidohu.expensetracker.data.AppContainer
import com.github.guidohu.expensetracker.data.toComposeColor
import com.github.guidohu.expensetracker.ui.SimpleViewModelFactory
import com.github.guidohu.expensetracker.ui.components.BarChart
import com.github.guidohu.expensetracker.ui.components.BarEntry
import com.github.guidohu.expensetracker.ui.components.BarSegment
import com.github.guidohu.expensetracker.ui.components.DonutChart
import com.github.guidohu.expensetracker.ui.components.DonutSlice
import com.github.guidohu.expensetracker.ui.components.EmptyState
import com.github.guidohu.expensetracker.ui.theme.AppCard
import com.github.guidohu.expensetracker.util.formatCurrency
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(container: AppContainer) {
    val viewModel: StatsViewModel = viewModel(
        factory = SimpleViewModelFactory { StatsViewModel(container.repository, container.userPreferences) }
    )
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Stats") }) }) { padding ->
        if (uiState.isEmpty) {
            EmptyState(
                icon = Icons.Filled.BarChart,
                title = "Nothing to show yet",
                body = "Log a few expenses and your spending breakdown will show up here.",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SummaryCard(
                        title = "This month",
                        value = formatCurrency(uiState.totalThisMonth, uiState.defaultCurrency),
                        emphasized = true,
                        modifier = Modifier.weight(1f),
                    )
                    SummaryCard(
                        title = "All time",
                        value = formatCurrency(uiState.totalAllTime, uiState.defaultCurrency),
                        emphasized = false,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            uiState.monthlyBudget?.let { budget ->
                item {
                    BudgetCard(spent = uiState.totalThisMonth, budget = budget, currencyCode = uiState.defaultCurrency)
                }
            }

            uiState.historic?.let { historic ->
                item {
                    HistoricStatsSection(
                        historic = historic,
                        currencyCode = uiState.defaultCurrency,
                        onSelectPeriod = viewModel::selectPeriod,
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoricStatsSection(
    historic: HistoricStats,
    currencyCode: String,
    onSelectPeriod: (StatsPeriod) -> Unit,
) {
    val pagerState = rememberPagerState { historic.dimensions.size }
    val scope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        HorizontalDivider()

        // Bleed the chips to the screen edges so they scroll under the page margin instead of clipping at it.
        LazyRow(
            modifier = Modifier.layout { measurable, constraints ->
                val bleed = 16.dp.roundToPx()
                val placeable = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth + 2 * bleed))
                layout(constraints.maxWidth, placeable.height) { placeable.place(-bleed, 0) }
            },
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(StatsPeriod.entries) { period ->
                FilterChip(
                    selected = period == historic.period,
                    onClick = { onSelectPeriod(period) },
                    label = { Text(period.label) },
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            MetricTile("Total", formatCurrency(historic.total, currencyCode), Modifier.weight(1f))
            MetricTile(
                "Avg / ${historic.averageUnit}",
                formatCurrency(historic.average, currencyCode),
                Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Spending by ${historic.dimensions[pagerState.currentPage].dimension.title}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            PageDots(
                count = historic.dimensions.size,
                current = pagerState.currentPage,
                onSelect = { page -> scope.launch { pagerState.animateScrollToPage(page) } },
            )
        }

        HorizontalPager(
            state = pagerState,
            pageSpacing = 16.dp,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            DimensionPage(historic, historic.dimensions[page], currencyCode)
        }
    }
}

@Composable
private fun DimensionPage(historic: HistoricStats, stats: DimensionStats, currencyCode: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        DonutChart(
            slices = stats.groups.map { DonutSlice(it.fraction, it.color.toComposeColor()) },
            centerLabel = historic.period.label.lowercase(),
            centerValue = formatCurrency(historic.total, currencyCode),
            modifier = Modifier.size(200.dp),
        )

        Card(modifier = Modifier.fillMaxWidth(), colors = AppCard.colors, elevation = AppCard.elevation) {
            BarChart(
                entries = stats.bars.map { bucket ->
                    BarEntry(
                        label = bucket.label,
                        value = bucket.total,
                        highlighted = bucket.highlighted,
                        segments = bucket.segments.map { BarSegment(it.value, it.color.toComposeColor()) },
                    )
                },
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            )
        }

        if (stats.groups.isEmpty()) {
            Text(
                "No expenses in this period.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Card(modifier = Modifier.fillMaxWidth(), colors = AppCard.colors, elevation = AppCard.elevation) {
                stats.groups.forEachIndexed { index, group ->
                    if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    GroupRow(group, currencyCode)
                }
            }
        }
    }
}

@Composable
private fun GroupRow(group: GroupTotal, currencyCode: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(group.color.toComposeColor().copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            if (group.emoji != null) {
                Text(group.emoji, style = MaterialTheme.typography.bodyLarge)
            } else {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(group.color.toComposeColor())
                )
            }
        }
        Text(
            group.name,
            modifier = Modifier.padding(start = 12.dp).weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            "${(group.fraction * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 10.dp),
        )
        Text(
            formatCurrency(group.total, currencyCode),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun MetricTile(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = AppCard.colors, elevation = AppCard.elevation) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PageDots(count: Int, current: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(count) { page ->
            // The touch target is larger than the dot it draws.
            Box(
                modifier = Modifier.size(24.dp).clip(CircleShape).clickable { onSelect(page) },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(if (page == current) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (page == current) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String, emphasized: Boolean, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = if (emphasized) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        } else {
            AppCard.colors
        },
        elevation = if (emphasized) AppCard.emphasizedElevation else AppCard.elevation,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BudgetCard(spent: Double, budget: Double, currencyCode: String, modifier: Modifier = Modifier) {
    val fraction = if (budget > 0) (spent / budget).toFloat().coerceIn(0f, 1f) else 0f
    val overBudget = spent > budget
    val progressColor = when {
        overBudget -> MaterialTheme.colorScheme.error
        fraction > 0.8f -> Color(0xFFB8860B)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        colors = AppCard.colors,
        elevation = AppCard.elevation,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Monthly budget", style = MaterialTheme.typography.labelLarge)
                Text(
                    if (overBudget) {
                        "${formatCurrency(spent - budget, currencyCode)} over"
                    } else {
                        "${formatCurrency(budget - spent, currencyCode)} left"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = progressColor,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Text(
                "${formatCurrency(spent, currencyCode)} of ${formatCurrency(budget, currencyCode)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
