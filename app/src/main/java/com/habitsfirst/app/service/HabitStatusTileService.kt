package com.habitsfirst.app.service

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.habitsfirst.app.R
import com.habitsfirst.app.data.repository.HabitRepository
import com.habitsfirst.app.ui.MainActivity

class HabitStatusTileService : TileService() {

    companion object {
        fun requestTileUpdate(context: Context) {
            try {
                requestListeningState(
                    context.applicationContext,
                    ComponentName(context.applicationContext, HabitStatusTileService::class.java)
                )
            } catch (t: Throwable) {
                // Ignore if not supported on older API, headless environment or platform issue
            }
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val repo = HabitRepository.getInstance(this)
        val (completed, total) = repo.getHabitsCompletionStatus()
        val isUnlocked = total > 0 && completed >= total

        tile.label = "Habits First"

        if (isUnlocked) {
            tile.state = Tile.STATE_ACTIVE
            tile.icon = Icon.createWithResource(this, R.drawable.ic_check)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Apps Unlocked 🔓"
            }
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.icon = Icon.createWithResource(this, R.drawable.ic_lock)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "$completed/$total Done 🔒"
            }
        }

        tile.updateTile()
    }
}
