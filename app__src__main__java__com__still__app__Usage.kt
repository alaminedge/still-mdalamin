package com.still.app

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.util.Calendar

object Usage {
    private var cache: Map<String, Long> = emptyMap()
    private var at = 0L

    fun hasAccess(c: Context): Boolean {
        val ops = c.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        @Suppress("DEPRECATION")
        val mode = ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Foreground milliseconds per package since midnight. */
    @Synchronized
    fun today(c: Context, maxAge: Long = 15_000): Map<String, Long> {
        val now = System.currentTimeMillis()
        if (now - at < maxAge) return cache
        cache = compute(c, now)
        at = now
        return cache
    }

    fun minutesToday(c: Context, pkg: String): Int = ((today(c)[pkg] ?: 0L) / 60_000L).toInt()

    private fun compute(c: Context, now: Long): Map<String, Long> {
        if (!hasAccess(c)) return emptyMap()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val usm = c.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val ev = usm.queryEvents(cal.timeInMillis, now)
        val e = UsageEvents.Event()
        val start = HashMap<String, Long>()
        val total = HashMap<String, Long>()
        while (ev.hasNextEvent()) {
            ev.getNextEvent(e)
            val p = e.packageName ?: continue
            when (e.eventType) {
                1 -> start[p] = e.timeStamp                       // activity resumed
                2 -> start.remove(p)?.let {                       // activity paused
                    total[p] = (total[p] ?: 0L) + (e.timeStamp - it)
                }
            }
        }
        // Only the most recently resumed app can still be on screen.
        start.maxByOrNull { it.value }?.let { (p, s) -> total[p] = (total[p] ?: 0L) + (now - s) }
        return total
    }
}
