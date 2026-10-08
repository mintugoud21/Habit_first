package com.habitsfirst.app.data.model

enum class HabitType {
    STEP_COUNT,
    WORKOUT_MINUTES,
    MEDITATION_MINUTES,
    POMODORO,
    APP_USAGE,
    MANUAL_CHECK
}

data class Habit(
    val id: String,
    val title: String,
    val description: String,
    val type: HabitType,
    val targetValue: Int,
    var currentValue: Int = 0,
    val unit: String = "",
    val targetPackageName: String? = null,
    val iconName: String = "ic_check",
    val colorHex: String = "#10B981",
    var isCompletedToday: Boolean = false,
    val isBuiltIn: Boolean = false
) {
    val progressFraction: Float
        get() = if (targetValue <= 0) 1.0f else (currentValue.toFloat() / targetValue.toFloat()).coerceIn(0f, 1f)

    val progressPercent: Int
        get() = (progressFraction * 100).toInt()

    fun getIconDrawableRes(): Int {
        return when (iconName) {
            "ic_directions_walk" -> com.habitsfirst.app.R.drawable.ic_directions_walk
            "ic_fitness_center" -> com.habitsfirst.app.R.drawable.ic_fitness_center
            "ic_self_improvement" -> com.habitsfirst.app.R.drawable.ic_self_improvement
            "ic_menu_book" -> com.habitsfirst.app.R.drawable.ic_menu_book
            "ic_timer" -> com.habitsfirst.app.R.drawable.ic_timer
            "ic_local_drink" -> com.habitsfirst.app.R.drawable.ic_local_drink
            "ic_bedtime" -> com.habitsfirst.app.R.drawable.ic_bedtime
            "ic_edit" -> com.habitsfirst.app.R.drawable.ic_edit
            "ic_code" -> com.habitsfirst.app.R.drawable.ic_code
            "ic_favorite" -> com.habitsfirst.app.R.drawable.ic_favorite
            "ic_sunny" -> com.habitsfirst.app.R.drawable.ic_sunny
            "ic_restaurant" -> com.habitsfirst.app.R.drawable.ic_restaurant
            "ic_credit_card" -> com.habitsfirst.app.R.drawable.ic_credit_card
            "ic_shield" -> com.habitsfirst.app.R.drawable.ic_shield
            "ic_lock" -> com.habitsfirst.app.R.drawable.ic_lock
            else -> when (type) {
                HabitType.STEP_COUNT -> com.habitsfirst.app.R.drawable.ic_directions_walk
                HabitType.WORKOUT_MINUTES -> com.habitsfirst.app.R.drawable.ic_fitness_center
                HabitType.MEDITATION_MINUTES -> com.habitsfirst.app.R.drawable.ic_self_improvement
                HabitType.POMODORO -> com.habitsfirst.app.R.drawable.ic_timer
                HabitType.APP_USAGE -> com.habitsfirst.app.R.drawable.ic_menu_book
                HabitType.MANUAL_CHECK -> com.habitsfirst.app.R.drawable.ic_check
            }
        }
    }

    companion object {
        val AVAILABLE_ICONS = listOf(
            "ic_check" to "Checkmark",
            "ic_directions_walk" to "Walking",
            "ic_fitness_center" to "Workout",
            "ic_self_improvement" to "Meditation",
            "ic_menu_book" to "Reading",
            "ic_timer" to "Pomodoro",
            "ic_local_drink" to "Water",
            "ic_bedtime" to "Sleep",
            "ic_edit" to "Journal",
            "ic_code" to "Coding",
            "ic_favorite" to "Heart",
            "ic_sunny" to "Morning",
            "ic_restaurant" to "Nutrition"
        )

        val AVAILABLE_COLORS = listOf(
            "#10B981", // Emerald
            "#3B82F6", // Blue
            "#8B5CF6", // Purple
            "#F59E0B", // Amber
            "#EF4444", // Red
            "#EC4899", // Pink
            "#06B6D4", // Cyan
            "#14B8A6"  // Teal
        )

        fun getIconResByName(iconName: String): Int {
            return when (iconName) {
                "ic_directions_walk" -> com.habitsfirst.app.R.drawable.ic_directions_walk
                "ic_fitness_center" -> com.habitsfirst.app.R.drawable.ic_fitness_center
                "ic_self_improvement" -> com.habitsfirst.app.R.drawable.ic_self_improvement
                "ic_menu_book" -> com.habitsfirst.app.R.drawable.ic_menu_book
                "ic_timer" -> com.habitsfirst.app.R.drawable.ic_timer
                "ic_local_drink" -> com.habitsfirst.app.R.drawable.ic_local_drink
                "ic_bedtime" -> com.habitsfirst.app.R.drawable.ic_bedtime
                "ic_edit" -> com.habitsfirst.app.R.drawable.ic_edit
                "ic_code" -> com.habitsfirst.app.R.drawable.ic_code
                "ic_favorite" -> com.habitsfirst.app.R.drawable.ic_favorite
                "ic_sunny" -> com.habitsfirst.app.R.drawable.ic_sunny
                "ic_restaurant" -> com.habitsfirst.app.R.drawable.ic_restaurant
                else -> com.habitsfirst.app.R.drawable.ic_check
            }
        }
    }
}
