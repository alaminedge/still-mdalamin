package com.still.app

import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.ui.platform.LocalDensity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Store.init(this)
        setContent { StillTheme { Root() } }
    }
}

private data class Tab(val label: String, val icon: ImageVector)

@Composable
fun Root() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var tick by remember { mutableIntStateOf(0) }
    val animOn = animationsOn()
    // While the keyboard is up: hide the tab bar (it would float above the keyboard)
    // and let the content sit directly on top of the keyboard.
    val density = LocalDensity.current
    val imeOpen = WindowInsets.ime.getBottom(density) > 0
    val topInset = WindowInsets.systemBars.getTop(density)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { Apps.refresh(); tick++ }
    val tabs = listOf(
        Tab("Today", Icons.Default.DateRange),
        Tab("Focus", Icons.Default.Lock),
        Tab("Home", Icons.Default.Home),
        Tab("More", Icons.Default.Settings)
    )
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = if (imeOpen) WindowInsets(0, topInset, 0, 0) else ScaffoldDefaults.contentWindowInsets,
        bottomBar = {
            AnimatedVisibility(visible = !imeOpen) {
            NavigationBar {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) }
                    )
                }
            }
            }
        }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad).statusBarsPadding().imePadding()) {
            Crossfade(
                targetState = tab,
                animationSpec = tween(if (animOn) 200 else 0),
                label = "tabs"
            ) { t ->
                when (t) {
                    0 -> ScreenToday(tick)
                    1 -> ScreenBlock()
                    2 -> ScreenLauncher(tick)
                    else -> ScreenMore(tick)
                }
            }
        }
    }
}
