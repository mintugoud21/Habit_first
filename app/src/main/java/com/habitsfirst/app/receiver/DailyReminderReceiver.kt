package com.habitsfirst.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.habitsfirst.app.data.repository.HabitRepository
import com.habitsfirst.app.util.NotificationHelper

class DailyReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MORNING_REMINDER = "com.habitsfirst.app.ACTION_MORNING_REMINDER"
        const val ACTION_EVENING_REMINDER = "com.habitsfirst.app.ACTION_EVENING_REMINDER"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val repo = HabitRepository.getInstance(context)
        if (!repo.areRemindersEnabled()) return

        when (intent.action) {
            ACTION_MORNING_REMINDER -> {
                NotificationHelper.showMorningReminder(context)
            }
            ACTION_EVENING_REMINDER -> {
                val (completed, total) = repo.getHabitsCompletionStatus()
                if (completed < total && total > 0) {
                    val remaining = total - completed
                    val streak = repo.getCurrentStreak()
                    NotificationHelper.showStreakReminder(context, remaining, streak)
                }
            }
        }
    }
}
