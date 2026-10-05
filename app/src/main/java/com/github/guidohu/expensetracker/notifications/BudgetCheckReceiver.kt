package com.github.guidohu.expensetracker.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.guidohu.expensetracker.ExpenseTrackerApplication
import com.github.guidohu.expensetracker.MainActivity
import com.github.guidohu.expensetracker.R
import com.github.guidohu.expensetracker.data.AppContainer
import com.github.guidohu.expensetracker.util.formatCurrency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Fired on the 1st of each month by the alarm armed in [ReminderScheduler]. Re-arms itself for next month. */
class BudgetCheckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val container = (context.applicationContext as ExpenseTrackerApplication).container
        CoroutineScope(Dispatchers.IO).launch {
            try {
                checkLastMonth(context, container)
            } finally {
                pendingResult.finish()
            }
        }
        ReminderScheduler.scheduleBudgetCheck(context)
    }

    private suspend fun checkLastMonth(context: Context, container: AppContainer) {
        val prefs = container.userPreferences
        if (!prefs.budgetCongratsEnabled.value) return
        val budget = prefs.monthlyBudget.value ?: return

        val lastMonth = YearMonth.now().minusMonths(1)
        val monthKey = lastMonth.toString()
        if (prefs.lastBudgetCongratsMonth == monthKey) return

        val total = container.repository.totalInRange(lastMonth.atDay(1).toEpochDay(), lastMonth.atEndOfMonth().toEpochDay())
        if (total >= budget) return

        prefs.lastBudgetCongratsMonth = monthKey
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val monthName = lastMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        val currency = prefs.defaultCurrency.value
        val contentIntent = PendingIntent.getActivity(
            context,
            NotificationHelper.REQUEST_CODE_BUDGET_CHECK,
            Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_BUDGET_CONGRATS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("You stayed under budget in $monthName! 🎉")
            .setContentText("You spent ${formatCurrency(total, currency)} of your ${formatCurrency(budget, currency)} budget.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NotificationHelper.NOTIFICATION_ID_BUDGET_CONGRATS, notification)
    }
}
