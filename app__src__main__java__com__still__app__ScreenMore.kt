package com.still.app

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun ScreenMore(tick: Int) {
    val c = LocalContext.current
    val apps = rememberApps(tick)
    val hc = Store.hardcoreOn()
    var days by remember { mutableIntStateOf(3) }
    var confirm by remember { mutableStateOf(false) }
    var pickBg by remember { mutableStateOf(false) }
    val admin = remember(tick) { Perms.adminOn(c) }
    val bg = apps.filter { it.pkg in Store.bgApps }

    LazyColumn(
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Header(
                "More", "Hardcore Mode, emergency rules and background control.",
                chip = if (hc) "Hardcore ${fmtLeft(Store.hardcoreLeftMs())}" else "Hardcore off",
                chipActive = hc
            )
        }

        item {
            Panel {
                Text("Hardcore Mode", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                if (hc) {
                    Text(
                        "ON until ${fmtDate(Store.hardcoreUntil)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Blocks, limits and schedules can't be loosened, hidden apps stay hidden, and STILL's own Settings pages are closed to you.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        "Lock your rules in for a set time so you can't talk yourself out of them.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 3, 7, 30).forEach { d ->
                            FilterChip(selected = days == d, onClick = { days = d }, label = { Text(if (d == 1) "1 day" else "$d days") })
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { confirm = true }) { Text("Turn on Hardcore Mode") }
                }
                Spacer(Modifier.height(14.dp))
                SetupRow(
                    "Uninstall protection",
                    "Device admin makes STILL harder to remove.",
                    admin, "Enable"
                ) { c.startActivity(Perms.adminIntent(c)) }
            }
        }

        item {
            Panel {
                Text("Emergency unlock", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Normal: 3 per day, 60 second wait, 5 minutes of access.\nHardcore: 1 per day, 3 minute wait.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Left today: ${Store.emergencyLeft()}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        item {
            Panel {
                Text("Background control", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Pick apps you want quieter. STILL opens each app's Android battery and notification settings so you can restrict them. Android may still run essential background work, and only you (or the system) can force-stop an app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { pickBg = true }) { Text(if (bg.isEmpty()) "Choose apps" else "Change apps") }
            }
        }

        items(bg, key = { "bg-" + it.pkg }) { a ->
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(a, 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(a.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        c.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", a.pkg, null)))
                    }) { Text("Battery") }
                    OutlinedButton(onClick = {
                        c.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, a.pkg))
                    }) { Text("Alerts") }
                }
            }
        }

        item {
            Column(Modifier.padding(top = 8.dp)) {
                Text("STILL 1.0", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Use your phone. Don't let it use you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Turn on Hardcore Mode?") },
            text = {
                Text("For $days ${if (days == 1) "day" else "days"} you won't be able to turn off blocks, remove limits or schedules, unhide apps, or switch STILL off. This can't be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    if (!Perms.accessibilityOn(c)) toast(c, "Turn on STILL's accessibility service first (Today tab).")
                    else Store.startHardcore(days)
                }) { Text("Start") }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } }
        )
    }

    if (pickBg) {
        AppPicker(apps, Store.bgApps, onToggle = { Store.toggleBg(it) }, onDone = { pickBg = false })
    }
}
