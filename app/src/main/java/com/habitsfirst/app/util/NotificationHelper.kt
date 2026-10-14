package com.habitsfirst.app.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.habitsfirst.app.R
import com.habitsfirst.app.receiver.DailyReminderReceiver
import com.habitsfirst.app.ui.MainActivity
import java.util.Calendar

object NotificationHelper {

    const val CHANNEL_ID = "habits_reminder_channel"
    const val CHANNEL_NAME = "Habit & Streak Reminders"

    const val NOTIFICATION_ID_MORNING = 1001
    const val NOTIFICATION_ID_EVENING = 1002

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily morning briefing and evening streak protection reminders"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun scheduleDailyReminders(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            // Schedule Morning Alarm (8:00 AM)
            val morningIntent = Intent(context, DailyReminderReceiver::class.java).apply {
                action = DailyReminderReceiver.ACTION_MORNING_REMINDER
            }
            val morningPending = PendingIntent.getBroadcast(
                context,
                1,
                morningIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val morningCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 8)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                morningCal.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                morningPending
            )

            // Schedule Evening Alarm (8:00 PM)
            val eveningIntent = Intent(context, DailyReminderReceiver::class.java).apply {
                action = DailyReminderReceiver.ACTION_EVENING_REMINDER
            }
            val eveningPending = PendingIntent.getBroadcast(
                context,
                2,
                eveningIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val eveningCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 20)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                eveningCal.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                eveningPending
            )
        } catch (_: Throwable) {}
    }

    fun cancelDailyReminders(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            val morningIntent = Intent(context, DailyReminderReceiver::class.java).apply {
                action = DailyReminderReceiver.ACTION_MORNING_REMINDER
            }
            val morningPending = PendingIntent.getBroadcast(
                context,
                1,
                morningIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(morningPending)

            val eveningIntent = Intent(context, DailyReminderReceiver::class.java).apply {
                action = DailyReminderReceiver.ACTION_EVENING_REMINDER
            }
            val eveningPending = PendingIntent.getBroadcast(
                context,
                2,
                eveningIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(eveningPending)
        } catch (_: Throwable) {}
    }

    fun showMorningReminder(context: Context) {
        createNotificationChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_check)
            .setContentTitle("☀️ Good Morning! Habits First")
            .setContentText("Complete your daily habits today to unlock Instagram, TikTok & games.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID_MORNING, notification)
    }

    fun showStreakReminder(context: Context, remainingCount: Int, streak: Int) {
        createNotificationChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (streak > 0) "🔥 Don't break your $streak-day streak!" else "🌙 Complete your habits before midnight"
        val message = "$remainingCount daily habit(s) left. Complete them to finish the day strong!"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID_EVENING, notification)
    }
}
