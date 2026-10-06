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
import com.github.guidohu.expensetracker.data.NoSpendMilestones
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Fired daily by the alarm armed in [ReminderScheduler]. Congratulates the user when their
 * current no-spend streak crosses a new milestone in [NoSpendMilestones]. Re-arms itself for
 * tomorrow, same self-rescheduling pattern as [DailyReminderReceiver]/[BudgetCheckReceiver].
 */
class NoSpendStreakReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val container = (context.applicationContext as ExpenseTrackerApplication).container
        CoroutineScope(Dispatchers.IO).launch {
            try {
                checkStreak(context, container)
            } finally {
                pendingResult.finish()
            }
        }
        ReminderScheduler.scheduleStreakCheck(context)
    }

    private suspend fun checkStreak(context: Context, container: AppContainer) {
        val prefs = container.userPreferences
        if (!prefs.budgetCongratsEnabled.value) return

        val lastExpenseDate = container.repository.lastExpenseDate() ?: return
        val today = LocalDate.now().toEpochDay()
        val streakDays = (today - lastExpenseDate).toInt()
        if (streakDays <= 0) return

        if (prefs.lastStreakAnchorDate != lastExpenseDate) {
            prefs.lastStreakAnchorDate = lastExpenseDate
            prefs.lastStreakMilestoneNotified = 0
        }

        val milestone = NoSpendMilestones
            .filter { it.days <= streakDays && it.days > prefs.lastStreakMilestoneNotified }
            .maxByOrNull { it.days } ?: return

        prefs.lastStreakMilestoneNotified = milestone.days
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val contentIntent = PendingIntent.getActivity(
            context,
            NotificationHelper.REQUEST_CODE_STREAK_CHECK,
            Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_BUDGET_CONGRATS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("No-spend streak! 🎉")
            .setContentText("You haven't spent anything in ${milestone.label}. Keep it up!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NotificationHelper.NOTIFICATION_ID_STREAK_CONGRATS, notification)
    }
}
