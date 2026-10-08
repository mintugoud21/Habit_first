package com.habitsfirst.app.data.model

data class DailyCompletion(
    val dateStr: String, // "YYYY-MM-DD"
    val completedCount: Int,
    val totalCount: Int,
    val isGoalMet: Boolean
) {
    val completionRatio: Float
        get() = if (totalCount > 0) (completedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f) else 0f
}
