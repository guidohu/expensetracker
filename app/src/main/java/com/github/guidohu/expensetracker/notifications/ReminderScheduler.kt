package com.github.guidohu.expensetracker.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.github.guidohu.expensetracker.data.UserPreferences
import java.time.ZonedDateTime

/**
 * Arms/disarms the one-shot alarms behind the daily expense reminder and the monthly budget
 * check. Both receivers reschedule their own next occurrence when they fire, so there's no
 * repeating alarm to drift out of sync — [scheduleDailyReminder]/[scheduleBudgetCheck] always
 * compute "the next time this should fire from now", whether called from Settings or from the
 * receiver itself right after firing.
 *
 * Deliberately inexact ([AlarmManager.setAndAllowWhileIdle]) rather than exact: a reminder that
 * can land a few minutes late doesn't need the user to grant the separate "Alarms & reminders"
 * permission that exact alarms require on Android 12+.
 */
object ReminderScheduler {

    fun rescheduleAllFromPrefs(context: Context, prefs: UserPreferences) {
        if (prefs.dailyReminderEnabled.value) {
            scheduleDailyReminder(context, prefs.dailyReminderHour.value, prefs.dailyReminderMinute.value)
        } else {
            cancelDailyReminder(context)
        }
        if (prefs.budgetCongratsEnabled.value) {
            scheduleBudgetCheck(context)
            scheduleStreakCheck(context)
        } else {
            cancelBudgetCheck(context)
            cancelStreakCheck(context)
        }
    }

    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        val now = ZonedDateTime.now()
        var trigger = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!trigger.isAfter(now)) trigger = trigger.plusDays(1)
        schedule(context, trigger.toInstant().toEpochMilli(), dailyReminderPendingIntent(context))
    }

    fun cancelDailyReminder(context: Context) {
        alarmManager(context).cancel(dailyReminderPendingIntent(context))
    }

    fun scheduleBudgetCheck(context: Context) {
        val now = ZonedDateTime.now()
        var trigger = now.withDayOfMonth(1).withHour(9).withMinute(0).withSecond(0).withNano(0)
        if (!trigger.isAfter(now)) trigger = trigger.plusMonths(1)
        schedule(context, trigger.toInstant().toEpochMilli(), budgetCheckPendingIntent(context))
    }

    fun cancelBudgetCheck(context: Context) {
        alarmManager(context).cancel(budgetCheckPendingIntent(context))
    }

    fun scheduleStreakCheck(context: Context) {
        val now = ZonedDateTime.now()
        var trigger = now.withHour(9).withMinute(5).withSecond(0).withNano(0)
        if (!trigger.isAfter(now)) trigger = trigger.plusDays(1)
        schedule(context, trigger.toInstant().toEpochMilli(), streakCheckPendingIntent(context))
    }

    fun cancelStreakCheck(context: Context) {
        alarmManager(context).cancel(streakCheckPendingIntent(context))
    }

    private fun schedule(context: Context, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        alarmManager(context).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }

    private fun alarmManager(context: Context): AlarmManager =
        context.getSystemService(AlarmManager::class.java)

    private fun dailyReminderPendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        NotificationHelper.REQUEST_CODE_DAILY_REMINDER,
        Intent(context, DailyReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun budgetCheckPendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        NotificationHelper.REQUEST_CODE_BUDGET_CHECK,
        Intent(context, BudgetCheckReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun streakCheckPendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        NotificationHelper.REQUEST_CODE_STREAK_CHECK,
        Intent(context, NoSpendStreakReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
