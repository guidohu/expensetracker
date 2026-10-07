package com.github.guidohu.expensetracker

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.guidohu.expensetracker.data.AppContainer
import com.github.guidohu.expensetracker.notifications.NotificationHelper
import com.github.guidohu.expensetracker.ui.categories.CategoriesScreen
import com.github.guidohu.expensetracker.ui.expenses.ExpensesScreen
import com.github.guidohu.expensetracker.ui.onboarding.OnboardingScreen
import com.github.guidohu.expensetracker.ui.settings.SettingsScreen
import com.github.guidohu.expensetracker.ui.stats.StatsScreen
import com.github.guidohu.expensetracker.ui.theme.ExpenseTrackerTheme
import com.github.guidohu.expensetracker.ui.wishlist.WishlistScreen

sealed class Destination(val route: String, val label: String) {
    data object Expenses : Destination("expenses", "Expenses")
    data object Wishlist : Destination("wishlist", "Wishlist")
    data object Categories : Destination("categories", "Categories")
    data object Stats : Destination("stats", "Stats")
    data object Settings : Destination("settings", "Settings")
}

private val bottomNavItems =
    listOf(Destination.Expenses, Destination.Wishlist, Destination.Categories, Destination.Stats, Destination.Settings)

class MainActivity : ComponentActivity() {
    private val sharedWishlistUrl: MutableState<String?> = mutableStateOf(null)
    private val openAddExpense: MutableState<Boolean> = mutableStateOf(false)

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        val container = (application as ExpenseTrackerApplication).container
        setContent {
            ExpenseTrackerTheme {
                ExpenseTrackerApp(container, sharedWishlistUrl, openAddExpense)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("text/plain") == true) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            val url = sharedText?.let { URL_REGEX.find(it)?.value }
            if (url != null) sharedWishlistUrl.value = url
        }
        if (intent.getBooleanExtra(NotificationHelper.EXTRA_OPEN_ADD_EXPENSE, false)) {
            NotificationManagerCompat.from(this).cancel(NotificationHelper.NOTIFICATION_ID_DAILY_REMINDER)
            openAddExpense.value = true
        }
    }

    companion object {
        private val URL_REGEX = Regex("""https?://\S+""")
    }
}

@Composable
fun ExpenseTrackerApp(
    container: AppContainer,
    sharedWishlistUrl: MutableState<String?> = remember { mutableStateOf(null) },
    openAddExpense: MutableState<Boolean> = remember { mutableStateOf(false) },
) {
    var onboarded by remember { mutableStateOf(container.userPreferences.hasCompletedOnboarding) }

    if (!onboarded) {
        OnboardingScreen(onComplete = { currencyCode ->
            container.userPreferences.completeOnboarding(currencyCode)
            onboarded = true
        })
        return
    }

    val navController = rememberNavController()

    // Latched locally, independent of sharedWishlistUrl, so WishlistScreen consuming its prefill
    // (clearing pendingWishlistUrl) can never race with — or get raced by — this effect, which
    // clears sharedWishlistUrl itself the moment it reads it.
    var pendingWishlistUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(sharedWishlistUrl.value) {
        val url = sharedWishlistUrl.value
        if (url != null) {
            pendingWishlistUrl = url
            sharedWishlistUrl.value = null
            navController.navigate(Destination.Wishlist.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    // Same latch-then-clear-the-source pattern as pendingWishlistUrl above, for the same reason.
    var pendingOpenAddExpense by remember { mutableStateOf(false) }

    LaunchedEffect(openAddExpense.value) {
        if (openAddExpense.value) {
            pendingOpenAddExpense = true
            openAddExpense.value = false
            navController.navigate(Destination.Expenses.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

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
                                    Destination.Wishlist -> Icons.Filled.CardGiftcard
                                    Destination.Categories -> Icons.AutoMirrored.Filled.List
                                    Destination.Stats -> Icons.Filled.PieChart
                                    Destination.Settings -> Icons.Filled.Settings
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
        // Only the bottom (nav bar) inset is applied here — each screen has its own
        // Scaffold/TopAppBar that already accounts for the status bar inset, so
        // applying the top inset here too would double-pad the top of every screen.
        NavHost(
            navController = navController,
            startDestination = Destination.Expenses.route,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            composable(Destination.Expenses.route) {
                ExpensesScreen(
                    container,
                    autoOpenAddSheet = pendingOpenAddExpense,
                    onAutoOpenConsumed = { pendingOpenAddExpense = false },
                )
            }
            composable(Destination.Wishlist.route) {
                WishlistScreen(
                    container,
                    prefillUrl = pendingWishlistUrl,
                    onPrefillConsumed = { pendingWishlistUrl = null },
                )
            }
            composable(Destination.Categories.route) { CategoriesScreen(container) }
            composable(Destination.Stats.route) { StatsScreen(container) }
            composable(Destination.Settings.route) { SettingsScreen(container) }
        }
    }
}
