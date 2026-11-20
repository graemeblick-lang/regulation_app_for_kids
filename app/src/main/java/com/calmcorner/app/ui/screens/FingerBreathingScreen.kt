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
import androidx.compose.ui.geometry.Offset
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
 * Finger in unit coordinates (fractions of the canvas): centre x, tip y,
 * half width, base y, and a tilt in degrees around the base centre.
 * Geometry was checked visually with a browser mock-up before porting.
 */
private data class FingerSpec(val cx: Float, val tip: Float, val hw: Float, val base: Float, val tilt: Float = 0f)

private val fingers = listOf(
    FingerSpec(0.27f, 0.43f, 0.070f, 0.75f, tilt = -34f), // thumb, angled out
    FingerSpec(0.36f, 0.12f, 0.062f, 0.52f, tilt = -5f),
    FingerSpec(0.50f, 0.05f, 0.064f, 0.50f),
    FingerSpec(0.64f, 0.11f, 0.062f, 0.51f, tilt = 4f),
    FingerSpec(0.775f, 0.25f, 0.055f, 0.55f, tilt = 10f), // little finger
)

private const val ARC_K = 0.5523f // cubic control factor for a quarter circle

/** Key points of one finger (unit coords, already tilted). */
private class FingerPoints(private val spec: FingerSpec) {
    private val a = Math.toRadians(spec.tilt.toDouble())
    private val c = cos(a).toFloat()
    private val s = sin(a).toFloat()
    private fun p(x: Float, y: Float): Offset {
        val dx = x - spec.cx
        val dy = y - spec.base
        return Offset(spec.cx + dx * c - dy * s, spec.base + dx * s + dy * c)
    }
    private val l = spec.cx - spec.hw
    private val r = spec.cx + spec.hw
    private val cy = spec.tip + spec.hw
    private val k = spec.hw * ARC_K
    val leftBase = p(l, spec.base)
    val leftTop = p(l, cy)
    val c1 = p(l, cy - k)
    val c2 = p(spec.cx - k, spec.tip)
    val apex = p(spec.cx, spec.tip)
    val c3 = p(spec.cx + k, spec.tip)
    val c4 = p(r, cy - k)
    val rightTop = p(r, cy)
    val rightBase = p(r, spec.base)
}

/**
 * The hand outline as 10 traced pieces: for each finger, "up" (breathe in)
 * from the valley to the fingertip, then "down" (breathe out) to the next valley.
 * Plus the closed hand silhouette for filling.
 */
private class HandGeometry(private val w: Float, private val h: Float) {
    private fun Offset.px() = Offset(x * w, y * h)
    private fun Path.moveTo(o: Offset) = o.px().let { moveTo(it.x, it.y) }
    private fun Path.lineTo(o: Offset) = o.px().let { lineTo(it.x, it.y) }
    private fun Path.quadTo(c: Offset, e: Offset) {
        val a = c.px(); val b = e.px(); quadraticTo(a.x, a.y, b.x, b.y)
    }
    private fun Path.cubicTo(c1: Offset, c2: Offset, e: Offset) {
        val a = c1.px(); val b = c2.px(); val d = e.px(); cubicTo(a.x, a.y, b.x, b.y, d.x, d.y)
    }

    private val pts = fingers.map { FingerPoints(it) }

    /** Adds the "up" piece of finger i (without the initial moveTo). */
    private fun Path.up(i: Int) {
        val f = pts[i]
        if (i > 0) {
            val a = pts[i - 1].rightBase
            val b = f.leftBase
            quadTo(Offset((a.x + b.x) / 2f, maxOf(a.y, b.y) + 0.02f), b)
        }
        lineTo(f.leftTop)
        cubicTo(f.c1, f.c2, f.apex)
    }

    /** Adds the "down" piece of finger i (without the initial moveTo). */
    private fun Path.down(i: Int) {
        val f = pts[i]
        cubicTo(f.c3, f.c4, f.rightTop)
        lineTo(f.rightBase)
    }

    val pieces: List<Path> = (0 until 10).map { n ->
        val i = n / 2
        Path().apply {
            if (n % 2 == 0) {
                moveTo(if (i == 0) pts[0].leftBase else pts[i - 1].rightBase)
                up(i)
            } else {
                moveTo(pts[i].apex)
                down(i)
            }
        }
    }

    val silhouette: Path = Path().apply {
        moveTo(pts[0].leftBase)
        for (i in 0 until 5) { up(i); down(i) }
        quadTo(Offset(0.87f, 0.62f), Offset(0.86f, 0.78f))
        quadTo(Offset(0.85f, 0.97f), Offset(0.66f, 0.97f))
        lineTo(Offset(0.40f, 0.97f))
        quadTo(Offset(0.24f, 0.97f), Offset(0.22f, 0.84f))
        close()
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
            val outlineW = 8.dp.toPx()
            val traceW = 9.dp.toPx()
            val hand = HandGeometry(size.width, size.height)
            val fill = tool.color.copy(alpha = 0.25f).compositeOver(bg)
            val outline = tool.color.copy(alpha = 0.80f).compositeOver(bg)
            val line = Stroke(traceW, cap = StrokeCap.Round, join = StrokeJoin.Round)

            drawPath(hand.silhouette, fill)
            drawPath(hand.silhouette, outline, style = Stroke(outlineW, join = StrokeJoin.Round))

            val current = timer.phaseIndex
            // Everything already traced stays dark, so she can see how far she's come.
            hand.pieces.forEachIndexed { i, piece -> if (i < current) drawPath(piece, tool.deep, style = line) }

            val measure = PathMeasure().apply { setPath(hand.pieces[current], false) }
            val dist = if (started) timer.progress * measure.length else 0f
            if (started) {
                val traced = Path()
                measure.getSegment(0f, dist, traced, true)
                drawPath(traced, tool.deep, style = line)
            }
            val dot = measure.getPosition(dist)
            drawCircle(tool.deep.copy(alpha = 0.25f), radius = traceW * 2.4f, center = dot)
            drawCircle(tool.deep, radius = traceW * 1.35f, center = dot)
            drawCircle(Color.White, radius = traceW * 0.5f, center = dot)
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
