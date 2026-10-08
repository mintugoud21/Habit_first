package com.habitsfirst.app.ui.adapters

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.habitsfirst.app.R
import com.habitsfirst.app.data.model.Habit
import java.util.Locale

class SettingsHabitAdapter(
    private var habits: List<Habit>,
    private val onEditHabit: (Habit) -> Unit,
    private val onDeleteHabit: (Habit) -> Unit
) : RecyclerView.Adapter<SettingsHabitAdapter.SettingsHabitViewHolder>() {

    fun updateData(newHabits: List<Habit>) {
        this.habits = newHabits
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingsHabitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_settings_habit, parent, false)
        return SettingsHabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: SettingsHabitViewHolder, position: Int) {
        holder.bind(habits[position])
    }

    override fun getItemCount(): Int = habits.size

    inner class SettingsHabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconContainer: FrameLayout = itemView.findViewById(R.id.settings_habit_icon_container)
        private val ivIcon: ImageView = itemView.findViewById(R.id.settings_habit_icon)
        private val tvTitle: TextView = itemView.findViewById(R.id.settings_habit_title)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.settings_habit_subtitle)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btn_edit_habit)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete_habit)

        fun bind(habit: Habit) {
            tvTitle.text = habit.title
            val typeLabel = habit.type.name.replace('_', ' ').lowercase(Locale.ROOT)
            tvSubtitle.text = "Goal: ${habit.targetValue} ${habit.unit} • $typeLabel"

            val color = try {
                Color.parseColor(habit.colorHex)
            } catch (e: Exception) {
                Color.parseColor("#10B981")
            }

            ivIcon.setImageResource(habit.getIconDrawableRes())
            ivIcon.setColorFilter(color)

            val bgAlphaColor = Color.argb(
                35,
                Color.red(color),
                Color.green(color),
                Color.blue(color)
            )
            val bgDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(bgAlphaColor)
                setStroke(2, color)
            }
            iconContainer.background = bgDrawable

            btnEdit.setOnClickListener { onEditHabit(habit) }
            btnDelete.setOnClickListener { onDeleteHabit(habit) }
        }
    }
}
