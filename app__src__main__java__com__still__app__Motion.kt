package com.still.app

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** False when the user turned system animations off. Every animation here respects it. */
@Composable
fun animationsOn(): Boolean {
    val c = LocalContext.current
    return remember {
        Settings.Global.getFloat(c.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
}

/** Soft moving highlight for placeholder rows while content loads. */
fun Modifier.shimmer(): Modifier = composed {
    val on = animationsOn()
    val base = MaterialTheme.colorScheme.surfaceVariant
    val hi = MaterialTheme.colorScheme.surfaceContainerHighest
    val x = if (on) {
        val t = rememberInfiniteTransition(label = "shimmer")
        t.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)),
            label = "shimmerX"
        ).value
    } else 0.5f
    background(
        Brush.linearGradient(
            colors = listOf(base, hi, base),
            start = Offset(x * 1400f - 500f, 0f),
            end = Offset(x * 1400f, 0f)
        )
    )
}

@Composable
fun SkeletonRow(iconSize: Dp = 44.dp) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(iconSize).clip(RoundedCornerShape(iconSize * 0.28f)).shimmer())
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Box(Modifier.fillMaxWidth(0.5f).height(14.dp).clip(RoundedCornerShape(50)).shimmer())
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth(0.3f).height(10.dp).clip(RoundedCornerShape(50)).shimmer())
        }
    }
}

/** Quick fade-in with a small rise (200 ms). */
@Composable
fun Reveal(content: @Composable () -> Unit) {
    val on = animationsOn()
    val a = remember { Animatable(if (on) 0f else 1f) }
    LaunchedEffect(Unit) { if (on) a.animateTo(1f, tween(200, easing = FastOutSlowInEasing)) }
    Box(
        Modifier.graphicsLayer {
            alpha = a.value
            translationY = (1f - a.value) * 10.dp.toPx()
        }
    ) { content() }
}

/** Clickable that shrinks very slightly while pressed. */
fun Modifier.pressClickable(onClick: () -> Unit): Modifier = composed {
    val on = animationsOn()
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed && on) 0.97f else 1f, tween(100), label = "press")
    this
        .graphicsLayer { scaleX = s; scaleY = s }
        .clickable(interactionSource = src, indication = LocalIndication.current, onClick = onClick)
}

/** Progress ring that fills in when it first appears and eases on changes. */
@Composable
fun Ring(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val on = animationsOn()
    var target by remember { mutableFloatStateOf(if (on) 0f else progress) }
    LaunchedEffect(progress) { target = progress }
    val p by animateFloatAsState(
        target.coerceIn(0f, 1f),
        tween(if (on) 900 else 0, easing = FastOutSlowInEasing),
        label = "ring"
    )
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = 14.dp.toPx()
            val d = size.minDimension - sw
            val tl = Offset(sw / 2, sw / 2)
            val sz = Size(d, d)
            drawArc(track, -90f, 360f, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
            if (p > 0.001f) {
                drawArc(color, -90f, 360f * p, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
            }
        }
        content()
    }
}

/**
 * The STILL mark: a ring with a gap, a still dot resting in the gap,
 * and an S drawn as one continuous wave. Same geometry as the launcher icon.
 */
@Composable
fun StillLogo(logoSize: Dp = 40.dp, animate: Boolean = true) {
    val on = animationsOn() && animate
    val p = remember { Animatable(if (on) 0f else 1f) }
    LaunchedEffect(Unit) { if (on) p.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    val mint = MaterialTheme.colorScheme.primary
    Canvas(Modifier.size(logoSize)) {
        // Icon geometry lives in a 108 grid; the mark itself spans x/y 18..90 (72 units).
        val k = this.size.minDimension / 72f
        fun o(v: Float) = (v - 18f) * k
        val fade = p.value

        drawCircle(
            mint.copy(alpha = 0.22f),
            radius = 28f * k,
            center = Offset(o(54f), o(54f)),
            style = Stroke(3.5f * k)
        )
        drawArc(
            mint, -35f, 310f * p.value, false,
            topLeft = Offset(o(26f), o(26f)),
            size = Size(56f * k, 56f * k),
            style = Stroke(4.5f * k, cap = StrokeCap.Round)
        )
        drawCircle(mint.copy(alpha = fade), 2.6f * k, Offset(o(68f), o(29.75f)))

        val s = Path().apply {
            arcTo(Rect(Offset(o(54f), o(46f)), 8f * k), -40f, -230f, true)
            arcTo(Rect(Offset(o(54f), o(62f)), 8f * k), -90f, 230f, false)
        }
        drawPath(
            s, mint.copy(alpha = fade),
            style = Stroke(4.5f * k, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun StatusChip(text: String, active: Boolean) {
    val bg = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val dot = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(7.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.sp),
            color = fg,
            maxLines = 1
        )
    }
}
