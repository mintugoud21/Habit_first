package com.habitsfirst.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.habitsfirst.app.data.repository.HabitRepository
import com.habitsfirst.app.util.NotificationHelper

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val repo = HabitRepository.getInstance(context)
            if (repo.areRemindersEnabled()) {
                NotificationHelper.scheduleDailyReminders(context)
            }
        }
    }
}
