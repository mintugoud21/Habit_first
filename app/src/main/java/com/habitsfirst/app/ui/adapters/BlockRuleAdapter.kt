package com.habitsfirst.app.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.habitsfirst.app.R
import com.habitsfirst.app.data.model.BlockRule
import com.habitsfirst.app.data.model.RuleConditionType

class BlockRuleAdapter(
    private var rules: List<BlockRule>,
    private val onToggleEnabled: (BlockRule, Boolean) -> Unit,
    private val onManageApps: (BlockRule) -> Unit
) : RecyclerView.Adapter<BlockRuleAdapter.RuleViewHolder>() {

    fun updateData(newRules: List<BlockRule>) {
        this.rules = newRules
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RuleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_block_rule_card, parent, false)
        return RuleViewHolder(view)
    }

    override fun onBindViewHolder(holder: RuleViewHolder, position: Int) {
        holder.bind(rules[position])
    }

    override fun getItemCount(): Int = rules.size

    inner class RuleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_rule_name)
        private val tvCondition: TextView = itemView.findViewById(R.id.tv_rule_condition)
        private val tvAppsCount: TextView = itemView.findViewById(R.id.tv_blocked_apps_count)
        private val swEnabled: SwitchCompat = itemView.findViewById(R.id.sw_rule_enabled)
        private val btnManageApps: MaterialButton = itemView.findViewById(R.id.btn_manage_apps)

        fun bind(rule: BlockRule) {
            tvName.text = rule.name

            val conditionDesc = when (rule.conditionType) {
                RuleConditionType.ALL_HABITS -> "Requires ALL ${rule.requiredHabitIds.size} habits"
                RuleConditionType.ANY_HABIT -> "Requires ANY 1 habit"
                RuleConditionType.AT_LEAST_N -> "Requires at least ${rule.requiredCount} of ${rule.requiredHabitIds.size} habits"
            }
            tvCondition.text = conditionDesc

            tvAppsCount.text = "${rule.blockedPackages.size} apps blocked"

            swEnabled.setOnCheckedChangeListener(null)
            swEnabled.isChecked = rule.isEnabled
            swEnabled.setOnClickListener {
                onToggleEnabled(rule, swEnabled.isChecked)
            }

            btnManageApps.setOnClickListener {
                onManageApps(rule)
            }

            itemView.setOnClickListener {
                onManageApps(rule)
            }
        }
    }
}
