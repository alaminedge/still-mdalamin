package com.still.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DAY_LABELS = listOf("S", "M", "T", "W", "T", "F", "S")
private val LIMITS = listOf(0 to "Off", 15 to "15m", 30 to "30m", 60 to "1h", 90 to "1.5h", 120 to "2h", 180 to "3h")

@Composable
fun ScreenBlock() {
    val c = LocalContext.current
    val apps = rememberApps()
    var q by remember { mutableStateOf("") }
    var sel by remember { mutableStateOf<AppItem?>(null) }
    var picking by remember { mutableStateOf<Pair<Long, Boolean>?>(null) }
    val shown = apps.filter { it.label.contains(q, true) }

    LazyColumn(
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Header("Focus", "Choose what to block, and when.") }
        item { SectionLabel("Focus hours") }
        items(Store.schedules, key = { it.id }) { s ->
            ScheduleCard(s) { start -> picking = s.id to start }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { Store.addSchedule("Study", 95, 9 * 60, 13 * 60) }) { Text("+ Study") }
                FilledTonalButton(onClick = { Store.addSchedule("Sleep", 127, 22 * 60, 6 * 60) }) { Text("+ Sleep") }
                OutlinedButton(onClick = { Store.addSchedule("Custom", 127, 14 * 60, 16 * 60) }) { Text("+ Custom") }
            }
        }
        item { SectionLabel("Apps") }
        item {
            TextField(
                value = q,
                onValueChange = { q = it },
                placeholder = { Text("Search apps") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )
        }
        items(shown, key = { it.pkg }) { a ->
            val mode = Store.modes[a.pkg] ?: 0
            val lim = Store.limits[a.pkg] ?: 0
            val status = buildList {
                if (mode == 2) add("Always blocked")
                if (mode == 1) add("Focus hours")
                if (lim > 0) add("$lim min limit")
            }.joinToString(" · ").ifEmpty { "Not blocked" }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { sel = a }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(a, 44.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(a.label, style = MaterialTheme.typography.titleMedium)
                    Text(
                        status,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (mode > 0 || lim > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    sel?.let { a -> BlockSheet(a) { sel = null } }

    picking?.let { (id, start) ->
        val s = Store.schedules.firstOrNull { it.id == id }
        if (s != null) {
            TimeDialog(
                initial = if (start) s.start else s.end,
                onDone = { m ->
                    val n = if (start) s.copy(start = m) else s.copy(end = m)
                    if (!Store.updateSchedule(n)) toast(c, lockedMsg())
                    picking = null
                },
                onCancel = { picking = null }
            )
        }
    }
}

@Composable
private fun ScheduleCard(s: Schedule, onPick: (Boolean) -> Unit) {
    val c = LocalContext.current
    fun guard(ok: Boolean) { if (!ok) toast(c, lockedMsg()) }
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(s.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Switch(checked = s.on, onCheckedChange = { guard(Store.updateSchedule(s.copy(on = it))) })
            IconButton(onClick = { guard(Store.removeSchedule(s.id)) }) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DAY_LABELS.forEachIndexed { i, l ->
                val on = ((s.days shr i) and 1) == 1
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            val nd = s.days xor (1 shl i)
                            if (nd != 0) guard(Store.updateSchedule(s.copy(days = nd)))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        l,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TimeButton("From", hm(s.start), Modifier.weight(1f)) { onPick(true) }
            TimeButton("Until", hm(s.end), Modifier.weight(1f)) { onPick(false) }
        }
    }
}

@Composable
private fun TimeButton(label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlockSheet(a: AppItem, onClose: () -> Unit) {
    val c = LocalContext.current
    val mode = Store.modes[a.pkg] ?: 0
    val lim = Store.limits[a.pkg] ?: 0
    val used = remember { Usage.minutesToday(c, a.pkg) }
    ModalBottomSheet(onDismissRequest = onClose, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(horizontal = 22.dp).padding(bottom = 28.dp).navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(a, 52.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(a.label, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Used today: ${used} min",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            SectionLabel("When to block")
            Spacer(Modifier.height(8.dp))
            Segmented(listOf("Off", "Focus hours", "Always"), mode) {
                if (!Store.setMode(a.pkg, it)) toast(c, lockedMsg())
            }
            if (mode == 1 && Store.schedules.none { it.on }) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "No focus hours are on yet. Add one on the Focus screen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(22.dp))
            SectionLabel("Daily limit")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LIMITS.forEach { (m, l) ->
                    FilterChip(
                        selected = lim == m,
                        onClick = { if (!Store.setLimit(a.pkg, m)) toast(c, lockedMsg()) },
                        label = { Text(l) }
                    )
                }
            }
        }
    }
}
