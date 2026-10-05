package com.github.guidohu.expensetracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

/** Handles notification action buttons that just dismiss, with no activity launch. */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_DISMISS_REMINDER ->
                NotificationManagerCompat.from(context).cancel(NotificationHelper.NOTIFICATION_ID_DAILY_REMINDER)
        }
    }

    companion object {
        const val ACTION_DISMISS_REMINDER = "com.github.guidohu.expensetracker.action.DISMISS_REMINDER"
    }
}
