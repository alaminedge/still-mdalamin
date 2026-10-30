package com.still.app

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppItem(val pkg: String, val label: String, val icon: ImageBitmap)

object Apps {
    @Volatile
    private var cache: List<AppItem>? = null

    fun load(c: Context): List<AppItem> {
        cache?.let { return it }
        val pm = c.packageManager
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val list = pm.queryIntentActivities(i, 0)
            .distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName != c.packageName }
            .map {
                AppItem(
                    it.activityInfo.packageName,
                    it.loadLabel(pm).toString(),
                    it.loadIcon(pm).toBitmap(96, 96).asImageBitmap()
                )
            }
            .sortedBy { it.label.lowercase() }
        cache = list
        return list
    }

    fun refresh() { cache = null }

    fun launch(c: Context, pkg: String) {
        c.packageManager.getLaunchIntentForPackage(pkg)?.let {
            c.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

@Composable
fun rememberApps(key: Any? = null): List<AppItem> {
    val c = LocalContext.current
    val s = produceState(initialValue = emptyList<AppItem>(), key) {
        value = withContext(Dispatchers.Default) { Apps.load(c) }
    }
    return s.value
}
