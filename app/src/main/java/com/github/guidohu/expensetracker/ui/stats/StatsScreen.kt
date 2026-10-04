package com.github.guidohu.expensetracker.ui.stats

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.guidohu.expensetracker.data.AppContainer
import com.github.guidohu.expensetracker.data.toComposeColor
import com.github.guidohu.expensetracker.ui.SimpleViewModelFactory
import com.github.guidohu.expensetracker.ui.components.BarChart
import com.github.guidohu.expensetracker.ui.components.BarEntry
import com.github.guidohu.expensetracker.ui.components.DonutChart
import com.github.guidohu.expensetracker.ui.components.DonutSlice
import com.github.guidohu.expensetracker.ui.components.EmptyState
import com.github.guidohu.expensetracker.util.formatCurrency
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

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

            item {
                Column {
                    Text(
                        "Spending by category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        currentMonthLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                if (uiState.categoryTotalsThisMonth.isEmpty()) {
                    Text(
                        "No expenses this month yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        DonutChart(
                            slices = uiState.categoryTotalsThisMonth.map {
                                DonutSlice(it.fraction, it.color.toComposeColor())
                            },
                            centerLabel = "this month",
                            centerValue = formatCurrency(uiState.totalThisMonth, uiState.defaultCurrency),
                            modifier = Modifier.size(200.dp).padding(bottom = 20.dp),
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        ) {
                            uiState.categoryTotalsThisMonth.forEachIndexed { index, category ->
                                if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(category.color.toComposeColor())
                                    )
                                    Text(
                                        category.name,
                                        modifier = Modifier.padding(start = 12.dp).weight(1f),
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                    Text(
                                        "${(category.fraction * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(end = 10.dp),
                                    )
                                    Text(
                                        formatCurrency(category.total, uiState.defaultCurrency),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text("Last 6 months", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            item {
                val currentMonth = YearMonth.now()
                Card {
                    BarChart(
                        entries = uiState.monthlyTotals.map {
                            BarEntry(it.label, it.total, highlighted = it.yearMonth == currentMonth)
                        },
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                    )
                }
            }
        }
    }
}

private fun currentMonthLabel(): String =
    YearMonth.now().month.getDisplayName(TextStyle.FULL, Locale.getDefault())

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
            CardDefaults.cardColors()
        },
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

    Card(modifier = modifier.fillMaxWidth()) {
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
