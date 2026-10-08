package com.habitsfirst.app

import android.app.Application
import com.habitsfirst.app.data.repository.HabitRepository

class HabitsFirstApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize repository and check midnight reset
        HabitRepository.getInstance(this)
    }
}
