package com.github.guidohu.expensetracker.ui.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.guidohu.expensetracker.data.filterCurrencies
import com.github.guidohu.expensetracker.ui.components.CurrencyOptionRow
import com.github.guidohu.expensetracker.ui.theme.AppCard
import java.util.Currency
import java.util.Locale

@Composable
fun OnboardingScreen(onComplete: (currencyCode: String) -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val deviceDefault = remember {
        runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }.getOrDefault("USD")
    }
    var selectedCurrency by remember { mutableStateOf(deviceDefault) }

    when (step) {
        0 -> WelcomeStep(onNext = { step = 1 })
        else -> CurrencyStep(
            selected = selectedCurrency,
            onSelect = { selectedCurrency = it },
            onFinish = { onComplete(selectedCurrency) },
        )
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 40.dp),
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(88.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        Icons.Filled.Savings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
            Text(
                "Welcome to Expense Tracker",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                "Track spending by category, see where your money goes — all stored on your device.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
            Button(onClick = onNext, modifier = Modifier.padding(top = 32.dp).fillMaxWidth()) {
                Text("Get started")
            }
        }
    }
}

@Composable
internal fun CurrencyStep(
    selected: String,
    onSelect: (String) -> Unit,
    onFinish: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) { filterCurrencies(query) }

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
        Text("What's your default currency?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "You can change this later in Settings. Expenses in other currencies get converted automatically.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Card(
            colors = AppCard.colors,
            elevation = AppCard.elevation,
            modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 16.dp),
        ) {
            LazyColumn {
                items(filtered, key = { it.code }) { currency ->
                    CurrencyOptionRow(
                        currency,
                        selected = currency.code == selected,
                        onClick = {
                            // Tapping a currency picks it immediately — no separate confirm step.
                            onSelect(currency.code)
                            onFinish()
                        },
                    )
                    if (currency != filtered.last()) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}
