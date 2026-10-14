package com.habitsfirst.app.service

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.habitsfirst.app.data.repository.HabitRepository

class StepSensorHelper(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val stepSensor = try {
        sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    } catch (e: Throwable) {
        null
    }
    private var initialStepOffset = -1

    fun startListening() {
        try {
            // Android 10+ requires runtime permission android.permission.ACTIVITY_RECOGNITION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.ACTIVITY_RECOGNITION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.d("HabitsFirst", "Step sensor skipped: ACTIVITY_RECOGNITION not granted yet")
                    return
                }
            }

            stepSensor?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        } catch (e: SecurityException) {
            Log.w("HabitsFirst", "SecurityException registering step sensor: ${e.message}")
        } catch (e: Throwable) {
            Log.w("HabitsFirst", "Could not register step sensor: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            sensorManager?.unregisterListener(this)
        } catch (e: Throwable) {
            // Safe fallback
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        try {
            if (event == null || event.sensor.type != Sensor.TYPE_STEP_COUNTER) return

            val totalStepsSinceBoot = event.values.getOrNull(0)?.toInt() ?: return
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
        } catch (e: Throwable) {
            Log.w("HabitsFirst", "Error processing step sensor event: ${e.message}")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}
