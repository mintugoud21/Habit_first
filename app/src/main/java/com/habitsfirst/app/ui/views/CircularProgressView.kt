package com.habitsfirst.app.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.habitsfirst.app.R

class CircularProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 18f
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 18f
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 48f
        isFakeBoldText = true
    }

    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 24f
    }

    private var progressFraction: Float = 0f
    private var centerText: String = "0%"
    private var subText: String = "done"
    private val arcBounds = RectF()
    private var customProgressColor: Int? = null

    init {
        updateColors()
    }

    private fun updateColors() {
        bgPaint.color = ContextCompat.getColor(context, R.color.card_stroke)
        progressPaint.color = customProgressColor ?: ContextCompat.getColor(context, R.color.primary_emerald)
        textPaint.color = ContextCompat.getColor(context, R.color.text_primary)
        subTextPaint.color = ContextCompat.getColor(context, R.color.text_secondary)
    }

    fun setProgress(fraction: Float, customColor: Int? = null) {
        this.progressFraction = fraction.coerceIn(0f, 1f)
        this.customProgressColor = customColor
        updateColors()
        this.centerText = "${(progressFraction * 100).toInt()}%"
        invalidate()
    }

    fun setCenterLabel(main: String, sub: String) {
        this.centerText = main
        this.subText = sub
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val stroke = 18f
        val padding = stroke / 2f + 8f
        arcBounds.set(padding, padding, w - padding, h - padding)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        updateColors()

        val startAngle = -90f
        val sweepAngle = 360f * progressFraction

        // Background track circle
        canvas.drawArc(arcBounds, 0f, 360f, false, bgPaint)

        // Progress arc
        if (progressFraction > 0f) {
            canvas.drawArc(arcBounds, startAngle, sweepAngle, false, progressPaint)
        }

        // Center typography
        val centerX = width / 2f
        val centerY = height / 2f

        val mainY = centerY - (textPaint.descent() + textPaint.ascent()) / 2f - 10f
        canvas.drawText(centerText, centerX, mainY, textPaint)

        val subY = centerY + 30f
        canvas.drawText(subText, centerX, subY, subTextPaint)
    }
}
