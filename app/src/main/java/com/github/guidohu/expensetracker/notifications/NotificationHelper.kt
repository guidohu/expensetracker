package com.github.guidohu.expensetracker.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

/** Channel ids, notification ids, and intent-extra keys shared across the notification pieces. */
object NotificationHelper {
    const val CHANNEL_DAILY_REMINDER = "daily_reminder"
    const val CHANNEL_BUDGET_CONGRATS = "budget_congrats"

    const val NOTIFICATION_ID_DAILY_REMINDER = 1001
    const val NOTIFICATION_ID_BUDGET_CONGRATS = 1002
    const val NOTIFICATION_ID_STREAK_CONGRATS = 1003

    const val REQUEST_CODE_DAILY_REMINDER = 2001
    const val REQUEST_CODE_BUDGET_CHECK = 2002
    const val REQUEST_CODE_STREAK_CHECK = 2003

    /** Set on the intent that opens [com.github.guidohu.expensetracker.MainActivity] from the "Add expenses" action. */
    const val EXTRA_OPEN_ADD_EXPENSE = "com.github.guidohu.expensetracker.OPEN_ADD_EXPENSE"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DAILY_REMINDER,
                "Daily reminder",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Reminds you to log today's expenses." }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_BUDGET_CONGRATS,
                "Budget achievements",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Congratulates you when you stay under your monthly budget, or go a streak without spending." }
        )
    }
}
