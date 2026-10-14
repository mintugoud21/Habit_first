package com.habitsfirst.app

import com.habitsfirst.app.data.model.DailyCompletion
import com.habitsfirst.app.data.model.Habit
import com.habitsfirst.app.data.model.HabitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitModelAndStreakTest {

    @Test
    fun testHabitCompletionStatus() {
        val habit = Habit(
            id = "test_steps",
            title = "Walk 10k Steps",
            description = "Movement",
            type = HabitType.STEP_COUNT,
            targetValue = 10000,
            currentValue = 5000,
            unit = "steps",
            isCompletedToday = false
        )

        assertFalse(habit.isCompletedToday)

        val completedHabit = habit.copy(
            currentValue = 10000,
            isCompletedToday = true
        )
        assertTrue(completedHabit.isCompletedToday)
        assertEquals(10000, completedHabit.currentValue)
    }

    @Test
    fun testStreakCalculationConsecutiveDays() {
        val history = listOf(
            DailyCompletion("2026-10-10", completedCount = 4, totalCount = 6, isGoalMet = true),
            DailyCompletion("2026-10-11", completedCount = 5, totalCount = 6, isGoalMet = true),
            DailyCompletion("2026-10-12", completedCount = 6, totalCount = 6, isGoalMet = true),
            DailyCompletion("2026-10-13", completedCount = 3, totalCount = 6, isGoalMet = true),
            // Today is 2026-10-14 (in progress)
            DailyCompletion("2026-10-14", completedCount = 1, totalCount = 6, isGoalMet = false)
        )

        val todayStr = "2026-10-14"
        var streak = 0
        for (i in history.indices.reversed()) {
            val item = history[i]
            if (item.dateStr == todayStr) {
                continue // Today does not break streak
            }
            if (item.isGoalMet) {
                streak++
            } else {
                break
            }
        }

        assertEquals(4, streak)
    }

    @Test
    fun testBrokenStreakStopsCounting() {
        val history = listOf(
            DailyCompletion("2026-10-10", completedCount = 6, totalCount = 6, isGoalMet = true),
            DailyCompletion("2026-10-11", completedCount = 1, totalCount = 6, isGoalMet = false), // Missed day
            DailyCompletion("2026-10-12", completedCount = 6, totalCount = 6, isGoalMet = true),
            DailyCompletion("2026-10-13", completedCount = 5, totalCount = 6, isGoalMet = true),
            DailyCompletion("2026-10-14", completedCount = 0, totalCount = 6, isGoalMet = false)
        )

        val todayStr = "2026-10-14"
        var streak = 0
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

        assertEquals(2, streak)
    }

    @Test
    fun testCompletionRatioCalculation() {
        val completion = DailyCompletion(
            dateStr = "2026-10-13",
            completedCount = 3,
            totalCount = 6,
            isGoalMet = true
        )

        assertEquals(0.5f, completion.completionRatio, 0.001f)
    }

    @Test
    fun testZeroTotalHabitsRatioSafe() {
        val completion = DailyCompletion(
            dateStr = "2026-10-13",
            completedCount = 0,
            totalCount = 0,
            isGoalMet = false
        )

        assertEquals(0f, completion.completionRatio, 0.001f)
    }
}
