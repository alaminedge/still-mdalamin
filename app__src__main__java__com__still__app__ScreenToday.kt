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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

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

    val byPkg = apps.associateBy { it.pkg }
    val shown = if (apps.isEmpty()) usage else usage.filterKeys { byPkg.containsKey(it) }
    val total = shown.values.sum()
    val rows = usage.entries
        .filter { byPkg.containsKey(it.key) && it.value > 60_000 }
        .sortedByDescending { it.value }
        .take(12)
    val max = (rows.firstOrNull()?.value ?: 1L).toFloat()
    val blocked = Store.modes.values.count { it > 0 }
    val schedules = Store.schedules.count { it.on }

    LazyColumn(
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(Modifier.padding(top = 8.dp)) {
                Text(
                    "STILL",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 6.sp),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Use your phone. Don't let it use you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Panel {
                Text(
                    "SCREEN TIME TODAY",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (ua) fmtDur(total) else "--",
                    style = MaterialTheme.typography.displayMedium
                )
                Text(
                    if (ua) "${rows.size} apps used for more than a minute" else "Turn on usage access to see this",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

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
                    Text("Focus is active", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$blocked apps managed · $schedules focus schedules · Hardcore ${if (Store.hardcoreOn()) "on" else "off"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (rows.isNotEmpty()) {
            item {
                Text(
                    "APP USAGE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            items(rows, key = { it.key }) { r ->
                val a = byPkg[r.key]
                if (a != null) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppIcon(a, 36.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                a.label,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
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
}
