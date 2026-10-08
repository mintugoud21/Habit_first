package com.habitsfirst.app.data.model

import java.util.Calendar

enum class RuleConditionType {
    ALL_HABITS,
    ANY_HABIT,
    AT_LEAST_N
}

data class BlockRule(
    val id: String,
    val name: String,
    val blockedPackages: List<String> = emptyList(),
    val conditionType: RuleConditionType = RuleConditionType.ALL_HABITS,
    val requiredHabitIds: List<String> = emptyList(),
    val requiredCount: Int = 1,
    var isEnabled: Boolean = true,
    val strictMode: Boolean = false,
    val startTime: String? = null, // e.g. "09:00"
    val endTime: String? = null,   // e.g. "17:00"
    val activeDays: List<Int> = listOf(
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.WEDNESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY,
        Calendar.SATURDAY,
        Calendar.SUNDAY
    )
) {
    fun isScheduleActiveNow(): Boolean {
        if (!isEnabled) return false
        val now = Calendar.getInstance()
        val currentDay = now.get(Calendar.DAY_OF_WEEK)
        if (!activeDays.contains(currentDay)) return false

        if (startTime.isNullOrBlank() || endTime.isNullOrBlank()) {
            return true // All day
        }

        try {
            val startParts = startTime.split(":")
            val endParts = endTime.split(":")
            val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            val startMinutes = startParts[0].toInt() * 60 + startParts[1].toInt()
            val endMinutes = endParts[0].toInt() * 60 + endParts[1].toInt()

            return if (startMinutes <= endMinutes) {
                currentMinutes in startMinutes..endMinutes
            } else {
                // Spans midnight
                currentMinutes >= startMinutes || currentMinutes <= endMinutes
            }
        } catch (e: Exception) {
            return true
        }
    }

    fun isRequirementFulfilled(habitsMap: Map<String, Habit>): Boolean {
        if (requiredHabitIds.isEmpty()) return true

        val habitsToCheck = requiredHabitIds.mapNotNull { habitsMap[it] }
        val completedCount = habitsToCheck.count { it.isCompletedToday }

        return when (conditionType) {
            RuleConditionType.ALL_HABITS -> completedCount == habitsToCheck.size
            RuleConditionType.ANY_HABIT -> completedCount > 0
            RuleConditionType.AT_LEAST_N -> completedCount >= requiredCount.coerceAtMost(habitsToCheck.size)
        }
    }
}
