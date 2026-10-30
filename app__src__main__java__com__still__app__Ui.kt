package com.still.app

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun toast(c: Context, s: String) = Toast.makeText(c, s, Toast.LENGTH_SHORT).show()

fun fmtDur(ms: Long): String {
    val m = ms / 60_000
    val h = m / 60
    return if (h > 0) "${h}h ${m % 60}m" else "${m}m"
}

fun fmtDate(ms: Long): String = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date(ms))

fun lockedMsg() = "Hardcore Mode is on until ${fmtDate(Store.hardcoreUntil)}"

fun hm(m: Int): String {
    val h12 = if ((m / 60) % 12 == 0) 12 else (m / 60) % 12
    return "%d:%02d %s".format(h12, m % 60, if (m < 720) "AM" else "PM")
}

@Composable
fun AppIcon(a: AppItem, size: Dp = 40.dp) {
    Image(
        bitmap = a.icon,
        contentDescription = null,
        modifier = Modifier.size(size).clip(RoundedCornerShape(size * 0.28f))
    )
}

@Composable
fun Header(title: String, sub: String) {
    Column(Modifier.padding(top = 8.dp, bottom = 6.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 10.dp)
    )
}

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(18.dp),
        content = content
    )
}

@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        options.forEachIndexed { i, o ->
            val on = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (on) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    o,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SetupRow(title: String, sub: String, done: Boolean, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (done) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!done) {
            Spacer(Modifier.width(10.dp))
            androidx.compose.material3.FilledTonalButton(onClick = onClick) { Text(action) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeDialog(initial: Int, onDone: (Int) -> Unit, onCancel: () -> Unit) {
    val st = rememberTimePickerState(initial / 60, initial % 60, false)
    AlertDialog(
        onDismissRequest = onCancel,
        confirmButton = { TextButton(onClick = { onDone(st.hour * 60 + st.minute) }) { Text("Set") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
        text = { TimePicker(state = st) }
    )
}

@Composable
fun AppPicker(apps: List<AppItem>, selected: List<String>, onToggle: (String) -> Unit, onDone: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDone,
        confirmButton = { TextButton(onClick = onDone) { Text("Done") } },
        title = { Text("Choose apps") },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(apps, key = { it.pkg }) { a ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onToggle(a.pkg) }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(a, 32.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(a.label, Modifier.weight(1f))
                        Checkbox(checked = a.pkg in selected, onCheckedChange = { onToggle(a.pkg) })
                    }
                }
            }
        }
    )
}
