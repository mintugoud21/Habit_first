package com.habitsfirst.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.habitsfirst.app.data.model.BlockRule
import com.habitsfirst.app.data.model.DailyCompletion
import com.habitsfirst.app.data.model.Habit
import com.habitsfirst.app.data.model.HabitType
import com.habitsfirst.app.data.model.RuleConditionType
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HabitRepository private constructor(context: Context) {

    private val appContext: Context = context.applicationContext
    private val prefs: SharedPreferences =
        context.getSharedPreferences("habits_first_prefs", Context.MODE_PRIVATE)

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private var isInitializing = true

    fun notifyWidgetsAndTiles() {
        if (isInitializing) return
        try {
            com.habitsfirst.app.widget.HabitsAppWidgetProvider.updateAllWidgets(appContext)
            com.habitsfirst.app.service.HabitStatusTileService.requestTileUpdate(appContext)
            com.habitsfirst.app.service.HabitBlockerAccessibilityService.refreshScopingIfRunning()
        } catch (e: Throwable) {
            // Safe catch
        }
    }

    companion object {
        @Volatile
        private var instance: HabitRepository? = null

        fun getInstance(context: Context): HabitRepository {
            return instance ?: synchronized(this) {
                if (instance != null) return instance!!
                val repo = HabitRepository(context.applicationContext)
                instance = repo
                repo.onInitComplete()
                repo
            }
        }
    }

    init {
        instance = this
        try {
            checkAndInitializeDefaults()
            checkMidnightReset()
        } catch (e: Throwable) {
            android.util.Log.e("HabitsFirst", "Error during HabitRepository init", e)
        }
    }

    private fun onInitComplete() {
        isInitializing = false
    }

    fun getTodayDateStr(): String = dateFormat.format(Date())

    @Volatile
    private var isCheckingMidnight = false

    fun checkMidnightReset() {
        if (isCheckingMidnight) return
        isCheckingMidnight = true
        try {
            val lastSavedDate = prefs.getString("last_active_date", "") ?: ""
            val today = getTodayDateStr()
            if (lastSavedDate != today) {
                // Immediately set today to prevent recursive triggers
                prefs.edit().putString("last_active_date", today).apply()
                if (lastSavedDate.isNotBlank()) {
                    archiveDayCompletion(lastSavedDate)
                }
                resetDailyProgress()
            }
        } catch (e: Throwable) {
            android.util.Log.e("HabitsFirst", "Error during checkMidnightReset", e)
        } finally {
            isCheckingMidnight = false
        }
    }

    private fun resetDailyProgress() {
        val habits = loadHabitsFromPrefs().map {
            it.copy(currentValue = 0, isCompletedToday = false)
        }
        saveHabits(habits)
    }

    private fun archiveDayCompletion(dateStr: String) {
        val habits = loadHabitsFromPrefs()
        val completed = habits.count { it.isCompletedToday }
        val total = habits.size
        val record = JSONObject().apply {
            put("date", dateStr)
            put("completed", completed)
            put("total", total)
            put("goalMet", completed >= (total / 2).coerceAtLeast(1))
        }
        val historyStr = prefs.getString("history_log", "[]") ?: "[]"
        val array = JSONArray(historyStr)
        array.put(record)
        prefs.edit().putString("history_log", array.toString()).apply()
    }

    fun getDefaultHabits(): List<Habit> {
        return listOf(
            Habit(
                id = "habit_steps",
                title = "Walk 10,000 Steps",
                description = "Stay active and hit your daily movement goal",
                type = HabitType.STEP_COUNT,
                targetValue = 10000,
                currentValue = 0,
                unit = "steps",
                iconName = "ic_directions_walk",
                colorHex = "#10B981",
                isBuiltIn = true
            ),
            Habit(
                id = "habit_workout",
                title = "Work out 45 minutes",
                description = "Gym, cardio, yoga, or calisthenics session",
                type = HabitType.WORKOUT_MINUTES,
                targetValue = 45,
                currentValue = 0,
                unit = "min",
                iconName = "ic_fitness_center",
                colorHex = "#3B82F6",
                isBuiltIn = true
            ),
            Habit(
                id = "habit_meditate",
                title = "Meditate 10 minutes",
                description = "Calm your mind before reaching for screens",
                type = HabitType.MEDITATION_MINUTES,
                targetValue = 10,
                currentValue = 0,
                unit = "min",
                iconName = "ic_self_improvement",
                colorHex = "#8B5CF6",
                isBuiltIn = true
            ),
            Habit(
                id = "habit_reading",
                title = "Read or Learn 20 mins",
                description = "Read Kindle or practice language on Duolingo",
                type = HabitType.APP_USAGE,
                targetValue = 20,
                currentValue = 0,
                unit = "min",
                targetPackageName = "com.duolingo",
                iconName = "ic_menu_book",
                colorHex = "#F59E0B",
                isBuiltIn = true
            ),
            Habit(
                id = "habit_pomodoro",
                title = "25m Deep Work Pomodoro",
                description = "Complete an uninterrupted high-focus work block",
                type = HabitType.POMODORO,
                targetValue = 25,
                currentValue = 0,
                unit = "min",
                iconName = "ic_timer",
                colorHex = "#EF4444",
                isBuiltIn = true
            ),
            Habit(
                id = "habit_water",
                title = "Drink 2L of Water",
                description = "Stay hydrated and energized all day",
                type = HabitType.MANUAL_CHECK,
                targetValue = 1,
                currentValue = 0,
                unit = "done",
                iconName = "ic_local_drink",
                colorHex = "#06B6D4",
                isBuiltIn = true
            )
        )
    }

    fun getDefaultBlockRules(): List<BlockRule> {
        return listOf(
            BlockRule(
                id = "rule_social_media",
                name = "Social & Distracting Apps",
                blockedPackages = listOf(
                    "com.instagram.android",
                    "com.zhiliaoapp.musically",      // TikTok
                    "com.google.android.youtube",    // YouTube
                    "com.twitter.android",           // X / Twitter
                    "com.reddit.frontpage",          // Reddit
                    "com.facebook.katana",           // Facebook
                    "com.snapchat.android",          // Snapchat
                    "com.netflix.mediaclient"        // Netflix
                ),
                conditionType = RuleConditionType.ALL_HABITS,
                requiredHabitIds = listOf("habit_steps", "habit_workout", "habit_meditate"),
                requiredCount = 2,
                isEnabled = true,
                strictMode = false
            )
        )
    }

    private fun checkAndInitializeDefaults() {
        val habitsJson = prefs.getString("habits_list", null)
        if (habitsJson.isNullOrBlank() || habitsJson == "[]") {
            saveHabits(getDefaultHabits())
        }

        val rulesJson = prefs.getString("block_rules", null)
        if (rulesJson.isNullOrBlank() || rulesJson == "[]") {
            saveBlockRules(getDefaultBlockRules())
        }

        // Purge any fake seeded sample history so users only see their own genuine progress
        if (!prefs.getBoolean("history_sample_purged_v2", false)) {
            prefs.edit()
                .putString("history_log", "[]")
                .putBoolean("history_sample_purged_v2", true)
                .apply()
        }
    }

    // --- Habits CRUD ---

    fun getAllHabits(): List<Habit> {
        try {
            checkMidnightReset()
        } catch (_: Throwable) {}
        return loadHabitsFromPrefs()
    }

    private fun loadHabitsFromPrefs(): List<Habit> {
        return try {
            val jsonStr = prefs.getString("habits_list", null)
            if (jsonStr.isNullOrBlank() || jsonStr == "[]") {
                val defaults = getDefaultHabits()
                saveHabits(defaults)
                return defaults
            }
            val array = JSONArray(jsonStr)
            val list = mutableListOf<Habit>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val type = try {
                    HabitType.valueOf(obj.getString("type"))
                } catch (e: Throwable) {
                    HabitType.MANUAL_CHECK
                }
                list.add(
                    Habit(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        description = obj.optString("description", ""),
                        type = type,
                        targetValue = obj.optInt("targetValue", 1),
                        currentValue = obj.optInt("currentValue", 0),
                        unit = obj.optString("unit", ""),
                        targetPackageName = if (obj.has("targetPackageName")) obj.getString("targetPackageName") else null,
                        iconName = obj.optString("iconName", "ic_check"),
                        colorHex = obj.optString("colorHex", "#10B981"),
                        isCompletedToday = obj.optBoolean("isCompletedToday", false),
                        isBuiltIn = obj.optBoolean("isBuiltIn", false)
                    )
                )
            }
            if (list.isEmpty()) {
                val defaults = getDefaultHabits()
                saveHabits(defaults)
                defaults
            } else {
                list
            }
        } catch (e: Throwable) {
            val defaults = getDefaultHabits()
            try { saveHabits(defaults) } catch (_: Throwable) {}
            defaults
        }
    }

    fun saveHabits(habits: List<Habit>) {
        val array = JSONArray()
        for (h in habits) {
            val obj = JSONObject().apply {
                put("id", h.id)
                put("title", h.title)
                put("description", h.description)
                put("type", h.type.name)
                put("targetValue", h.targetValue)
                put("currentValue", h.currentValue)
                put("unit", h.unit)
                if (h.targetPackageName != null) put("targetPackageName", h.targetPackageName)
                put("iconName", h.iconName)
                put("colorHex", h.colorHex)
                put("isCompletedToday", h.isCompletedToday)
                put("isBuiltIn", h.isBuiltIn)
            }
            array.put(obj)
        }
        prefs.edit().putString("habits_list", array.toString()).apply()
        notifyWidgetsAndTiles()
    }

    fun updateHabitProgress(habitId: String, addedValue: Int) {
        val habits = getAllHabits().map { h ->
            if (h.id == habitId) {
                val newVal = (h.currentValue + addedValue).coerceAtLeast(0)
                val completed = newVal >= h.targetValue
                h.copy(currentValue = newVal, isCompletedToday = completed)
            } else {
                h
            }
        }
        saveHabits(habits)
    }

    fun setHabitCompleted(habitId: String, completed: Boolean) {
        val habits = getAllHabits().map { h ->
            if (h.id == habitId) {
                h.copy(
                    isCompletedToday = completed,
                    currentValue = if (completed) h.targetValue else 0
                )
            } else {
                h
            }
        }
        saveHabits(habits)
    }

    fun addHabit(habit: Habit) {
        val list = getAllHabits().toMutableList()
        list.add(habit)
        saveHabits(list)
    }

    fun updateHabit(updatedHabit: Habit) {
        val list = getAllHabits().map { if (it.id == updatedHabit.id) updatedHabit else it }
        saveHabits(list)
    }

    fun resetDefaultHabits() {
        saveHabits(getDefaultHabits())
    }

    fun deleteHabit(habitId: String) {
        val list = getAllHabits().filterNot { it.id == habitId }
        saveHabits(list)
    }

    // --- Block Rules CRUD ---

    fun getAllBlockRules(): List<BlockRule> {
        return try {
            val jsonStr = prefs.getString("block_rules", null)
            if (jsonStr.isNullOrBlank() || jsonStr == "[]") {
                val defaults = getDefaultBlockRules()
                saveBlockRules(defaults)
                return defaults
            }
            val array = JSONArray(jsonStr)
            val list = mutableListOf<BlockRule>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val pkgsArray = obj.optJSONArray("blockedPackages") ?: JSONArray()
                val pkgs = mutableListOf<String>()
                for (p in 0 until pkgsArray.length()) {
                    val pkgName = pkgsArray.getString(p)
                    if (!com.habitsfirst.app.util.PaymentSafetyHelper.isExemptFromBlocking(pkgName)) {
                        pkgs.add(pkgName)
                    }
                }

                val habitsArray = obj.optJSONArray("requiredHabitIds") ?: JSONArray()
                val reqHabits = mutableListOf<String>()
                for (h in 0 until habitsArray.length()) reqHabits.add(habitsArray.getString(h))

                val conditionType = try {
                    RuleConditionType.valueOf(obj.optString("conditionType", "ALL_HABITS"))
                } catch (e: Throwable) {
                    RuleConditionType.ALL_HABITS
                }

                list.add(
                    BlockRule(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        blockedPackages = pkgs,
                        conditionType = conditionType,
                        requiredHabitIds = reqHabits,
                        requiredCount = obj.optInt("requiredCount", 1),
                        isEnabled = obj.optBoolean("isEnabled", true),
                        strictMode = obj.optBoolean("strictMode", false),
                        startTime = if (obj.has("startTime")) obj.getString("startTime") else null,
                        endTime = if (obj.has("endTime")) obj.getString("endTime") else null
                    )
                )
            }
            if (list.isEmpty()) {
                val defaults = getDefaultBlockRules()
                saveBlockRules(defaults)
                defaults
            } else {
                list
            }
        } catch (e: Throwable) {
            val defaults = getDefaultBlockRules()
            try { saveBlockRules(defaults) } catch (_: Throwable) {}
            defaults
        }
    }

    fun resetDefaultBlockRules() {
        saveBlockRules(getDefaultBlockRules())
    }

    fun getAllBlockedPackages(): List<String> {
        return getAllBlockRules()
            .filter { it.isEnabled }
            .flatMap { it.blockedPackages }
            .distinct()
            .filterNot { com.habitsfirst.app.util.PaymentSafetyHelper.isExemptFromBlocking(it) }
    }

    fun saveBlockRules(rules: List<BlockRule>) {
        val array = JSONArray()
        for (r in rules) {
            val safePkgs = r.blockedPackages.filterNot { com.habitsfirst.app.util.PaymentSafetyHelper.isExemptFromBlocking(it) }
            val obj = JSONObject().apply {
                put("id", r.id)
                put("name", r.name)
                put("blockedPackages", JSONArray(safePkgs))
                put("conditionType", r.conditionType.name)
                put("requiredHabitIds", JSONArray(r.requiredHabitIds))
                put("requiredCount", r.requiredCount)
                put("isEnabled", r.isEnabled)
                put("strictMode", r.strictMode)
                if (r.startTime != null) put("startTime", r.startTime)
                if (r.endTime != null) put("endTime", r.endTime)
            }
            array.put(obj)
        }
        prefs.edit().putString("block_rules", array.toString()).apply()
        notifyWidgetsAndTiles()
    }

    fun updateBlockRule(rule: BlockRule) {
        val rules = getAllBlockRules().map { if (it.id == rule.id) rule else it }
        saveBlockRules(rules)
    }

    fun addBlockRule(rule: BlockRule) {
        val list = getAllBlockRules().toMutableList()
        list.add(rule)
        saveBlockRules(list)
    }

    fun deleteBlockRule(ruleId: String) {
        val list = getAllBlockRules().filterNot { it.id == ruleId }
        saveBlockRules(list)
    }

    // --- Blocking Decision Engine ---

    fun shouldBlockPackage(packageName: String): Pair<Boolean, BlockRule?> {
        // Strict safety exclusion: Never block payment, banking, or essential apps
        if (com.habitsfirst.app.util.PaymentSafetyHelper.isExemptFromBlocking(packageName)) {
            return Pair(false, null)
        }

        val rules = getAllBlockRules().filter { it.isEnabled && it.isScheduleActiveNow() }
        val matchingRule = rules.firstOrNull { it.blockedPackages.contains(packageName) } ?: return Pair(false, null)

        // Check if snooze is active
        if (isSnoozeActive()) {
            return Pair(false, null)
        }

        val habitsMap = getAllHabits().associateBy { it.id }
        val isFulfilled = matchingRule.isRequirementFulfilled(habitsMap)

        return Pair(!isFulfilled, matchingRule)
    }

    // --- Snooze (Emergency Break / Payment Pause) ---

    fun activateSnooze(durationMinutes: Int = 1) {
        val expiryTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        prefs.edit().putLong("snooze_until", expiryTime).apply()
        notifyWidgetsAndTiles()
    }

    fun cancelSnooze() {
        prefs.edit().remove("snooze_until").apply()
        notifyWidgetsAndTiles()
    }

    fun isSnoozeActive(): Boolean {
        val snoozeUntil = prefs.getLong("snooze_until", 0L)
        return System.currentTimeMillis() < snoozeUntil
    }

    fun getRemainingSnoozeSeconds(): Int {
        val snoozeUntil = prefs.getLong("snooze_until", 0L)
        val diff = (snoozeUntil - System.currentTimeMillis()) / 1000
        return diff.coerceAtLeast(0L).toInt()
    }

    // --- History & Streaks ---

    fun getHistoryCompletions(): List<DailyCompletion> {
        val jsonStr = prefs.getString("history_log", "[]") ?: "[]"
        val array = try { JSONArray(jsonStr) } catch (_: Throwable) { JSONArray() }
        val list = mutableListOf<DailyCompletion>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            list.add(
                DailyCompletion(
                    dateStr = obj.getString("date"),
                    completedCount = obj.getInt("completed"),
                    totalCount = obj.getInt("total"),
                    isGoalMet = obj.optBoolean("goalMet", false)
                )
            )
        }

        // Add today's live completion without duplicating
        val todayStr = getTodayDateStr()
        list.removeAll { it.dateStr == todayStr }

        val todayHabits = getAllHabits()
        val todayCompleted = todayHabits.count { it.isCompletedToday }
        val isGoalMetToday = todayCompleted > 0 && todayCompleted >= (todayHabits.size / 2).coerceAtLeast(1)

        list.add(
            DailyCompletion(
                dateStr = todayStr,
                completedCount = todayCompleted,
                totalCount = todayHabits.size,
                isGoalMet = isGoalMetToday
            )
        )

        return list.sortedBy { it.dateStr }
    }

    fun clearStreakAndHistory() {
        prefs.edit()
            .putString("history_log", "[]")
            .remove("last_active_date")
            .apply()
        notifyWidgetsAndTiles()
    }

    fun getCurrentStreak(): Int {
        val history = getHistoryCompletions()
        val todayStr = getTodayDateStr()
        var streak = 0

        // Count consecutive completed days from yesterday backwards.
        // Today is excluded from the streak calculation since the day is not yet over.
        for (i in history.indices.reversed()) {
            val item = history[i]
            if (item.dateStr == todayStr) {
                continue
            }
            if (item.isGoalMet) {
                streak++
            } else {
                break
            }
        }
        return streak
    }

    fun getBestStreak(): Int {
        val history = getHistoryCompletions()
        var maxStreak = 0
        var current = 0
        for (item in history) {
            if (item.isGoalMet) {
                current++
                if (current > maxStreak) maxStreak = current
            } else {
                current = 0
            }
        }
        return maxStreak.coerceAtLeast(getCurrentStreak())
    }

    fun getRemainingHabits(): List<Habit> {
        return getAllHabits().filterNot { it.isCompletedToday }
    }

    fun isAllHabitsCompleted(): Boolean {
        val habits = getAllHabits()
        return habits.isNotEmpty() && habits.all { it.isCompletedToday }
    }

    fun getHabitsCompletionStatus(): Pair<Int, Int> {
        val habits = getAllHabits()
        val completed = habits.count { it.isCompletedToday }
        return Pair(completed, habits.size)
    }

    // --- Daily Reminder Preferences ---

    fun areRemindersEnabled(): Boolean {
        return prefs.getBoolean("reminders_enabled", true)
    }

    fun setRemindersEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("reminders_enabled", enabled).apply()
    }

    // --- Backup & Restore (JSON) ---

    fun exportBackupJson(): String {
        val root = JSONObject().apply {
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())
            put("habits", JSONArray(prefs.getString("habits_list", "[]") ?: "[]"))
            put("blockRules", JSONArray(prefs.getString("block_rules", "[]") ?: "[]"))
            put("historyLog", JSONArray(prefs.getString("history_log", "[]") ?: "[]"))
        }
        return root.toString(2)
    }

    fun importBackupJson(jsonStr: String): Boolean {
        return try {
            val root = JSONObject(jsonStr)
            if (root.has("habits")) {
                prefs.edit().putString("habits_list", root.getJSONArray("habits").toString()).apply()
            }
            if (root.has("blockRules")) {
                prefs.edit().putString("block_rules", root.getJSONArray("blockRules").toString()).apply()
            }
            if (root.has("historyLog")) {
                prefs.edit().putString("history_log", root.getJSONArray("historyLog").toString()).apply()
            }
            notifyWidgetsAndTiles()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

