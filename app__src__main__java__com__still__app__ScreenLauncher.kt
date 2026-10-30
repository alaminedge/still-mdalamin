package com.still.app

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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

@Composable
fun ScreenLauncher(tick: Int) {
    val c = LocalContext.current
    val apps = rememberApps(tick)
    var q by remember { mutableStateOf("") }
    var sel by remember { mutableStateOf<AppItem?>(null) }
    val isHome = remember(tick) { Perms.isHome(c) }
    val pinned = apps.count { Store.home[it.pkg] == 1 }
    val hidden = apps.count { Store.home[it.pkg] == 2 }
    val shown = apps.filter { it.label.contains(q, true) }

    LazyColumn(
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Header("Minimal home", "Only what you need, nothing that pulls you in.") }
        item {
            Panel {
                Text(
                    if (isHome) "STILL is your home app" else "STILL is not your home app yet",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isHome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "$pinned on home · $hidden hidden",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!isHome) {
                        Button(onClick = { c.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }) { Text("Set as home app") }
                    }
                    OutlinedButton(onClick = { c.startActivity(Intent(c, LauncherActivity::class.java)) }) { Text("Preview") }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Hidden apps disappear from STILL's home and app list. They stay installed and still appear in Android Settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item { SectionLabel("Your apps") }
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
            val m = Store.home[a.pkg] ?: 0
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { sel = a }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(a, 44.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(a.label, style = MaterialTheme.typography.titleMedium)
                    Text(
                        when (m) { 1 -> "On home"; 2 -> "Hidden"; else -> "In app list" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (m != 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    sel?.let { a -> HomeSheet(a) { sel = null } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeSheet(a: AppItem, onClose: () -> Unit) {
    val c = LocalContext.current
    val m = Store.home[a.pkg] ?: 0
    val order = listOf(2, 0, 1) // Hidden, In list, On home
    ModalBottomSheet(onDismissRequest = onClose, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(horizontal = 22.dp).padding(bottom = 28.dp).navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(a, 52.dp)
                Spacer(Modifier.width(14.dp))
                Text(a.label, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(24.dp))
            Segmented(listOf("Hidden", "In list", "On home"), order.indexOf(m)) {
                if (!Store.setHome(a.pkg, order[it])) toast(c, lockedMsg())
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Hidden apps are removed from STILL. Pin only the essentials to your home.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
