package com.still.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap

data class Schedule(
    val id: Long,
    val name: String,
    val days: Int,   // bit 0 = Sunday ... bit 6 = Saturday
    val start: Int,  // minutes after midnight
    val end: Int,
    val on: Boolean
)

object Store {
    private const val DAY = 86_400_000L
    private var sp: SharedPreferences? = null

    /** 0 = off, 1 = focus hours, 2 = always */
    val modes = mutableStateMapOf<String, Int>()
    /** daily limit in minutes (absent = none) */
    val limits = mutableStateMapOf<String, Int>()
    /** launcher: 0 = in drawer, 1 = on home, 2 = hidden */
    val home = mutableStateMapOf<String, Int>()
    val schedules = mutableStateListOf<Schedule>()
    val bgApps = mutableStateListOf<String>()
    var hardcoreUntil by mutableLongStateOf(0L)
        private set
    /** total phone limit per day, in minutes (0 = none) */
    var totalLimit by mutableIntStateOf(0)
        private set

    private var emDay = 0L
    private var emUsed = 0
    val tempUnlock = ConcurrentHashMap<String, Long>()

    @Synchronized
    fun init(c: Context) {
        if (sp != null) return
        val p = c.applicationContext.getSharedPreferences("still", Context.MODE_PRIVATE)
        sp = p
        readMap(p.getString("modes", "{}") ?: "{}", modes)
        readMap(p.getString("limits", "{}") ?: "{}", limits)
        readMap(p.getString("home", "{}") ?: "{}", home)
        val arr = JSONArray(p.getString("schedules", "[]") ?: "[]")
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            schedules.add(Schedule(o.getLong("id"), o.getString("n"), o.getInt("d"), o.getInt("s"), o.getInt("e"), o.getBoolean("on")))
        }
        val b = JSONArray(p.getString("bg", "[]") ?: "[]")
        for (i in 0 until b.length()) bgApps.add(b.getString(i))
        hardcoreUntil = p.getLong("hc", 0L)
        totalLimit = p.getInt("tl", 0)
        emDay = p.getLong("emDay", 0L)
        emUsed = p.getInt("emUsed", 0)
    }

    private fun readMap(s: String, into: MutableMap<String, Int>) {
        val o = JSONObject(s)
        o.keys().forEach { into[it] = o.getInt(it) }
    }

    private fun writeMap(m: Map<String, Int>): String {
        val o = JSONObject()
        m.forEach { (k, v) -> o.put(k, v) }
        return o.toString()
    }

    private fun save() {
        val arr = JSONArray()
        schedules.forEach {
            arr.put(JSONObject().put("id", it.id).put("n", it.name).put("d", it.days).put("s", it.start).put("e", it.end).put("on", it.on))
        }
        val b = JSONArray()
        bgApps.forEach { b.put(it) }
        sp?.edit()
            ?.putString("modes", writeMap(modes))
            ?.putString("limits", writeMap(limits))
            ?.putString("home", writeMap(home))
            ?.putString("schedules", arr.toString())
            ?.putString("bg", b.toString())
            ?.putLong("hc", hardcoreUntil)
            ?.putInt("tl", totalLimit)
            ?.putLong("emDay", emDay)
            ?.putInt("emUsed", emUsed)
            ?.apply()
    }

    // ---------- Hardcore ----------
    fun hardcoreOn() = hardcoreUntil > System.currentTimeMillis()

    fun hardcoreLeftMs() = (hardcoreUntil - System.currentTimeMillis()).coerceAtLeast(0L)

    fun setTotalLimit(mins: Int): Boolean {
        val loosening = totalLimit > 0 && (mins == 0 || mins > totalLimit)
        if (loosening && hardcoreOn()) return false
        totalLimit = mins
        save()
        return true
    }

    fun startHardcore(days: Int) {
        hardcoreUntil = maxOf(hardcoreUntil, System.currentTimeMillis() + days * DAY)
        save()
    }

    // ---------- Rules (return false when Hardcore forbids loosening) ----------
    fun setMode(pkg: String, m: Int): Boolean {
        val cur = modes[pkg] ?: 0
        if (m < cur && hardcoreOn()) return false
        if (m == 0) modes.remove(pkg) else modes[pkg] = m
        save()
        return true
    }

    fun setLimit(pkg: String, mins: Int): Boolean {
        val cur = limits[pkg] ?: 0
        val loosening = cur > 0 && (mins == 0 || mins > cur)
        if (loosening && hardcoreOn()) return false
        if (mins == 0) limits.remove(pkg) else limits[pkg] = mins
        save()
        return true
    }

    fun setHome(pkg: String, m: Int): Boolean {
        val cur = home[pkg] ?: 0
        if (cur == 2 && m != 2 && hardcoreOn()) return false
        if (m == 0) home.remove(pkg) else home[pkg] = m
        save()
        return true
    }

    fun addSchedule(name: String, days: Int, start: Int, end: Int) {
        schedules.add(Schedule(System.currentTimeMillis(), name, days, start, end, true))
        save()
    }

    fun updateSchedule(s: Schedule): Boolean {
        val i = schedules.indexOfFirst { it.id == s.id }
        if (i < 0) return false
        val old = schedules[i]
        if (hardcoreOn() && !(s.on && !old.on && s.copy(on = false) == old)) return false
        schedules[i] = s
        save()
        return true
    }

    fun removeSchedule(id: Long): Boolean {
        if (hardcoreOn()) return false
        schedules.removeAll { it.id == id }
        save()
        return true
    }

    fun toggleBg(pkg: String) {
        if (!bgApps.remove(pkg)) bgApps.add(pkg)
        save()
    }

    // ---------- Emergency unlock ----------
    private fun dayKey(): Long {
        val c = Calendar.getInstance()
        return c.get(Calendar.YEAR) * 1000L + c.get(Calendar.DAY_OF_YEAR)
    }

    fun maxEmergency() = if (hardcoreOn()) 1 else 3
    fun emergencyWait() = if (hardcoreOn()) 180 else 60

    fun emergencyLeft(): Int {
        if (emDay != dayKey()) return maxEmergency()
        return (maxEmergency() - emUsed).coerceAtLeast(0)
    }

    fun useEmergency(pkg: String, minutes: Int) {
        val d = dayKey()
        if (emDay != d) { emDay = d; emUsed = 0 }
        emUsed++
        tempUnlock[pkg] = System.currentTimeMillis() + minutes * 60_000L
        save()
    }

    // ---------- Decisions ----------
    private fun has(d: Int, i: Int) = ((d shr i) and 1) == 1

    private fun activeAt(s: Schedule, dow: Int, prev: Int, m: Int): Boolean = s.on && when {
        s.start < s.end -> has(s.days, dow) && m >= s.start && m < s.end
        s.start > s.end -> (has(s.days, dow) && m >= s.start) || (has(s.days, prev) && m < s.end)
        else -> false
    }

    fun scheduleActive(): Boolean {
        val c = Calendar.getInstance()
        val dow = c.get(Calendar.DAY_OF_WEEK) - 1
        val prev = (dow + 6) % 7
        val m = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
        return schedules.any { activeAt(it, dow, prev, m) }
    }

    data class FocusNow(val name: String, val endMin: Int, val minsLeft: Int)

    /** The focus period running right now (the one that ends last, if several overlap). */
    fun activeFocus(): FocusNow? {
        val c = Calendar.getInstance()
        val dow = c.get(Calendar.DAY_OF_WEEK) - 1
        val prev = (dow + 6) % 7
        val m = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
        return schedules
            .filter { activeAt(it, dow, prev, m) }
            .map { s -> FocusNow(s.name, s.end, if (s.end > m) s.end - m else 1440 - m + s.end) }
            .maxByOrNull { it.minsLeft }
    }

    /** The next focus period to start, and how many minutes until then. */
    fun nextFocus(): Pair<Schedule, Int>? {
        val c = Calendar.getInstance()
        val dow = c.get(Calendar.DAY_OF_WEEK) - 1
        val m = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
        var best: Pair<Schedule, Int>? = null
        for (s in schedules) {
            if (!s.on) continue
            for (off in 0..7) {
                if (!has(s.days, (dow + off) % 7)) continue
                val mins = off * 1440 + s.start - m
                if (mins <= 0) continue
                val b = best
                if (b == null || mins < b.second) best = s to mins
                break
            }
        }
        return best
    }

    /** Same rules as [blockReason], but pure: pass in today's usage so no I/O happens. */
    fun blockedNow(pkg: String, usedMs: Long): Boolean {
        val u = tempUnlock[pkg]
        if (u != null && u > System.currentTimeMillis()) return false
        val m = modes[pkg] ?: 0
        if (m == 2) return true
        if (m == 1 && scheduleActive()) return true
        val l = limits[pkg] ?: 0
        return l > 0 && usedMs / 60_000L >= l
    }

    /** Returns a human reason if [pkg] must be blocked right now, else null. */
    fun blockReason(c: Context, pkg: String): String? {
        val u = tempUnlock[pkg]
        if (u != null && u > System.currentTimeMillis()) return null
        val m = modes[pkg] ?: 0
        if (m == 2) return "This app is blocked."
        if (m == 1 && scheduleActive()) return "It's a focus period."
        val l = limits[pkg] ?: 0
        if (l > 0 && Usage.minutesToday(c, pkg) >= l) return "You've used your $l-minute limit for today."
        return null
    }
}
