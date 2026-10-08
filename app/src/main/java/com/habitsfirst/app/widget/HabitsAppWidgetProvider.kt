package com.habitsfirst.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.habitsfirst.app.R
import com.habitsfirst.app.data.repository.HabitRepository
import com.habitsfirst.app.ui.MainActivity

class HabitsAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
                val thisWidget = ComponentName(context, HabitsAppWidgetProvider::class.java)
                val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget) ?: return
                for (widgetId in allWidgetIds) {
                    updateAppWidget(context, appWidgetManager, widgetId)
                }
            } catch (t: Throwable) {
                // Ignore widget update errors in headless/unsupported environments
            }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val repo = HabitRepository.getInstance(context)
            val (completed, total) = repo.getHabitsCompletionStatus()
            val remainingHabits = repo.getRemainingHabits()
            val streak = repo.getCurrentStreak()
            val isUnlocked = total > 0 && completed >= total

            val views = RemoteViews(context.packageName, R.layout.widget_habits_status)

            // Streak Text
            views.setTextViewText(R.id.widget_streak_text, "🔥 $streak Days")

            // Status & Progress
            val progressPercent = if (total > 0) (completed * 100) / total else 0
            views.setProgressBar(R.id.widget_progress_bar, 100, progressPercent, false)

            if (isUnlocked) {
                views.setTextViewText(R.id.widget_status_text, "🎉 All Habits Complete! Apps Unlocked 🔓")
                views.setTextColor(R.id.widget_status_text, 0xFF10B981.toInt())
                views.setTextViewText(R.id.widget_habits_preview, "All daily habits completed. Enjoy your screen time mindfully!")
            } else {
                val remainingCount = total - completed
                views.setTextViewText(R.id.widget_status_text, "🔒 $remainingCount of $total habits left to unlock")
                views.setTextColor(R.id.widget_status_text, 0xFFF59E0B.toInt())

                val preview = if (remainingHabits.isNotEmpty()) {
                    remainingHabits.take(3).joinToString("\n") { "• ${it.title}" }
                } else {
                    "No habits active today."
                }
                views.setTextViewText(R.id.widget_habits_preview, preview)
            }

            // Click Intent to open MainActivity
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
