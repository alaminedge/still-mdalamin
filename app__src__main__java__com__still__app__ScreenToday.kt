package com.still.app

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val Amber = Color(0xFFE8B84A)
private val LIMIT_CHOICES = listOf(0 to "Off", 60 to "1h", 120 to "2h", 180 to "3h", 240 to "4h", 300 to "5h", 360 to "6h", 480 to "8h")

@Composable
fun ScreenToday(tick: Int) {
    val c = LocalContext.current
    val apps = rememberApps(tick)
    var t by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); t++ } }
    val a11y = remember(tick) { Perms.accessibilityOn(c) }
    val ua = remember(tick) { Usage.hasAccess(c) }
    val usage by produceState(emptyMap<String, Long>(), tick, t, ua) {
        value = withContext(Dispatchers.Default) { Usage.today(c, 0) }
    }
    var pickLimit by remember { mutableStateOf(false) }

    val byPkg = apps.associateBy { it.pkg }
    val shown = if (apps.isEmpty()) usage else usage.filterKeys { byPkg.containsKey(it) }
    val total = shown.values.sum()
    val rows = usage.entries
        .filter { byPkg.containsKey(it.key) && it.value > 60_000 }
        .sortedByDescending { it.value }
        .take(12)
    val max = (rows.firstOrNull()?.value ?: 1L).toFloat()

    // t is read so these refresh every 30 s even when nothing else changes.
    val focus = remember(tick, t, Store.schedules.toList()) { Store.activeFocus() }
    val next = remember(tick, t, Store.schedules.toList()) { Store.nextFocus() }
    val managed = (Store.modes.keys + Store.limits.keys).filter { apps.isEmpty() || byPkg.containsKey(it) }.toSet()
    val blockedCount = managed.count { Store.blockedNow(it, usage[it] ?: 0L) }
    val alwaysBlocked = Store.modes.count { it.value == 2 && (apps.isEmpty() || byPkg.containsKey(it.key)) }
    val loadingApps = apps.isEmpty() && !Apps.loaded
    val blockedText = run {
        val base = when {
            blockedCount > 0 -> "$blockedCount app${if (blockedCount == 1) "" else "s"} blocked right now"
            alwaysBlocked > 0 -> "$alwaysBlocked always blocked"
            else -> "No apps blocked right now"
        }
        if (Store.hardcoreOn()) "$base · Hardcore on" else base
    }

    LazyColumn(
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ---- Header: logo, name, status ----
        item {
            Column(Modifier.padding(top = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StillLogo(46.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "STILL",
                        style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 6.sp),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    StatusChip(if (focus != null) "Focus on" else "Focus off", focus != null)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Use your phone. Don't let it use you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ---- 1. Time left today ----
        item {
            val limit = Store.totalLimit
            val limitMs = limit * 60_000L
            val left = limitMs - total
            val over = ua && limit > 0 && left < 0
            val fraction = when {
                !ua || limit == 0 -> 0f
                over -> 1f
                else -> (left.toFloat() / limitMs).coerceIn(0f, 1f)
            }
            val ringColor = when {
                over -> MaterialTheme.colorScheme.error
                limit > 0 && fraction < 0.25f -> Amber
                else -> MaterialTheme.colorScheme.primary
            }
            Panel(Modifier.pressClickable { pickLimit = true }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Ring(fraction, ringColor, Modifier.size(150.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            when {
                                !ua -> Text("--", style = MaterialTheme.typography.displayMedium)
                                limit == 0 -> {
                                    Text(fmtDur(total), style = MaterialTheme.typography.headlineMedium)
                                    Text("used today", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                over -> {
                                    Text(fmtDur(-left), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.error)
                                    Text("over limit", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                else -> {
                                    Text(fmtDur(left), style = MaterialTheme.typography.headlineMedium)
                                    Text("left today", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.width(18.dp))
                    Column(Modifier.weight(1f)) {
                        Text("USED TODAY", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (ua) fmtDur(total) else "--", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(12.dp))
                        Text("DAILY LIMIT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            if (limit > 0) fmtDur(limitMs) else "Not set",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (limit > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    when {
                        !ua -> "Turn on usage access to see this"
                        limit == 0 -> "Tap to set a daily limit for your whole phone"
                        else -> "Tap to change your daily limit"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ---- 2. Focus status / setup ----
        if (!a11y || !ua) {
            item {
                Panel {
                    Text("Finish setup", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    SetupRow(
                        "Blocking engine",
                        "Turn on STILL in Accessibility so it can stop blocked apps.",
                        a11y, "Turn on"
                    ) { c.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                    SetupRow(
                        "Usage access",
                        "Needed for screen time and daily limits.",
                        ua, "Allow"
                    ) { c.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                }
            }
        } else {
            item {
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (focus != null) {
                            Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            if (focus != null) "Focus is on" else "Focus is off",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (focus != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        when {
                            focus != null -> "${focus.name} until ${hm(focus.endMin)} · ${fmtLeft(focus.minsLeft * 60_000L)} left"
                            next != null -> "Next: ${next.first.name} in ${fmtLeft(next.second * 60_000L)}"
                            else -> "No focus hours scheduled"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        blockedText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ---- 3. App usage ----
        if (rows.isNotEmpty() || (loadingApps && ua)) {
            item {
                Text(
                    "APP USAGE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
        if (loadingApps && ua) {
            items(4) { SkeletonRow(36.dp) }
        }
        items(rows, key = { it.key }) { r ->
            val a = byPkg[r.key]
            if (a != null) {
                Reveal {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppIcon(a, 36.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                a.label,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (Store.blockedNow(r.key, r.value)) {
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Blocked now",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            Text(
                                fmtDur(r.value),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { r.value / max },
                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (pickLimit) {
        AlertDialog(
            onDismissRequest = { pickLimit = false },
            confirmButton = { TextButton(onClick = { pickLimit = false }) { Text("Close") } },
            title = { Text("Daily phone limit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Total screen time across all apps. The ring shows how much is left.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LIMIT_CHOICES.chunked(4).forEach { rowChoices ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowChoices.forEach { (mins, label) ->
                                FilterChip(
                                    selected = Store.totalLimit == mins,
                                    onClick = {
                                        if (Store.setTotalLimit(mins)) {
                                            pickLimit = false
                                        } else {
                                            toast(c, lockedMsg())
                                        }
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}
