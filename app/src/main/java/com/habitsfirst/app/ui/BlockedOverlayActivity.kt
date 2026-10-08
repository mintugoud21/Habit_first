package com.habitsfirst.app.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.habitsfirst.app.R
import com.habitsfirst.app.data.model.Habit
import com.habitsfirst.app.data.model.HabitType
import com.habitsfirst.app.data.repository.HabitRepository

class BlockedOverlayActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "extra_blocked_package"
        const val EXTRA_RULE_ID = "extra_rule_id"
    }

    private lateinit var repository: HabitRepository
    private var blockedPackage: String? = null
    private var ruleId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blocked_overlay)

        val rootLayout = findViewById<android.view.View>(R.id.blocked_root_layout)
        val origTop = rootLayout.paddingTop
        val origBottom = rootLayout.paddingBottom
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { _, insets ->
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
            insets
        }

        repository = HabitRepository.getInstance(this)
        blockedPackage = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE)
        ruleId = intent.getStringExtra(EXTRA_RULE_ID)

        setupUI()
    }

    private fun setupUI() {
        val ivAppIcon = findViewById<ImageView>(R.id.iv_blocked_app_icon)
        val tvAppName = findViewById<TextView>(R.id.tv_blocked_app_name)
        val tvStatusDesc = findViewById<TextView>(R.id.tv_block_status_desc)
        val habitsContainer = findViewById<LinearLayout>(R.id.ll_required_habits)
        val btnGoHome = findViewById<MaterialButton>(R.id.btn_go_home)
        val btnSnooze = findViewById<MaterialButton>(R.id.btn_emergency_snooze)

        // Resolve app name & icon
        val pm = packageManager
        if (!blockedPackage.isNullOrBlank()) {
            try {
                val appInfo = pm.getApplicationInfo(blockedPackage!!, 0)
                ivAppIcon.setImageDrawable(pm.getApplicationIcon(appInfo))
                val appLabel = pm.getApplicationLabel(appInfo).toString()
                tvAppName.text = "$appLabel is locked"
            } catch (e: PackageManager.NameNotFoundException) {
                tvAppName.text = "App is locked"
            }
        }

        // Populate required habits
        val rules = repository.getAllBlockRules()
        val currentRule = rules.firstOrNull { it.id == ruleId }
        val allHabitsMap = repository.getAllHabits().associateBy { it.id }

        val requiredHabits = currentRule?.requiredHabitIds?.mapNotNull { allHabitsMap[it] }
            ?: repository.getAllHabits()

        val uncompleted = requiredHabits.filterNot { it.isCompletedToday }

        tvStatusDesc.text = "Complete your daily habits to unlock. ${uncompleted.size} habit(s) remaining today."

        habitsContainer.removeAllViews()
        for (habit in uncompleted) {
            val habitCard = createHabitQuickCard(habit)
            habitsContainer.addView(habitCard)
        }

        btnGoHome.setOnClickListener {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
            finish()
        }

        if (currentRule?.strictMode == true) {
            btnSnooze.visibility = android.view.View.GONE
        } else {
            btnSnooze.setOnClickListener {
                repository.activateSnooze(1)
                Toast.makeText(this, "Emergency 1-minute unlock active. Use it wisely!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun createHabitQuickCard(habit: Habit): MaterialCardView {
        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
            radius = 16f
            setCardBackgroundColor(Color.parseColor("#1B1E26"))
            strokeColor = Color.parseColor("#2E3340")
            strokeWidth = 2
            cardElevation = 0f
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 24, 24, 24)
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val habitColor = try {
            Color.parseColor(habit.colorHex)
        } catch (e: Exception) {
            Color.parseColor("#10B981")
        }

        val iconIv = ImageView(this).apply {
            val size = (32 * resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins(0, 0, (14 * resources.displayMetrics.density).toInt(), 0)
            }
            setImageResource(habit.getIconDrawableRes())
            setColorFilter(habitColor)
        }
        row.addView(iconIv)

        val infoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
        }

        val titleTv = TextView(this).apply {
            text = habit.title
            setTextColor(Color.WHITE)
            textSize = 15f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val progressTv = TextView(this).apply {
            text = "${habit.currentValue} / ${habit.targetValue} ${habit.unit}"
            setTextColor(Color.parseColor("#9CA3AF"))
            textSize = 13f
        }

        infoLayout.addView(titleTv)
        infoLayout.addView(progressTv)

        val actionBtn = MaterialButton(this).apply {
            when (habit.type) {
                HabitType.MEDITATION_MINUTES, HabitType.POMODORO, HabitType.WORKOUT_MINUTES -> {
                    text = "Do Habit"
                    setOnClickListener {
                        val timerIntent = Intent(this@BlockedOverlayActivity, TimerActivity::class.java).apply {
                            putExtra(TimerActivity.EXTRA_HABIT_ID, habit.id)
                        }
                        startActivity(timerIntent)
                        finish()
                    }
                }
                HabitType.APP_USAGE -> {
                    text = "Open App"
                    setOnClickListener {
                        if (!habit.targetPackageName.isNullOrBlank()) {
                            val launchIntent = packageManager.getLaunchIntentForPackage(habit.targetPackageName)
                            if (launchIntent != null) {
                                startActivity(launchIntent)
                                finish()
                            }
                        }
                    }
                }
                else -> {
                    text = "Check Off"
                    setOnClickListener {
                        repository.setHabitCompleted(habit.id, true)
                        Toast.makeText(this@BlockedOverlayActivity, "${habit.title} completed!", Toast.LENGTH_SHORT).show()
                        setupUI() // Refresh
                    }
                }
            }
        }

        row.addView(infoLayout)
        row.addView(actionBtn)
        card.addView(row)
        return card
    }

    override fun onBackPressed() {
        // Redirect back to Home screen instead of returning into the blocked app
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }
}
