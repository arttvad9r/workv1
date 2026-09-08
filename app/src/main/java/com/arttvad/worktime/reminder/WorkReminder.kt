package com.arttvad.worktime.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.arttvad.worktime.MainActivity
import com.arttvad.worktime.R
import com.arttvad.worktime.WorkTimeApplication
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object WorkReminderScheduler {
    const val ACTION_REMIND_TODAY = "com.arttvad.worktime.action.REMIND_TODAY"
    private const val RequestCode = 2401

    fun sync(context: Context, enabled: Boolean, hour: Int, minute: Int) {
        if (enabled) scheduleNext(context, hour, minute) else cancel(context)
    }

    fun scheduleNext(context: Context, hour: Int, minute: Int) {
        val triggerAtMillis = ReminderTimeCalculator
            .nextTrigger(ZonedDateTime.now(), hour, minute)
            .toInstant()
            .toEpochMilli()
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            reminderPendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT),
        )
    }

    fun cancel(context: Context) {
        existingPendingIntent(context)?.let { existing ->
            context.getSystemService(AlarmManager::class.java).cancel(existing)
            existing.cancel()
        }
    }

    fun isScheduled(context: Context): Boolean = existingPendingIntent(context) != null

    private fun existingPendingIntent(context: Context): PendingIntent? = PendingIntent.getBroadcast(
        context,
        RequestCode,
        reminderIntent(context),
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun reminderPendingIntent(context: Context, modeFlag: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        RequestCode,
        reminderIntent(context),
        modeFlag or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun reminderIntent(context: Context): Intent = Intent(context, WorkReminderReceiver::class.java).apply {
        action = ACTION_REMIND_TODAY
    }
}

class WorkReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val isReminder = action == WorkReminderScheduler.ACTION_REMIND_TODAY
        val isReschedule = action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        if (!isReminder && !isReschedule) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val application = context.applicationContext as WorkTimeApplication
                val preferences = application.container.preferencesRepository.preferences.first()
                WorkReminderScheduler.sync(
                    context = context,
                    enabled = preferences.reminderEnabled,
                    hour = preferences.reminderHour,
                    minute = preferences.reminderMinute,
                )
                if (isReminder && preferences.reminderEnabled) {
                    val today = LocalDate.now()
                    val hasEntry = application.container.workDayRepository
                        .observeMonth(YearMonth.from(today))
                        .first()
                        .any { day -> day.date == today }
                    if (!hasEntry) WorkReminderNotifier.show(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

private object WorkReminderNotifier {
    private const val ChannelId = "work_reminders"
    private const val NotificationId = 2401

    fun show(context: Context) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                ChannelId,
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_ADD_TODAY
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NotificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, ChannelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.reminder_notification_text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        manager.notify(NotificationId, notification)
    }
}
