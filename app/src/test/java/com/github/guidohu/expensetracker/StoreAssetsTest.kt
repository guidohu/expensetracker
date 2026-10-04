package com.github.guidohu.expensetracker

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.github.guidohu.expensetracker.ui.categories.CategoriesScreen
import com.github.guidohu.expensetracker.ui.expenses.AddExpenseSheetContent
import com.github.guidohu.expensetracker.ui.expenses.ExpensesScreen
import com.github.guidohu.expensetracker.ui.stats.StatsScreen
import com.github.guidohu.expensetracker.ui.theme.ExpenseTrackerTheme
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Renders Play Store listing assets (icon, phone screenshots) at real resolutions without
 * transparency, separate from [ScreenshotTest]'s small renders used for day-to-day UI verification.
 */
class StoreAssetsTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig(
            screenWidth = 1080,
            screenHeight = 2340,
            xdpi = 480,
            ydpi = 480,
            density = Density.XXHIGH,
        ),
        theme = "android:Theme.Material.Light.NoActionBar",
    )

    private fun screenshot(content: @androidx.compose.runtime.Composable () -> Unit) {
        val composeView = ComposeView(paparazzi.context).apply {
            setContent {
                val viewModelStoreOwner = remember { object : ViewModelStoreOwner {
                    override val viewModelStore = ViewModelStore()
                } }
                CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
                    ExpenseTrackerTheme(darkTheme = false, dynamicColor = false) {
                        androidx.compose.material3.Surface { content() }
                    }
                }
            }
        }
        paparazzi.snapshot(composeView)
    }

    @Test
    fun shot_expenses() = screenshot {
        ExpensesScreen(repositoryOf(sampleCategories, sampleExpenses))
    }

    @Test
    fun shot_stats() = screenshot {
        StatsScreen(repositoryOf(sampleCategories, sampleExpenses))
    }

    @Test
    fun shot_categories() = screenshot {
        CategoriesScreen(repositoryOf(sampleCategories, sampleExpenses))
    }

    @Test
    fun shot_addExpense() = screenshot {
        AddExpenseSheetContent(
            amountText = "24.50",
            onAmountChange = {},
            categories = sampleCategories,
            selectedCategoryId = food.id,
            onCategorySelect = {},
            note = "Lunch with Sam",
            onNoteChange = {},
            selectedDate = LocalDate.now().toEpochDay(),
            onDateClick = {},
            isValid = true,
            onSave = {},
        )
    }

    @Test
    fun icon_playStore() {
        val composeView = ComposeView(paparazzi.context).apply {
            setContent {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1B5E20))) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        paparazzi.unsafeUpdateConfig(
            deviceConfig = DeviceConfig(
                screenWidth = 512,
                screenHeight = 512,
                xdpi = 160,
                ydpi = 160,
                density = Density.MEDIUM,
            ),
        )
        paparazzi.snapshot(composeView)
    }
}
