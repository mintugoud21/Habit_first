package com.habitsfirst.app.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.habitsfirst.app.data.repository.HabitRepository

class StepSensorHelper(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private var initialStepOffset = -1

    fun startListening() {
        stepSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_STEP_COUNTER) return

        val totalStepsSinceBoot = event.values[0].toInt()
        val prefs = context.getSharedPreferences("habits_first_step_prefs", Context.MODE_PRIVATE)
        val todayStr = HabitRepository.getInstance(context).getTodayDateStr()
        val savedDate = prefs.getString("steps_date", "")

        if (savedDate != todayStr) {
            prefs.edit()
                .putString("steps_date", todayStr)
                .putInt("steps_base_offset", totalStepsSinceBoot)
                .apply()
            initialStepOffset = totalStepsSinceBoot
        } else {
            initialStepOffset = prefs.getInt("steps_base_offset", totalStepsSinceBoot)
        }

        val todaySteps = (totalStepsSinceBoot - initialStepOffset).coerceAtLeast(0)
        val repository = HabitRepository.getInstance(context)
        val habit = repository.getAllHabits().firstOrNull { it.id == "habit_steps" }
        if (habit != null && habit.currentValue != todaySteps) {
            val isCompleted = todaySteps >= habit.targetValue
            repository.updateHabit(habit.copy(currentValue = todaySteps, isCompletedToday = isCompleted))
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}
