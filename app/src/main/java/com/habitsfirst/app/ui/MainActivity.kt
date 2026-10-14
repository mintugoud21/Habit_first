package com.habitsfirst.app.ui

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.habitsfirst.app.R
import com.habitsfirst.app.data.model.BlockRule
import com.habitsfirst.app.data.model.Habit
import com.habitsfirst.app.data.model.HabitType
import com.habitsfirst.app.data.model.RuleConditionType
import com.habitsfirst.app.data.repository.HabitRepository
import com.habitsfirst.app.service.StepSensorHelper
import com.habitsfirst.app.service.UsageTrackerHelper
import com.habitsfirst.app.ui.adapters.BlockRuleAdapter
import com.habitsfirst.app.ui.adapters.HabitAdapter
import com.habitsfirst.app.ui.adapters.SettingsHabitAdapter
import com.habitsfirst.app.ui.views.CircularProgressView
import com.habitsfirst.app.ui.views.ContributionHeatmapView
import com.habitsfirst.app.util.NotificationHelper
import com.habitsfirst.app.util.PaymentSafetyHelper
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var repository: HabitRepository
    private lateinit var usageTracker: UsageTrackerHelper
    private lateinit var stepHelper: StepSensorHelper

    // Views
    private lateinit var todayProgressView: CircularProgressView
    private lateinit var tvHabitsCount: TextView
    private lateinit var rvHabits: RecyclerView
    private lateinit var habitAdapter: HabitAdapter

    private lateinit var rvSettingsHabits: RecyclerView
    private lateinit var settingsHabitAdapter: SettingsHabitAdapter

    private lateinit var rvBlockRules: RecyclerView
    private lateinit var blockRuleAdapter: BlockRuleAdapter

    private lateinit var heatmapView: ContributionHeatmapView
    private lateinit var tvCurrentStreak: TextView
    private lateinit var tvBestStreak: TextView

    // Bottom tab containers
    private lateinit var layoutHabitsTab: View
    private lateinit var layoutBlocksTab: View
    private lateinit var layoutStreaksTab: View
    private lateinit var layoutSettingsTab: View

    // Tab buttons
    private lateinit var btnTabHabits: View
    private lateinit var btnTabBlocks: View
    private lateinit var btnTabStreaks: View
    private lateinit var btnTabSettings: View

    private lateinit var ivTabHabits: ImageView
    private lateinit var ivTabBlocks: ImageView
    private lateinit var ivTabStreaks: ImageView
    private lateinit var ivTabSettings: ImageView

    private lateinit var tvTabHabits: TextView
    private lateinit var tvTabBlocks: TextView
    private lateinit var tvTabStreaks: TextView
    private lateinit var tvTabSettings: TextView

    // Permission cards
    private lateinit var cardAccessibilityNotice: MaterialCardView
    private lateinit var cardUsageNotice: MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = HabitRepository.getInstance(this)
        usageTracker = UsageTrackerHelper(this)
        stepHelper = StepSensorHelper(this)

        initViews()
        setupAdapters()
        setupTabs()
        setupListeners()
        applySystemInsets()

        // Notification channel and daily reminder scheduling
        NotificationHelper.createNotificationChannel(this)
        if (repository.areRemindersEnabled()) {
            NotificationHelper.scheduleDailyReminders(this)
        }

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun applySystemInsets() {
        try {
            val rootLayout = findViewById<View>(R.id.main_root_layout) ?: return
            val topAppBar = findViewById<View>(R.id.top_app_bar)
            val bottomNavBar = findViewById<View>(R.id.bottom_nav_bar)

            val topPaddingOriginal = topAppBar?.paddingTop ?: 0
            val bottomPaddingOriginal = bottomNavBar?.paddingBottom ?: 0

            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { _, insets ->
                try {
                    val statusBars = insets.getInsets(
                        androidx.core.view.WindowInsetsCompat.Type.statusBars() or
                        androidx.core.view.WindowInsetsCompat.Type.displayCutout()
                    )
                    val navBars = insets.getInsets(
                        androidx.core.view.WindowInsetsCompat.Type.navigationBars()
                    )

                    topAppBar?.let {
                        it.setPadding(
                            it.paddingLeft,
                            topPaddingOriginal + statusBars.top,
                            it.paddingRight,
                            it.paddingBottom
                        )
                    }

                    bottomNavBar?.let {
                        it.setPadding(
                            it.paddingLeft,
                            it.paddingTop,
                            it.paddingRight,
                            bottomPaddingOriginal + navBars.bottom
                        )
                    }
                } catch (_: Throwable) {}
                insets
            }
        } catch (_: Throwable) {}
    }

    override fun onResume() {
        super.onResume()
        refreshAllData()
        checkPermissions()
        stepHelper.startListening()
        checkProductiveAppsUsage()
    }

    override fun onPause() {
        super.onPause()
        stepHelper.stopListening()
    }

    private fun initViews() {
        todayProgressView = findViewById(R.id.progress_today)
        tvHabitsCount = findViewById(R.id.tv_habits_status_summary)
        rvHabits = findViewById(R.id.rv_habits_list)

        rvSettingsHabits = findViewById(R.id.rv_settings_habits)

        rvBlockRules = findViewById(R.id.rv_block_rules)

        heatmapView = findViewById(R.id.heatmap_view)
        tvCurrentStreak = findViewById(R.id.tv_current_streak)
        tvBestStreak = findViewById(R.id.tv_best_streak)

        layoutHabitsTab = findViewById(R.id.tab_content_habits)
        layoutBlocksTab = findViewById(R.id.tab_content_blocks)
        layoutStreaksTab = findViewById(R.id.tab_content_streaks)
        layoutSettingsTab = findViewById(R.id.tab_content_settings)

        btnTabHabits = findViewById(R.id.tab_btn_habits)
        btnTabBlocks = findViewById(R.id.tab_btn_blocks)
        btnTabStreaks = findViewById(R.id.tab_btn_streaks)
        btnTabSettings = findViewById(R.id.tab_btn_settings)

        ivTabHabits = findViewById(R.id.iv_tab_habits)
        ivTabBlocks = findViewById(R.id.iv_tab_blocks)
        ivTabStreaks = findViewById(R.id.iv_tab_streaks)
        ivTabSettings = findViewById(R.id.iv_tab_settings)

        tvTabHabits = findViewById(R.id.tv_tab_habits)
        tvTabBlocks = findViewById(R.id.tv_tab_blocks)
        tvTabStreaks = findViewById(R.id.tv_tab_streaks)
        tvTabSettings = findViewById(R.id.tv_tab_settings)

        // Set modern header date
        val dateFormatHeader = java.text.SimpleDateFormat("EEEE, MMM d", java.util.Locale.getDefault())
        findViewById<TextView>(R.id.tv_header_date)?.text = dateFormatHeader.format(java.util.Date()).uppercase()

        cardAccessibilityNotice = findViewById(R.id.card_accessibility_notice)
        cardUsageNotice = findViewById(R.id.card_usage_notice)
    }

    private fun setupAdapters() {
        habitAdapter = HabitAdapter(
            habits = repository.getAllHabits(),
            onHabitToggle = { habit, isChecked ->
                repository.setHabitCompleted(habit.id, isChecked)
                rvHabits.post { refreshAllData() }
            },
            onHabitAction = { habit ->
                handleHabitAction(habit)
            }
        )
        rvHabits.layoutManager = LinearLayoutManager(this)
        rvHabits.adapter = habitAdapter

        settingsHabitAdapter = SettingsHabitAdapter(
            habits = repository.getAllHabits(),
            onEditHabit = { habit ->
                showEditHabitDialog(habit)
            },
            onDeleteHabit = { habit ->
                showDeleteHabitConfirmation(habit)
            }
        )
        rvSettingsHabits.layoutManager = LinearLayoutManager(this)
        rvSettingsHabits.adapter = settingsHabitAdapter

        blockRuleAdapter = BlockRuleAdapter(
            rules = repository.getAllBlockRules(),
            onToggleEnabled = { rule, isEnabled ->
                repository.updateBlockRule(rule.copy(isEnabled = isEnabled))
                rvBlockRules.post { refreshAllData() }
            },
            onManageApps = { rule ->
                showAppPickerDialog(rule)
            }
        )
        rvBlockRules.layoutManager = LinearLayoutManager(this)
        rvBlockRules.adapter = blockRuleAdapter
    }

    private fun setupTabs() {
        selectTab(0)
        btnTabHabits.setOnClickListener { selectTab(0) }
        btnTabBlocks.setOnClickListener { selectTab(1) }
        btnTabStreaks.setOnClickListener { selectTab(2) }
        btnTabSettings.setOnClickListener { selectTab(3) }
    }

    private fun selectTab(index: Int) {
        layoutHabitsTab.visibility = if (index == 0) View.VISIBLE else View.GONE
        layoutBlocksTab.visibility = if (index == 1) View.VISIBLE else View.GONE
        layoutStreaksTab.visibility = if (index == 2) View.VISIBLE else View.GONE
        layoutSettingsTab.visibility = if (index == 3) View.VISIBLE else View.GONE

        val colorActive = androidx.core.content.ContextCompat.getColor(this, R.color.primary_emerald)
        val colorInactive = androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary)

        // Dim inactive tabs
        btnTabHabits.alpha = if (index == 0) 1.0f else 0.55f
        btnTabBlocks.alpha = if (index == 1) 1.0f else 0.55f
        btnTabStreaks.alpha = if (index == 2) 1.0f else 0.55f
        btnTabSettings.alpha = if (index == 3) 1.0f else 0.55f

        // Tab 0
        ivTabHabits.setColorFilter(if (index == 0) colorActive else colorInactive)
        tvTabHabits.setTextColor(if (index == 0) colorActive else colorInactive)
        tvTabHabits.typeface = if (index == 0) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT

        // Tab 1
        ivTabBlocks.setColorFilter(if (index == 1) colorActive else colorInactive)
        tvTabBlocks.setTextColor(if (index == 1) colorActive else colorInactive)
        tvTabBlocks.typeface = if (index == 1) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT

        // Tab 2
        ivTabStreaks.setColorFilter(if (index == 2) colorActive else colorInactive)
        tvTabStreaks.setTextColor(if (index == 2) colorActive else colorInactive)
        tvTabStreaks.typeface = if (index == 2) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT

        // Tab 3
        ivTabSettings.setColorFilter(if (index == 3) colorActive else colorInactive)
        tvTabSettings.setTextColor(if (index == 3) colorActive else colorInactive)
        tvTabSettings.typeface = if (index == 3) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
    }

    private fun setupListeners() {
        findViewById<MaterialButton>(R.id.btn_add_habit).setOnClickListener {
            showEditHabitDialog(null)
        }

        findViewById<MaterialButton>(R.id.btn_settings_add_habit).setOnClickListener {
            showEditHabitDialog(null)
        }

        findViewById<MaterialButton>(R.id.btn_reset_default_habits).setOnClickListener {
            showResetDefaultsConfirmation()
        }

        findViewById<MaterialButton>(R.id.btn_pause_for_payment).setOnClickListener {
            repository.activateSnooze(10)
            Toast.makeText(this, "🛡️ Blocker paused for 10 minutes. Complete your payments safely!", Toast.LENGTH_LONG).show()
        }

        findViewById<MaterialButton>(R.id.btn_enable_accessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        findViewById<MaterialButton>(R.id.btn_enable_usage_stats).setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        // Daily Reminders Toggle Switch
        val switchReminders = findViewById<SwitchMaterial>(R.id.switch_reminders)
        switchReminders.isChecked = repository.areRemindersEnabled()
        switchReminders.setOnCheckedChangeListener { _, isChecked ->
            repository.setRemindersEnabled(isChecked)
            if (isChecked) {
                NotificationHelper.scheduleDailyReminders(this)
                Toast.makeText(this, "Daily morning & evening reminders enabled", Toast.LENGTH_SHORT).show()
            } else {
                NotificationHelper.cancelDailyReminders(this)
                Toast.makeText(this, "Daily reminders disabled", Toast.LENGTH_SHORT).show()
            }
        }

        // Export Backup
        findViewById<MaterialButton>(R.id.btn_export_backup).setOnClickListener {
            showExportBackupDialog()
        }

        // Import Backup
        findViewById<MaterialButton>(R.id.btn_import_backup).setOnClickListener {
            showImportBackupDialog()
        }
    }

    private fun refreshAllData() {
        try {
            val habits = repository.getAllHabits()
            val completedCount = habits.count { it.isCompletedToday }
            val totalCount = habits.size
            val fraction = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f

            todayProgressView.setProgress(fraction)
            todayProgressView.setCenterLabel("$completedCount/$totalCount", "completed")
            tvHabitsCount.text = if (completedCount == totalCount && totalCount > 0) {
                "🎉 All habits finished! Apps are completely unlocked."
            } else {
                "${totalCount - completedCount} habits left before apps unlock."
            }

            habitAdapter.updateData(habits)
            settingsHabitAdapter.updateData(habits)
            blockRuleAdapter.updateData(repository.getAllBlockRules())

            // Refresh heatmap & streaks
            heatmapView.setData(repository.getHistoryCompletions())
            tvCurrentStreak.text = "${repository.getCurrentStreak()} Days"
            tvBestStreak.text = "${repository.getBestStreak()} Days"
        } catch (e: Throwable) {
            android.util.Log.e("HabitsFirst", "Error refreshing data: ${e.message}")
        }
    }

    private fun checkPermissions() {
        try {
            val hasUsage = usageTracker.hasUsageStatsPermission()
            cardUsageNotice.visibility = if (hasUsage) View.GONE else View.VISIBLE

            // Accessibility service check
            val isAccessibilityEnabled = isAccessibilityServiceEnabled()
            cardAccessibilityNotice.visibility = if (isAccessibilityEnabled) View.GONE else View.VISIBLE
        } catch (_: Throwable) {}
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        return try {
            val expectedServiceName = "${packageName}/com.habitsfirst.app.service.HabitBlockerAccessibilityService"
            val enabledServices = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            enabledServices.contains(expectedServiceName)
        } catch (_: Throwable) {
            false
        }
    }

    private fun checkProductiveAppsUsage() {
        try {
            val habits = repository.getAllHabits().filter { it.type == HabitType.APP_USAGE }
            for (habit in habits) {
                val pkg = habit.targetPackageName ?: continue
                val minutesToday = usageTracker.getMinutesUsedToday(pkg)
                if (habit.currentValue != minutesToday) {
                    val isCompleted = minutesToday >= habit.targetValue
                    repository.updateHabit(habit.copy(currentValue = minutesToday, isCompletedToday = isCompleted))
                }
            }
        } catch (_: Throwable) {}
    }

    private fun handleHabitAction(habit: Habit) {
        when (habit.type) {
            HabitType.STEP_COUNT -> {
                repository.updateHabitProgress(habit.id, 1000)
                refreshAllData()
                Toast.makeText(this, "+1,000 steps logged!", Toast.LENGTH_SHORT).show()
            }
            HabitType.MEDITATION_MINUTES, HabitType.POMODORO, HabitType.WORKOUT_MINUTES -> {
                val intent = Intent(this, TimerActivity::class.java).apply {
                    putExtra(TimerActivity.EXTRA_HABIT_ID, habit.id)
                }
                startActivity(intent)
            }
            HabitType.APP_USAGE -> {
                if (!habit.targetPackageName.isNullOrBlank()) {
                    val launchIntent = packageManager.getLaunchIntentForPackage(habit.targetPackageName)
                    if (launchIntent != null) {
                        startActivity(launchIntent)
                    } else {
                        Toast.makeText(this, "Target app is not installed on this device.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            HabitType.MANUAL_CHECK -> {
                repository.setHabitCompleted(habit.id, true)
                refreshAllData()
            }
        }
    }

    private fun showEditHabitDialog(existingHabit: Habit?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_habit, null)
        val etTitle = dialogView.findViewById<EditText>(R.id.et_edit_habit_title)
        val etDesc = dialogView.findViewById<EditText>(R.id.et_edit_habit_desc)
        val etTarget = dialogView.findViewById<EditText>(R.id.et_edit_habit_target)
        val etUnit = dialogView.findViewById<EditText>(R.id.et_edit_habit_unit)
        val layoutIconPicker = dialogView.findViewById<LinearLayout>(R.id.layout_icon_picker)
        val layoutColorPicker = dialogView.findViewById<LinearLayout>(R.id.layout_color_picker)
        val rgType = dialogView.findViewById<RadioGroup>(R.id.rg_edit_habit_type)
        val rbManual = dialogView.findViewById<RadioButton>(R.id.rb_edit_manual)
        val rbTimer = dialogView.findViewById<RadioButton>(R.id.rb_edit_timer)
        val rbSteps = dialogView.findViewById<RadioButton>(R.id.rb_edit_steps)
        val rbApp = dialogView.findViewById<RadioButton>(R.id.rb_edit_app)

        var selectedIcon = existingHabit?.iconName ?: "ic_check"
        var selectedColor = existingHabit?.colorHex ?: "#10B981"

        if (existingHabit != null) {
            etTitle.setText(existingHabit.title)
            etDesc.setText(existingHabit.description)
            etTarget.setText(existingHabit.targetValue.toString())
            etUnit.setText(existingHabit.unit)

            when (existingHabit.type) {
                HabitType.STEP_COUNT -> rbSteps.isChecked = true
                HabitType.WORKOUT_MINUTES, HabitType.MEDITATION_MINUTES, HabitType.POMODORO -> rbTimer.isChecked = true
                HabitType.APP_USAGE -> rbApp.isChecked = true
                HabitType.MANUAL_CHECK -> rbManual.isChecked = true
            }
        }

        val isNight = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES

        fun refreshIconPicker() {
            layoutIconPicker.removeAllViews()
            val density = resources.displayMetrics.density
            val size = (42 * density).toInt()
            val iconSize = (22 * density).toInt()

            for ((iconName, _) in Habit.AVAILABLE_ICONS) {
                val isSelected = iconName == selectedIcon
                val frame = FrameLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(size, size).apply {
                        setMargins(0, 0, (10 * density).toInt(), 0)
                    }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        if (isSelected) {
                            setColor(Color.parseColor(selectedColor))
                            setStroke((2 * density).toInt(), if (isNight) Color.WHITE else Color.parseColor("#0F172A"))
                        } else {
                            setColor(if (isNight) Color.parseColor("#1B1E26") else Color.parseColor("#E2E8F0"))
                            setStroke((1 * density).toInt(), if (isNight) Color.parseColor("#2E3340") else Color.parseColor("#CBD5E1"))
                        }
                    }
                }

                val iconView = ImageView(this).apply {
                    layoutParams = FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER)
                    setImageResource(Habit.getIconResByName(iconName))
                    if (isSelected) {
                        setColorFilter(Color.WHITE)
                    } else {
                        setColorFilter(if (isNight) Color.parseColor("#9CA3AF") else Color.parseColor("#475569"))
                    }
                }
                frame.addView(iconView)
                frame.setOnClickListener {
                    selectedIcon = iconName
                    refreshIconPicker()
                }
                layoutIconPicker.addView(frame)
            }
        }

        fun refreshColorPicker() {
            layoutColorPicker.removeAllViews()
            val density = resources.displayMetrics.density
            val size = (36 * density).toInt()

            for (hex in Habit.AVAILABLE_COLORS) {
                val isSelected = hex.equals(selectedColor, ignoreCase = true)
                val colorFrame = FrameLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(size, size).apply {
                        setMargins(0, 0, (10 * density).toInt(), 0)
                    }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor(hex))
                        if (isSelected) {
                            setStroke((3 * density).toInt(), Color.WHITE)
                        }
                    }
                    setOnClickListener {
                        selectedColor = hex
                        refreshColorPicker()
                        refreshIconPicker()
                    }
                }
                layoutColorPicker.addView(colorFrame)
            }
        }

        refreshIconPicker()
        refreshColorPicker()

        val dialogBuilder = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(if (existingHabit != null) "Edit Habit" else "Add New Habit")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val title = etTitle.text.toString().trim()
                if (title.isBlank()) {
                    Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val target = etTarget.text.toString().toIntOrNull() ?: 1
                val desc = etDesc.text.toString().trim().ifBlank { "Daily habit goal" }

                val type = when (rgType.checkedRadioButtonId) {
                    R.id.rb_edit_steps -> HabitType.STEP_COUNT
                    R.id.rb_edit_timer -> {
                        if (existingHabit != null && (existingHabit.type == HabitType.MEDITATION_MINUTES ||
                                existingHabit.type == HabitType.WORKOUT_MINUTES ||
                                existingHabit.type == HabitType.POMODORO)) {
                            existingHabit.type
                        } else {
                            HabitType.WORKOUT_MINUTES
                        }
                    }
                    R.id.rb_edit_app -> HabitType.APP_USAGE
                    else -> HabitType.MANUAL_CHECK
                }

                val enteredUnit = etUnit.text.toString().trim()
                val unit = if (enteredUnit.isNotBlank()) {
                    enteredUnit
                } else {
                    when (type) {
                        HabitType.STEP_COUNT -> "steps"
                        HabitType.MEDITATION_MINUTES, HabitType.WORKOUT_MINUTES, HabitType.POMODORO -> "min"
                        else -> "done"
                    }
                }

                if (existingHabit != null) {
                    val updated = existingHabit.copy(
                        title = title,
                        description = desc,
                        type = type,
                        targetValue = target,
                        unit = unit,
                        iconName = selectedIcon,
                        colorHex = selectedColor
                    )
                    repository.updateHabit(updated)
                    refreshAllData()
                    Toast.makeText(this, "Updated '$title'", Toast.LENGTH_SHORT).show()
                } else {
                    val newHabit = Habit(
                        id = "habit_${UUID.randomUUID()}",
                        title = title,
                        description = desc,
                        type = type,
                        targetValue = target,
                        currentValue = 0,
                        unit = unit,
                        iconName = selectedIcon,
                        colorHex = selectedColor
                    )
                    repository.addHabit(newHabit)
                    refreshAllData()
                    Toast.makeText(this, "Added '$title'", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)

        if (existingHabit != null) {
            dialogBuilder.setNeutralButton("Delete") { _, _ ->
                showDeleteHabitConfirmation(existingHabit)
            }
        }

        dialogBuilder.show()
    }

    private fun showDeleteHabitConfirmation(habit: Habit) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Delete Habit")
            .setMessage("Are you sure you want to delete '${habit.title}'?")
            .setPositiveButton("Delete") { _, _ ->
                repository.deleteHabit(habit.id)
                refreshAllData()
                Toast.makeText(this, "Deleted '${habit.title}'", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showResetDefaultsConfirmation() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Restore Default Habits")
            .setMessage("This will reset your habits to the default set (Walking, Workout, Meditation, Reading, Pomodoro, Water). Continue?")
            .setPositiveButton("Restore") { _, _ ->
                repository.resetDefaultHabits()
                refreshAllData()
                Toast.makeText(this, "Default habits restored", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAppPickerDialog(rule: BlockRule) {
        val pm = packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || isPopularSocialApp(it.packageName) }
            .filterNot { PaymentSafetyHelper.isExemptFromBlocking(it.packageName) }
            .sortedBy { pm.getApplicationLabel(it).toString().lowercase() }

        val appNames = installedApps.map { pm.getApplicationLabel(it).toString() }.toTypedArray()
        val checkedItems = BooleanArray(installedApps.size) { i ->
            rule.blockedPackages.contains(installedApps[i].packageName)
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Select Blocked Apps")
            .setMultiChoiceItems(appNames, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("Save") { _, _ ->
                val selectedPackages = mutableListOf<String>()
                for (i in checkedItems.indices) {
                    if (checkedItems[i]) {
                        selectedPackages.add(installedApps[i].packageName)
                    }
                }
                repository.updateBlockRule(rule.copy(blockedPackages = selectedPackages))
                refreshAllData()
                Toast.makeText(this, "Updated blocked apps for ${rule.name}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun isPopularSocialApp(pkg: String): Boolean {
        return pkg.contains("instagram") || pkg.contains("tiktok") ||
                pkg.contains("youtube") || pkg.contains("twitter") ||
                pkg.contains("reddit") || pkg.contains("facebook")
    }

    private fun showExportBackupDialog() {
        val json = repository.exportBackupJson()
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Habits First Backup", json)
        clipboard.setPrimaryClip(clip)

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Backup Data Exported")
            .setMessage("Your habits, rules, and streaks JSON has been copied to your clipboard.\n\nYou can also share it directly:")
            .setPositiveButton("Share") { _, _ ->
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, json)
                    type = "text/plain"
                }
                startActivity(Intent.createChooser(sendIntent, "Share Habits First Backup"))
            }
            .setNegativeButton("Done", null)
            .show()
    }

    private fun showImportBackupDialog() {
        val etInput = EditText(this).apply {
            hint = "Paste exported JSON here"
            minLines = 5
            maxLines = 10
            setPadding(32, 24, 32, 24)
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Import Backup Data")
            .setMessage("Paste your exported Habits First JSON. This will update your habits, rules, and streak history.")
            .setView(etInput)
            .setPositiveButton("Restore") { _, _ ->
                val text = etInput.text.toString().trim()
                if (text.isNotEmpty() && repository.importBackupJson(text)) {
                    refreshAllData()
                    Toast.makeText(this, "Backup successfully restored!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Invalid JSON data. Import failed.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

