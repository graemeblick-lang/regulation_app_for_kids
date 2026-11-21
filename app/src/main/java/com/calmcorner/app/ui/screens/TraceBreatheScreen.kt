package com.calmcorner.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.calmcorner.app.data.LocalPrefs
import com.calmcorner.app.data.Tool
import com.calmcorner.app.ui.components.BreathingControls
import com.calmcorner.app.ui.components.KeepScreenOn
import com.calmcorner.app.ui.components.PhaseKind
import com.calmcorner.app.ui.components.TimedPhase
import com.calmcorner.app.ui.components.ToolScaffold
import com.calmcorner.app.ui.components.doneText
import com.calmcorner.app.ui.components.rememberCues
import com.calmcorner.app.ui.components.rememberPhaseTimer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** Pieces of the shape to trace: even pieces = breathe in, odd = breathe out. */
private fun shapePieces(shape: Int, w: Float, h: Float): List<Path> {
    return if (shape == 0) {
        // Infinity loop: left loop = breathe in, right loop = breathe out (starts in the middle).
        fun pt(t: Double): Offset {
            val s = sin(t)
            val c = cos(t)
            val d = 1 + s * s
            return Offset((w / 2 + (c / d) * w * 0.45).toFloat(), (h / 2 + (s * c / d) * w * 0.55).toFloat())
        }
        listOf(PI / 2 to 3 * PI / 2, 3 * PI / 2 to 5 * PI / 2).map { (a, b) ->
            Path().apply {
                for (i in 0..120) {
                    val q = pt(a + (b - a) * i / 120.0)
                    if (i == 0) moveTo(q.x, q.y) else lineTo(q.x, q.y)
                }
            }
        }
    } else {
        // Star: up to a point = breathe in, down to the next dip = breathe out.
        val c = Offset(w / 2, h / 2 + h * 0.04f)
        val outer = minOf(w, h) * 0.46f
        val pts = List(10) { i ->
            val a = -PI / 2 + i * PI / 5
            val r = if (i % 2 == 0) outer else outer * 0.45f
            Offset((c.x + cos(a) * r).toFloat(), (c.y + sin(a) * r).toFloat())
        }
        // Order: dip 9 -> point 0 -> dip 1 -> point 2 -> ... -> dip 9
        val order = listOf(9) + (0..9).toList()
        order.zipWithNext().map { (a, b) -> Path().apply { moveTo(pts[a].x, pts[a].y); lineTo(pts[b].x, pts[b].y) } }
    }
}

@Composable
fun TraceBreatheScreen(onBack: () -> Unit, onFinish: () -> Unit) {
    val tool = Tool.TraceBreathe
    val prefs = LocalPrefs.current
    val cues = rememberCues()
    var pace by rememberSaveable { mutableIntStateOf(prefs.speedFor(tool)) }
    var running by rememberSaveable { mutableStateOf(false) }
    val shape = prefs.traceShape
    val piecesPerRound = if (shape == 0) 2 else 10
    // Loop: 1 round = 1 breath. Star: 1 round = 5 breaths.
    val target = prefs.breathsPerSession.let { if (it == 0) 0 else if (shape == 0) it else maxOf(1, it / 5) }
    val phases = remember(pace, shape) {
        List(piecesPerRound) { i ->
            if (i % 2 == 0) TimedPhase("Breathe in", pace.toFloat(), PhaseKind.BreatheIn)
            else TimedPhase("Breathe out", pace.toFloat(), PhaseKind.BreatheOut)
        }
    }
    val timer = rememberPhaseTimer(phases, running, maxRounds = target, onPhaseStart = cues::phase)
    val started = running || timer.progress > 0f || timer.phaseIndex > 0 || timer.rounds > 0
    KeepScreenOn()
    LaunchedEffect(timer.finished) {
        if (timer.finished) { cues.say("All done"); onFinish() }
    }
    // Where her finger is (to make the dot glow when she's following it).
    var finger by remember { mutableStateOf<Offset?>(null) }

    ToolScaffold(
        title = tool.title,
        tip = "Trace the shape with your finger. Follow the dot.",
        howTo = if (shape == 0) "Put your finger on the dot.\nGo round the left loop: breathe in.\nGo round the right loop: breathe out."
        else "Put your finger on the dot.\nGo up to a point: breathe in.\nGo down: breathe out.",
        color = tool.deep,
        showTip = !running,
        onBack = onBack,
    ) {
        if (!running) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "♾️ Loop", 1 to "⭐ Star").forEach { (v, label) ->
                    FilterChip(
                        selected = shape == v,
                        onClick = { prefs.updateTraceShape(v); running = false },
                        label = { Text(label, style = MaterialTheme.typography.bodyLarge) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = tool.color.copy(alpha = 0.3f)),
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Canvas(
            Modifier
                .fillMaxWidth(0.95f)
                .aspectRatio(if (shape == 0) 1.6f else 1f)
                .semantics { contentDescription = "Shape to trace with your finger" }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        finger = down.position
                        if (!running) running = true // touching the dot starts it
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            finger = change.position
                            if (change.positionChange() != Offset.Zero) change.consume()
                        } while (change.pressed)
                        finger = null
                    }
                },
        ) {
            val pieces = shapePieces(shape, size.width, size.height)
            val line = Stroke(14.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            pieces.forEach { drawPath(it, tool.color.copy(alpha = 0.35f), style = line) }
            val index = timer.phaseIndex.coerceAtMost(pieces.lastIndex)
            pieces.forEachIndexed { i, piece -> if (i < index) drawPath(piece, tool.deep, style = line) }
            val measure = PathMeasure().apply { setPath(pieces[index], false) }
            val dist = (if (started) timer.progress else 0f) * measure.length
            if (started) {
                val traced = Path()
                measure.getSegment(0f, dist, traced, true)
                drawPath(traced, tool.deep, style = line)
            }
            val dot = measure.getPosition(dist)
            val following = finger?.let { hypot(it.x - dot.x, it.y - dot.y) < 48.dp.toPx() } == true
            if (following) drawCircle(tool.color.copy(alpha = 0.45f), 34.dp.toPx(), dot)
            drawCircle(tool.deep.copy(alpha = 0.25f), 24.dp.toPx(), dot)
            drawCircle(tool.deep, 14.dp.toPx(), dot)
            drawCircle(Color.White, 5.dp.toPx(), dot)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            if (started) timer.phase.label else "Put your finger on the dot",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(doneText(timer.rounds, if (shape == 0) "loop" else "star", target), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.weight(1f))
        BreathingControls(
            pace = pace, onPace = { pace = it; prefs.saveSpeed(tool, it); running = false },
            running = running, started = started, color = tool.color, deep = tool.deep,
            onToggle = { running = !running }, onDone = { running = false; onFinish() },
        )
    }
}
