package com.habitsfirst.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import com.habitsfirst.app.data.repository.HabitRepository
import com.habitsfirst.app.ui.BlockedOverlayActivity
import com.habitsfirst.app.util.PaymentSafetyHelper

class HabitBlockerAccessibilityService : AccessibilityService() {

    companion object {
        const val ACTION_REFRESH_SCOPING = "com.habitsfirst.app.REFRESH_BLOCKER_SCOPING"

        @Volatile
        var instance: HabitBlockerAccessibilityService? = null
            private set

        fun refreshScopingIfRunning() {
            instance?.updateServiceInfoScoping()
        }
    }

    private lateinit var repository: HabitRepository
    private var lastBlockedPackage: String? = null
    private var lastBlockedTimestamp: Long = 0L

    private val scopingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateServiceInfoScoping()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        repository = HabitRepository.getInstance(applicationContext)

        val filter = IntentFilter(ACTION_REFRESH_SCOPING)
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(scopingReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(scopingReceiver, filter)
            }
        } catch (e: Exception) {
            // Safe fallback
        }

        updateServiceInfoScoping()
    }

    override fun onDestroy() {
        instance = null
        try {
            unregisterReceiver(scopingReceiver)
        } catch (e: Exception) {
            // Safe ignore
        }
        super.onDestroy()
    }

    fun updateServiceInfoScoping() {
        try {
            if (!::repository.isInitialized) {
                repository = HabitRepository.getInstance(applicationContext)
            }

            // Exclude payment, banking, and essential apps from package monitoring
            val blockedPackages = repository.getAllBlockedPackages()
                .filterNot { PaymentSafetyHelper.isExemptFromBlocking(it) }

            val info = serviceInfo ?: AccessibilityServiceInfo()
            info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            info.flags = 0
            info.notificationTimeout = 100

            if (blockedPackages.isNotEmpty() && !repository.isSnoozeActive()) {
                // Dynamically scope monitoring ONLY to blocked packages!
                // Banking & payment apps will never be monitored or inspected by Android OS.
                info.packageNames = blockedPackages.toTypedArray()
            } else {
                // Dummy package so service receives zero events when snooze is active or list is empty
                info.packageNames = arrayOf("com.habitsfirst.app.none")
            }
            serviceInfo = info
        } catch (t: Throwable) {
            // Ignore in unsupported environments
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // 1. Strict Exclusion: Ignore our own app, system UI, keyboards, launchers
        if (packageName == applicationContext.packageName) return
        if (PaymentSafetyHelper.isExemptFromBlocking(packageName)) return

        if (!::repository.isInitialized) {
            repository = HabitRepository.getInstance(applicationContext)
        }

        // 2. Strict Exclusion: If snooze / emergency payment pause is active, bypass immediately
        if (repository.isSnoozeActive()) {
            return
        }

        val (shouldBlock, rule) = repository.shouldBlockPackage(packageName)
        if (shouldBlock && rule != null) {
            val now = System.currentTimeMillis()
            // Debounce within 1 second for same package to prevent rapid relaunch loops
            if (packageName == lastBlockedPackage && (now - lastBlockedTimestamp) < 1000) {
                return
            }

            lastBlockedPackage = packageName
            lastBlockedTimestamp = now

            val overlayIntent = Intent(this, BlockedOverlayActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(BlockedOverlayActivity.EXTRA_BLOCKED_PACKAGE, packageName)
                putExtra(BlockedOverlayActivity.EXTRA_RULE_ID, rule.id)
            }
            startActivity(overlayIntent)
        }
    }

    override fun onInterrupt() {
        // Accessibility service interrupted
    }
}
