package com.calmcorner.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.calmcorner.app.data.Tool
import com.calmcorner.app.ui.components.BreathingControls
import com.calmcorner.app.ui.components.KeepScreenOn
import com.calmcorner.app.ui.components.PhaseKind
import com.calmcorner.app.ui.components.TimedPhase
import com.calmcorner.app.ui.components.ToolScaffold
import com.calmcorner.app.ui.components.doneText
import com.calmcorner.app.ui.components.rememberPhaseBuzz
import com.calmcorner.app.ui.components.rememberPhaseTimer
import com.calmcorner.app.ui.components.tapToToggle
import kotlin.math.cos
import kotlin.math.sin

private val fingerNames = listOf("thumb", "pointer finger", "middle finger", "ring finger", "little finger")

/**
 * Finger geometry as fractions of the canvas: left/right edges, tip and base
 * (before rotation), and a tilt in degrees around the base centre.
 */
private data class FingerSpec(val left: Float, val right: Float, val tip: Float, val base: Float, val tilt: Float = 0f)

private val fingers = listOf(
    FingerSpec(0.25f, 0.40f, 0.48f, 0.80f, tilt = -35f), // thumb, angled out
    FingerSpec(0.27f, 0.41f, 0.10f, 0.56f, tilt = -4f),
    FingerSpec(0.44f, 0.58f, 0.03f, 0.56f),
    FingerSpec(0.61f, 0.75f, 0.09f, 0.56f, tilt = 3f),
    FingerSpec(0.78f, 0.90f, 0.22f, 0.58f, tilt = 8f), // little finger
)

/** Up one side, round the tip, down the other side. Half the length = breathe in. */
private fun fingerPath(spec: FingerSpec, w: Float, h: Float): Path {
    val l = spec.left * w
    val r = spec.right * w
    val base = spec.base * h
    val rad = (r - l) / 2f
    val cy = spec.tip * h + rad
    val pivot = Offset((l + r) / 2f, base)
    val a = Math.toRadians(spec.tilt.toDouble())
    val c = cos(a).toFloat()
    val s = sin(a).toFloat()
    fun p(x: Float, y: Float): Offset {
        val dx = x - pivot.x
        val dy = y - pivot.y
        return Offset(pivot.x + dx * c - dy * s, pivot.y + dx * s + dy * c)
    }
    val k = rad * 1.33f // cubic control distance for a near-perfect semicircle
    return Path().apply {
        p(l, base).let { moveTo(it.x, it.y) }
        p(l, cy).let { lineTo(it.x, it.y) }
        val c1 = p(l, cy - k)
        val c2 = p(r, cy - k)
        val e = p(r, cy)
        cubicTo(c1.x, c1.y, c2.x, c2.y, e.x, e.y)
        p(r, base).let { lineTo(it.x, it.y) }
    }
}

@Composable
fun FingerBreathingScreen(onBack: () -> Unit) {
    val tool = Tool.FingerBreathing
    var pace by rememberSaveable { mutableIntStateOf(4) }
    var running by rememberSaveable { mutableStateOf(false) }
    val phases = remember(pace) {
        val s = pace.toFloat()
        List(5) { listOf(TimedPhase("Breathe in", s, PhaseKind.BreatheIn), TimedPhase("Breathe out", s, PhaseKind.BreatheOut)) }.flatten()
    }
    val timer = rememberPhaseTimer(phases, running, onPhaseStart = rememberPhaseBuzz())
    val started = running || timer.progress > 0f || timer.phaseIndex > 0 || timer.rounds > 0
    KeepScreenOn()

    val fingerIndex = timer.phaseIndex / 2
    val breathingIn = timer.phase.kind == PhaseKind.BreatheIn
    val bg = MaterialTheme.colorScheme.background

    ToolScaffold(
        title = tool.title,
        tip = "Trace your hand with the dot.",
        howTo = "Hold up one hand.\nUse a finger from your other hand.\n⬆ Slide up a finger: breathe in.\n⬇ Slide down: breathe out.",
        color = tool.deep,
        showTip = !running,
        onBack = onBack,
    ) {
        Spacer(Modifier.weight(1f))
        Canvas(Modifier.fillMaxWidth(0.82f).aspectRatio(0.9f).tapToToggle { running = !running }) {
            val w = size.width
            val h = size.height
            val outlineW = 4.dp.toPx()
            val traceW = 9.dp.toPx()
            // Opaque colours so overlapping shapes don't look darker.
            val fill = tool.color.copy(alpha = 0.22f).compositeOver(bg)
            val doneFill = tool.color.copy(alpha = 0.55f).compositeOver(bg)
            val outline = tool.color.copy(alpha = 0.75f).compositeOver(bg)

            val palm = Path().apply {
                addRoundRect(RoundRect(0.22f * w, 0.50f * h, 0.92f * w, 0.97f * h, CornerRadius(0.13f * w)))
            }
            val paths = fingers.map { fingerPath(it, w, h) }
            val all = paths + palm

            // Outline trick: draw thick strokes first, then fills on top -> one clean hand silhouette.
            all.forEach { drawPath(it, outline, style = Stroke(outlineW * 2, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
            all.forEach { drawPath(it, fill) }
            paths.forEachIndexed { i, path -> if (i < fingerIndex) drawPath(path, doneFill) }

            if (started) {
                val path = paths[fingerIndex]
                val measure = PathMeasure().apply { setPath(path, false) }
                val half = measure.length / 2f
                val dist = if (breathingIn) timer.progress * half else half + timer.progress * half
                val traced = Path()
                measure.getSegment(0f, dist, traced, true)
                drawPath(traced, tool.deep, style = Stroke(traceW, cap = StrokeCap.Round, join = StrokeJoin.Round))
                val dot = measure.getPosition(dist)
                drawCircle(tool.deep.copy(alpha = 0.25f), radius = traceW * 2.4f, center = dot)
                drawCircle(tool.deep, radius = traceW * 1.35f, center = dot)
                drawCircle(Color.White, radius = traceW * 0.5f, center = dot)
            } else {
                // Show where to begin: a dot at the bottom of the thumb.
                val start = PathMeasure().apply { setPath(paths[0], false) }.getPosition(0f)
                drawCircle(tool.deep, radius = traceW * 1.35f, center = start)
                drawCircle(Color.White, radius = traceW * 0.5f, center = start)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            if (started) timer.phase.label else "Tap ▶ to start",
            style = MaterialTheme.typography.headlineMedium,
        )
        if (started) {
            Text(
                (if (breathingIn) "⬆ Up your " else "⬇ Down your ") + fingerNames[fingerIndex],
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(doneText(timer.rounds, "hand"), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.weight(1f))
        BreathingControls(
            pace = pace, onPace = { pace = it; running = false },
            running = running, started = started, color = tool.color, deep = tool.deep,
            onToggle = { running = !running }, onReset = { running = false; timer.reset() },
        )
    }
}
