package com.habitsfirst.app

import android.app.Application
import android.util.Log
import com.habitsfirst.app.data.repository.HabitRepository

class HabitsFirstApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Install uncaught exception handler to log crash details and prevent silent failure
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e("HabitsFirst", "FATAL CRASH in thread ${thread.name}", throwable)
                val crashFile = java.io.File(filesDir, "crash_log.txt")
                crashFile.writeText(
                    "Timestamp: ${java.util.Date()}\n" +
                    "Thread: ${thread.name}\n" +
                    "Exception: ${throwable.javaClass.name}: ${throwable.message}\n" +
                    "Stacktrace:\n" + Log.getStackTraceString(throwable)
                )
            } catch (_: Throwable) {
                // Ignore secondary logging failures
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }

        try {
            // Initialize repository and check midnight reset
            HabitRepository.getInstance(this)
        } catch (e: Throwable) {
            Log.e("HabitsFirst", "Error initializing HabitRepository in Application.onCreate", e)
        }
    }
}
