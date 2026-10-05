package com.github.guidohu.expensetracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.guidohu.expensetracker.ExpenseTrackerApplication

/** Alarms are cleared on reboot and on app update — re-arm them from the persisted prefs. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val container = (context.applicationContext as ExpenseTrackerApplication).container
        ReminderScheduler.rescheduleAllFromPrefs(context, container.userPreferences)
    }
}
