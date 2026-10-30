package com.still.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent

class BlockerService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var current = ""
    private var lastBlock = 0L

    private val ticker = object : Runnable {
        override fun run() {
            if (current.isNotEmpty()) enforce(current)
            handler.postDelayed(this, 20_000)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Store.init(this)
        handler.removeCallbacks(ticker)
        handler.postDelayed(ticker, 20_000)
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        super.onDestroy()
    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        if (e == null || e.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = e.packageName?.toString() ?: return
        if (pkg == packageName) return
        Store.init(this)

        if (Store.hardcoreOn() && pkg in GUARDED) {
            if (showsStill()) { performGlobalAction(GLOBAL_ACTION_HOME); return }
            // Settings pages often fill in a moment later, so look once more.
            handler.postDelayed({
                if (Store.hardcoreOn() && showsStill()) performGlobalAction(GLOBAL_ACTION_HOME)
            }, 500)
        }

        if (pkg != "com.android.systemui" && !pkg.contains("inputmethod")) current = pkg
        enforce(pkg)
    }

    private fun enforce(pkg: String) {
        val reason = Store.blockReason(this, pkg) ?: return
        val now = SystemClock.elapsedRealtime()
        if (now - lastBlock < 1200) return
        lastBlock = now
        performGlobalAction(GLOBAL_ACTION_HOME)
        startActivity(
            Intent(this, BlockActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                .putExtra("pkg", pkg)
                .putExtra("reason", reason)
        )
    }

    private fun showsStill(): Boolean {
        val root = rootInActiveWindow ?: return false
        return try {
            root.findAccessibilityNodeInfosByText("STILL").any { it.text?.toString() == "STILL" }
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        private val GUARDED = setOf(
            "com.android.settings",
            "com.google.android.packageinstaller",
            "com.android.packageinstaller",
            "com.samsung.android.packageinstaller",
            "com.miui.packageinstaller",
            "com.google.android.permissioncontroller",
            "com.miui.securitycenter"
        )
    }
}
