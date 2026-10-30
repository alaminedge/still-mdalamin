package com.still.app

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.concurrent.thread

data class AppItem(val pkg: String, val label: String, val icon: ImageBitmap)

object Apps {
    @Volatile
    private var cache: List<AppItem>? = null
    @Volatile
    private var stale = true

    /** True once the first full load has finished. Compose reads this, so UI updates when it flips. */
    var loaded by mutableStateOf(false)
        private set

    /** Whatever is in memory right now, even if slightly out of date. Never blocks. */
    fun peek(): List<AppItem>? = cache

    fun preload(c: Context) {
        val app = c.applicationContext
        thread(name = "still-apps", isDaemon = true) { load(app) }
    }

    @Synchronized
    fun load(c: Context): List<AppItem> {
        val old = cache
        if (old != null && !stale) return old
        val pm = c.packageManager
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        // Reuse icons we already built; only new apps cost anything.
        val known = old?.associateBy { it.pkg } ?: emptyMap()
        val list = pm.queryIntentActivities(i, 0)
            .distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName != c.packageName }
            .map {
                known[it.activityInfo.packageName] ?: AppItem(
                    it.activityInfo.packageName,
                    it.loadLabel(pm).toString(),
                    it.loadIcon(pm).toBitmap(96, 96).asImageBitmap()
                )
            }
            .sortedBy { it.label.lowercase() }
        cache = list
        stale = false
        loaded = true
        return list
    }

    /** Mark the list as possibly changed. The old list stays visible until the new one is ready. */
    fun refresh() { stale = true }

    fun launch(c: Context, pkg: String) {
        c.packageManager.getLaunchIntentForPackage(pkg)?.let {
            c.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

@Composable
fun rememberApps(key: Any? = null): List<AppItem> {
    val c = LocalContext.current
    val s = produceState(initialValue = Apps.peek() ?: emptyList<AppItem>(), key) {
        value = withContext(Dispatchers.Default) { Apps.load(c) }
    }
    return s.value
}
