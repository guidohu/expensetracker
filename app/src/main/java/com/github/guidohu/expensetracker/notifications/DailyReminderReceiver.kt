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
import com.github.guidohu.expensetracker.MainActivity
import com.github.guidohu.expensetracker.R
import com.github.guidohu.expensetracker.data.UserPreferences

/** Fired by the daily alarm armed in [ReminderScheduler]. Shows the reminder, then re-arms itself for tomorrow. */
class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            val addExpenseIntent = Intent(context, MainActivity::class.java).apply {
                putExtra(NotificationHelper.EXTRA_OPEN_ADD_EXPENSE, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val addExpensePendingIntent = PendingIntent.getActivity(
                context,
                NotificationHelper.REQUEST_CODE_DAILY_REMINDER,
                addExpenseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val dismissPendingIntent = PendingIntent.getBroadcast(
                context,
                NotificationHelper.REQUEST_CODE_DAILY_REMINDER,
                Intent(context, NotificationActionReceiver::class.java)
                    .setAction(NotificationActionReceiver.ACTION_DISMISS_REMINDER),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_DAILY_REMINDER)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Log today's expenses?")
                .setContentText("Tap to add what you spent today.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(addExpensePendingIntent)
                .addAction(0, "Add expenses", addExpensePendingIntent)
                .addAction(0, "No expenses", dismissPendingIntent)
                .build()

            NotificationManagerCompat.from(context).notify(NotificationHelper.NOTIFICATION_ID_DAILY_REMINDER, notification)
        }

        val prefs = UserPreferences(context)
        ReminderScheduler.scheduleDailyReminder(context, prefs.dailyReminderHour.value, prefs.dailyReminderMinute.value)
    }
}
