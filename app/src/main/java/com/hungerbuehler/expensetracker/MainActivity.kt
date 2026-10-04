package com.hungerbuehler.expensetracker

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hungerbuehler.expensetracker.ui.categories.CategoriesScreen
import com.hungerbuehler.expensetracker.ui.expenses.ExpensesScreen
import com.hungerbuehler.expensetracker.ui.stats.StatsScreen
import com.hungerbuehler.expensetracker.ui.theme.ExpenseTrackerTheme

sealed class Destination(val route: String, val label: String) {
    data object Expenses : Destination("expenses", "Expenses")
    data object Categories : Destination("categories", "Categories")
    data object Stats : Destination("stats", "Stats")
}

private val bottomNavItems = listOf(Destination.Expenses, Destination.Categories, Destination.Stats)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as ExpenseTrackerApplication).repository
        setContent {
            ExpenseTrackerTheme {
                ExpenseTrackerApp(repository)
            }
        }
    }
}

@Composable
fun ExpenseTrackerApp(repository: com.hungerbuehler.expensetracker.data.ExpenseRepository) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = when (item) {
                                    Destination.Expenses -> Icons.Filled.Receipt
                                    Destination.Categories -> Icons.AutoMirrored.Filled.List
                                    Destination.Stats -> Icons.Filled.PieChart
                                },
                                contentDescription = item.label,
                            )
                        },
                        label = { Text(item.label) },
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Expenses.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Destination.Expenses.route) {
                ExpensesScreen(
                    repository = repository,
                    onNavigateToCategories = {
                        navController.navigate(Destination.Categories.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Destination.Categories.route) { CategoriesScreen(repository) }
            composable(Destination.Stats.route) { StatsScreen(repository) }
        }
    }
}
