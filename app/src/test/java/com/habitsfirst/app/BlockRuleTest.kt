package com.habitsfirst.app

import com.habitsfirst.app.data.model.BlockRule
import com.habitsfirst.app.data.model.Habit
import com.habitsfirst.app.data.model.HabitType
import com.habitsfirst.app.data.model.RuleConditionType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class BlockRuleTest {

    private fun createHabit(id: String, completed: Boolean): Habit {
        return Habit(
            id = id,
            title = "Test Habit $id",
            description = "Description",
            type = HabitType.MANUAL_CHECK,
            targetValue = 1,
            currentValue = if (completed) 1 else 0,
            unit = "done",
            isCompletedToday = completed
        )
    }

    @Test
    fun testAllHabitsCondition_allCompleted_returnsTrue() {
        val h1 = createHabit("h1", true)
        val h2 = createHabit("h2", true)
        val habitsMap = mapOf("h1" to h1, "h2" to h2)

        val rule = BlockRule(
            id = "rule1",
            name = "Test Rule",
            conditionType = RuleConditionType.ALL_HABITS,
            requiredHabitIds = listOf("h1", "h2")
        )

        assertTrue(rule.isRequirementFulfilled(habitsMap))
    }

    @Test
    fun testAllHabitsCondition_partialCompleted_returnsFalse() {
        val h1 = createHabit("h1", true)
        val h2 = createHabit("h2", false)
        val habitsMap = mapOf("h1" to h1, "h2" to h2)

        val rule = BlockRule(
            id = "rule1",
            name = "Test Rule",
            conditionType = RuleConditionType.ALL_HABITS,
            requiredHabitIds = listOf("h1", "h2")
        )

        assertFalse(rule.isRequirementFulfilled(habitsMap))
    }

    @Test
    fun testAnyHabitCondition_oneCompleted_returnsTrue() {
        val h1 = createHabit("h1", false)
        val h2 = createHabit("h2", true)
        val habitsMap = mapOf("h1" to h1, "h2" to h2)

        val rule = BlockRule(
            id = "rule1",
            name = "Test Rule",
            conditionType = RuleConditionType.ANY_HABIT,
            requiredHabitIds = listOf("h1", "h2")
        )

        assertTrue(rule.isRequirementFulfilled(habitsMap))
    }

    @Test
    fun testAnyHabitCondition_noneCompleted_returnsFalse() {
        val h1 = createHabit("h1", false)
        val h2 = createHabit("h2", false)
        val habitsMap = mapOf("h1" to h1, "h2" to h2)

        val rule = BlockRule(
            id = "rule1",
            name = "Test Rule",
            conditionType = RuleConditionType.ANY_HABIT,
            requiredHabitIds = listOf("h1", "h2")
        )

        assertFalse(rule.isRequirementFulfilled(habitsMap))
    }

    @Test
    fun testAtLeastNCondition_thresholdMet_returnsTrue() {
        val h1 = createHabit("h1", true)
        val h2 = createHabit("h2", true)
        val h3 = createHabit("h3", false)
        val habitsMap = mapOf("h1" to h1, "h2" to h2, "h3" to h3)

        val rule = BlockRule(
            id = "rule1",
            name = "Test Rule",
            conditionType = RuleConditionType.AT_LEAST_N,
            requiredHabitIds = listOf("h1", "h2", "h3"),
            requiredCount = 2
        )

        assertTrue(rule.isRequirementFulfilled(habitsMap))
    }

    @Test
    fun testAtLeastNCondition_thresholdNotMet_returnsFalse() {
        val h1 = createHabit("h1", true)
        val h2 = createHabit("h2", false)
        val h3 = createHabit("h3", false)
        val habitsMap = mapOf("h1" to h1, "h2" to h2, "h3" to h3)

        val rule = BlockRule(
            id = "rule1",
            name = "Test Rule",
            conditionType = RuleConditionType.AT_LEAST_N,
            requiredHabitIds = listOf("h1", "h2", "h3"),
            requiredCount = 2
        )

        assertFalse(rule.isRequirementFulfilled(habitsMap))
    }

    @Test
    fun testEmptyRequiredHabits_returnsTrue() {
        val rule = BlockRule(
            id = "rule1",
            name = "Test Rule",
            conditionType = RuleConditionType.ALL_HABITS,
            requiredHabitIds = emptyList()
        )

        assertTrue(rule.isRequirementFulfilled(emptyMap()))
    }

    @Test
    fun testSchedule_disabled_returnsFalse() {
        val rule = BlockRule(
            id = "rule1",
            name = "Test Rule",
            isEnabled = false
        )

        assertFalse(rule.isScheduleActiveNow())
    }

    @Test
    fun testSchedule_allDay_returnsTrue() {
        val rule = BlockRule(
            id = "rule1",
            name = "Test Rule",
            isEnabled = true,
            startTime = null,
            endTime = null
        )

        assertTrue(rule.isScheduleActiveNow())
    }
}
