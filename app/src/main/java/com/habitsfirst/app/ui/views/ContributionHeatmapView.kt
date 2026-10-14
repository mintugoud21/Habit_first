package com.habitsfirst.app.ui.views

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.habitsfirst.app.R
import com.habitsfirst.app.data.model.DailyCompletion
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ContributionHeatmapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f
    }

    private var completionsMap: Map<String, DailyCompletion> = emptyMap()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val numWeeks = 16 // Shows past ~4 months horizontally
    private val daysPerWeek = 7

    private val cellSize = 36f
    private val cellSpacing = 8f
    private val cornerRadius = 8f

    fun setData(completions: List<DailyCompletion>) {
        this.completionsMap = completions.associateBy { it.dateStr }
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val totalWidth = (numWeeks * (cellSize + cellSpacing) + 60f).toInt()
        val totalHeight = (daysPerWeek * (cellSize + cellSpacing) + 50f).toInt()
        setMeasuredDimension(
            resolveSize(totalWidth, widthMeasureSpec),
            resolveSize(totalHeight, heightMeasureSpec)
        )
    }

    private fun isDarkMode(): Boolean {
        val mode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return mode == Configuration.UI_MODE_NIGHT_YES
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        textPaint.color = ContextCompat.getColor(context, R.color.text_secondary)

        val calendar = Calendar.getInstance()
        val today = Date()

        // Days of week labels (M, W, F)
        val dayLabels = listOf("M", "", "W", "", "F", "", "S")
        for (i in 0 until daysPerWeek) {
            if (dayLabels[i].isNotEmpty()) {
                val y = i * (cellSize + cellSpacing) + cellSize * 0.8f + 30f
                canvas.drawText(dayLabels[i], 10f, y, textPaint)
            }
        }

        // Align to the end of the week for today
        val startX = 50f
        val startY = 30f

        val cal = Calendar.getInstance()
        cal.time = today
        // Day of week with Monday = 0, Tuesday = 1, ... Sunday = 6
        val todayDow = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7

        // Draw cells ending at today's day of week in the current week
        for (week in 0 until numWeeks) {
            val weeksAgo = (numWeeks - 1) - week
            for (day in 0 until daysPerWeek) {
                val daysDiff = (weeksAgo * 7) + (todayDow - day)
                val isFuture = daysDiff < 0

                calendar.time = today
                calendar.add(Calendar.DAY_OF_YEAR, -daysDiff)
                val dateStr = dateFormat.format(calendar.time)

                val completion = completionsMap[dateStr]
                val ratio = if (isFuture) 0f else (completion?.completionRatio ?: 0f)

                cellPaint.color = getColorForRatio(ratio, isFuture)

                val left = startX + week * (cellSize + cellSpacing)
                val top = startY + day * (cellSize + cellSpacing)
                val rect = RectF(left, top, left + cellSize, top + cellSize)
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, cellPaint)
            }
        }
    }

    private fun getColorForRatio(ratio: Float, isFuture: Boolean): Int {
        val dark = isDarkMode()
        if (isFuture) {
            return if (dark) Color.parseColor("#16181D") else Color.parseColor("#F1F5F9")
        }
        return if (dark) {
            when {
                ratio <= 0.05f -> Color.parseColor("#1F2430")
                ratio < 0.4f -> Color.parseColor("#065F46")
                ratio < 0.75f -> Color.parseColor("#059669")
                ratio < 1.0f -> Color.parseColor("#10B981")
                else -> Color.parseColor("#34D399")
            }
        } else {
            when {
                ratio <= 0.05f -> Color.parseColor("#E2E8F0")
                ratio < 0.4f -> Color.parseColor("#A7F3D0")
                ratio < 0.75f -> Color.parseColor("#34D399")
                ratio < 1.0f -> Color.parseColor("#10B981")
                else -> Color.parseColor("#059669")
            }
        }
    }
}
