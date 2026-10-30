package com.still.app

import androidx.compose.ui.input.nestedscroll.nestedScroll
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Store.init(this)
        setContent { StillTheme { LauncherScreen() } }
    }
}

@Composable
fun LauncherScreen() {
    val c = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { Apps.refresh(); tick++ }
    val apps = rememberApps(tick)
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(15_000) } }
    var drawer by remember { mutableStateOf(false) }
    BackHandler(enabled = true) { }
    val pinned = apps.filter { Store.home[it.pkg] == 1 }
    val timeFmt = remember { SimpleDateFormat("h:mm", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 28.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, d -> if (d < -25f) drawer = true }
                    }
            ) {
                Spacer(Modifier.height(48.dp))
                Text(timeFmt.format(Date(now)), style = MaterialTheme.typography.displayLarge)
                Text(
                    dateFmt.format(Date(now)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                pinned.forEach { a ->
                    Text(
                        a.label,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Normal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { Apps.launch(c, a.pkg) }
                            .padding(vertical = 12.dp)
                    )
                }
                if (pinned.isEmpty()) {
                    Text(
                        "Pin your essentials from the STILL app, on the Home tab.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { drawer = true }) { Text("All apps") }
                    TextButton(onClick = {
                        c.startActivity(Intent(c, MainActivity::class.java))
                    }) { Text("STILL") }
                }
                Spacer(Modifier.height(8.dp))
            }
            AnimatedVisibility(
                visible = drawer,
                enter = slideInVertically { it / 2 } + fadeIn(),
                exit = fadeOut()
            ) {
                DrawerPanel(apps) { drawer = false }
            }
        }
    }
}

@Composable
private fun DrawerPanel(apps: List<AppItem>, onClose: () -> Unit) {
    val c = LocalContext.current
    var q by remember { mutableStateOf("") }
    BackHandler { onClose() }
    val list = apps.filter { (Store.home[it.pkg] ?: 0) != 2 && it.label.contains(q, true) }
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp)
    ) {
        SearchField(
            value = q,
            onValueChange = { q = it },
            modifier = Modifier.padding(top = 16.dp),
            imeAction = ImeAction.Go,
            onAction = { list.firstOrNull()?.let { Apps.launch(c, it.pkg); onClose() } }
        )
        LazyColumn(Modifier.weight(1f).padding(top = 8.dp).nestedScroll(rememberHideKeyboardOnScroll())) {
            items(list, key = { it.pkg }) { a ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { Apps.launch(c, a.pkg); onClose() }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIcon(a, 40.dp)
                    Spacer(Modifier.width(16.dp))
                    Text(a.label, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
