package com.habitsfirst.app.ui

import android.content.Context
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Vibrator
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.habitsfirst.app.R
import com.habitsfirst.app.data.model.Habit
import com.habitsfirst.app.data.repository.HabitRepository
import com.habitsfirst.app.ui.views.CircularProgressView

class TimerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_HABIT_ID = "extra_habit_id"
    }

    private lateinit var repository: HabitRepository
    private var currentHabit: Habit? = null
    private var countDownTimer: CountDownTimer? = null
    private var isRunning = false

    private var totalSeconds = 25 * 60
    private var secondsRemaining = 25 * 60

    private lateinit var progressView: CircularProgressView
    private lateinit var tvTitle: TextView
    private lateinit var tvRemainingTime: TextView
    private lateinit var btnToggle: MaterialButton
    private lateinit var btnCancel: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)

        try {
            val rootLayout = findViewById<android.view.View>(R.id.timer_root_layout)
            if (rootLayout != null) {
                val origTop = rootLayout.paddingTop
                val origBottom = rootLayout.paddingBottom
                androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { _, insets ->
                    try {
                        val statusBars = insets.getInsets(
                            androidx.core.view.WindowInsetsCompat.Type.statusBars() or
                            androidx.core.view.WindowInsetsCompat.Type.displayCutout()
                        )
                        val navBars = insets.getInsets(
                            androidx.core.view.WindowInsetsCompat.Type.navigationBars()
                        )
                        rootLayout.setPadding(
                            rootLayout.paddingLeft,
                            origTop + statusBars.top,
                            rootLayout.paddingRight,
                            origBottom + navBars.bottom
                        )
                    } catch (_: Throwable) {}
                    insets
                }
            }
        } catch (_: Throwable) {}

        repository = HabitRepository.getInstance(this)
        val habitId = intent.getStringExtra(EXTRA_HABIT_ID)
        currentHabit = repository.getAllHabits().firstOrNull { it.id == habitId }

        progressView = findViewById(R.id.timer_progress_view)
        tvTitle = findViewById(R.id.tv_timer_habit_title)
        tvRemainingTime = findViewById(R.id.tv_timer_countdown)
        btnToggle = findViewById(R.id.btn_timer_toggle)
        btnCancel = findViewById(R.id.btn_timer_cancel)

        initHabitTimer()
        setupListeners()
    }

    private fun initHabitTimer() {
        val habit = currentHabit
        if (habit != null) {
            tvTitle.text = habit.title
            val minutes = if (habit.targetValue > 0) habit.targetValue else 25
            totalSeconds = minutes * 60
            secondsRemaining = totalSeconds
            val color = try {
                Color.parseColor(habit.colorHex)
            } catch (e: Exception) {
                Color.parseColor("#10B981")
            }
            progressView.setProgress(0f, color)
        } else {
            tvTitle.text = "Focus Session"
            totalSeconds = 25 * 60
            secondsRemaining = totalSeconds
        }
        updateTimeDisplay()
    }

    private fun setupListeners() {
        btnToggle.setOnClickListener {
            if (isRunning) {
                pauseTimer()
            } else {
                startTimer()
            }
        }

        btnCancel.setOnClickListener {
            cancelTimer()
            finish()
        }
    }

    private fun startTimer() {
        isRunning = true
        btnToggle.text = "Pause"

        countDownTimer = object : CountDownTimer(secondsRemaining * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                secondsRemaining = (millisUntilFinished / 1000).toInt()
                updateTimeDisplay()
                val elapsed = totalSeconds - secondsRemaining
                val fraction = elapsed.toFloat() / totalSeconds.toFloat()
                progressView.setProgress(fraction)
            }

            override fun onFinish() {
                isRunning = false
                secondsRemaining = 0
                updateTimeDisplay()
                progressView.setProgress(1.0f)
                onTimerComplete()
            }
        }.start()
    }

    private fun pauseTimer() {
        isRunning = false
        btnToggle.text = "Resume"
        countDownTimer?.cancel()
    }

    private fun cancelTimer() {
        isRunning = false
        countDownTimer?.cancel()
    }

    private fun updateTimeDisplay() {
        val minutes = secondsRemaining / 60
        val seconds = secondsRemaining % 60
        tvRemainingTime.text = String.format("%02d:%02d", minutes, seconds)
        progressView.setCenterLabel(String.format("%02d:%02d", minutes, seconds), "remaining")
    }

    private fun onTimerComplete() {
        val habit = currentHabit
        if (habit != null) {
            repository.updateHabitProgress(habit.id, habit.targetValue)
            repository.setHabitCompleted(habit.id, true)
        }

        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(applicationContext, notificationUri)
            ringtone.play()

            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(500L)
        } catch (e: Exception) {
            // ignore
        }

        Toast.makeText(this, "Habit completed! Apps have been unlocked!", Toast.LENGTH_LONG).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
