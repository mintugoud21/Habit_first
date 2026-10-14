package com.habitsfirst.app.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.habitsfirst.app.R
import com.habitsfirst.app.data.model.Habit
import com.habitsfirst.app.data.model.HabitType

class HabitAdapter(
    private var habits: List<Habit>,
    private val onHabitToggle: (Habit, Boolean) -> Unit,
    private val onHabitAction: (Habit) -> Unit
) : RecyclerView.Adapter<HabitAdapter.HabitViewHolder>() {

    fun updateData(newHabits: List<Habit>) {
        this.habits = newHabits
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_habit_card, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        holder.bind(habits[position])
    }

    override fun getItemCount(): Int = habits.size

    inner class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.card_habit)
        private val iconContainer: View? = itemView.findViewById(R.id.layout_habit_icon_container)
        private val ivIcon: ImageView = itemView.findViewById(R.id.iv_habit_icon)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_habit_title)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.tv_habit_subtitle)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.pb_habit_progress)
        private val tvProgressLabel: TextView = itemView.findViewById(R.id.tv_progress_label)
        private val cbComplete: CheckBox = itemView.findViewById(R.id.cb_habit_done)
        private val btnAction: MaterialButton = itemView.findViewById(R.id.btn_habit_action)

        fun bind(habit: Habit) {
            tvTitle.text = habit.title
            tvSubtitle.text = habit.description

            val color = try {
                Color.parseColor(habit.colorHex)
            } catch (e: Exception) {
                Color.parseColor("#10B981")
            }

            // Set habit icon and tint
            ivIcon.setImageResource(habit.getIconDrawableRes())
            ivIcon.setColorFilter(color)

            // Dynamic badge background with matching tint
            val bgAlphaColor = Color.argb(
                35,
                Color.red(color),
                Color.green(color),
                Color.blue(color)
            )
            val bgDrawable = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(bgAlphaColor)
                setStroke(2, color)
            }
            iconContainer?.background = bgDrawable

            progressBar.max = habit.targetValue.coerceAtLeast(1)
            progressBar.progress = habit.currentValue.coerceAtMost(habit.targetValue)
            tvProgressLabel.text = "${habit.currentValue} / ${habit.targetValue} ${habit.unit}"

            cbComplete.setOnCheckedChangeListener(null)
            cbComplete.isChecked = habit.isCompletedToday
            cbComplete.setOnClickListener {
                onHabitToggle(habit, cbComplete.isChecked)
            }
            cardView.setOnClickListener {
                val newState = !cbComplete.isChecked
                cbComplete.isChecked = newState
                onHabitToggle(habit, newState)
            }

            when (habit.type) {
                HabitType.STEP_COUNT -> {
                    btnAction.visibility = View.VISIBLE
                    btnAction.text = "+1,000 Steps"
                    btnAction.setOnClickListener { onHabitAction(habit) }
                }
                HabitType.WORKOUT_MINUTES, HabitType.MEDITATION_MINUTES, HabitType.POMODORO -> {
                    btnAction.visibility = View.VISIBLE
                    btnAction.text = "Start Timer"
                    btnAction.setOnClickListener { onHabitAction(habit) }
                }
                HabitType.APP_USAGE -> {
                    btnAction.visibility = View.VISIBLE
                    btnAction.text = "Open App"
                    btnAction.setOnClickListener { onHabitAction(habit) }
                }
                HabitType.MANUAL_CHECK -> {
                    btnAction.visibility = View.GONE
                }
            }

            if (habit.isCompletedToday) {
                cardView.strokeColor = androidx.core.content.ContextCompat.getColor(cardView.context, R.color.primary_emerald)
                tvTitle.alpha = 0.9f
            } else {
                cardView.strokeColor = androidx.core.content.ContextCompat.getColor(cardView.context, R.color.card_stroke)
                tvTitle.alpha = 1.0f
            }
        }
    }
}
