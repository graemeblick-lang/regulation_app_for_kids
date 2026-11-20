package com.calmcorner.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.calmcorner.app.data.Tool
import com.calmcorner.app.ui.components.BreathingControls
import com.calmcorner.app.ui.components.KeepScreenOn
import com.calmcorner.app.ui.components.PhaseKind
import com.calmcorner.app.ui.components.PhaseLabel
import com.calmcorner.app.ui.components.TimedPhase
import com.calmcorner.app.ui.components.ToolScaffold
import com.calmcorner.app.ui.components.doneText
import androidx.compose.runtime.LaunchedEffect
import com.calmcorner.app.data.LocalPrefs
import com.calmcorner.app.ui.components.rememberCues
import com.calmcorner.app.ui.components.rememberPhaseTimer
import com.calmcorner.app.ui.components.tapToToggle

@Composable
fun BoxBreathingScreen(onBack: () -> Unit, onFinish: () -> Unit) {
    val tool = Tool.BoxBreathing
    val prefs = LocalPrefs.current
    val cues = rememberCues()
    var pace by rememberSaveable { mutableIntStateOf(prefs.speedFor(tool)) }
    val target = prefs.breathsPerSession
    var running by rememberSaveable { mutableStateOf(false) }
    val phases = remember(pace) {
        val s = pace.toFloat()
        listOf(
            TimedPhase("Breathe in", s, PhaseKind.BreatheIn),
            TimedPhase("Hold", s, PhaseKind.Hold),
            TimedPhase("Breathe out", s, PhaseKind.BreatheOut),
            TimedPhase("Hold", s, PhaseKind.Hold),
        )
    }
    val timer = rememberPhaseTimer(phases, running, maxRounds = target, onPhaseStart = cues::phase)
    val started = running || timer.progress > 0f || timer.phaseIndex > 0 || timer.rounds > 0
    KeepScreenOn()
    LaunchedEffect(timer.finished) {
        if (timer.finished) { cues.say("All done"); onFinish() }
    }

    ToolScaffold(
        title = tool.title,
        tip = "Follow the dot around the square.",
        howTo = "⬆ Going up: breathe in.\n➡ Across the top: hold.\n⬇ Going down: breathe out.\n⬅ Across the bottom: hold.",
        color = tool.deep,
        showTip = !running,
        onBack = onBack,
    ) {
        Spacer(Modifier.weight(1f))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth(0.85f).aspectRatio(1f).tapToToggle { running = !running },
        ) {
            Canvas(Modifier.matchParentSize()) {
                val stroke = 16.dp.toPx()
                val inset = stroke * 1.6f
                val l = inset
                val t = inset
                val r = size.width - inset
                val b = size.height - inset
                // Soft filled square behind everything, so the shape is always clear.
                drawRect(tool.color.copy(alpha = 0.12f), topLeft = androidx.compose.ui.geometry.Offset(l, t), size = androidx.compose.ui.geometry.Size(r - l, b - t))
                // Start bottom-left: up (in), across (hold), down (out), back (hold).
                val square = Path().apply {
                    moveTo(l, b); lineTo(l, t); lineTo(r, t); lineTo(r, b); close()
                }
                val line = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                drawPath(square, tool.color.copy(alpha = 0.40f), style = line)

                val measure = PathMeasure().apply { setPath(square, false) }
                val side = measure.length / 4f
                val distance = (timer.phaseIndex + timer.progress) * side
                val traced = Path()
                measure.getSegment(0f, distance, traced, true)
                drawPath(traced, tool.color, style = line)

                val dot = measure.getPosition(distance)
                drawCircle(tool.deep.copy(alpha = 0.25f), radius = stroke * 2.2f, center = dot)
                drawCircle(tool.deep, radius = stroke * 1.25f, center = dot)
                drawCircle(Color.White, radius = stroke * 0.45f, center = dot)
            }
            PhaseLabel(
                label = if (started) timer.phase.label else "Tap ▶ to start",
                secondsLeft = timer.secondsLeft,
                showCount = running,
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(doneText(timer.rounds, "box", target), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.weight(1f))
        BreathingControls(
            pace = pace, onPace = { pace = it; prefs.saveSpeed(tool, it); running = false },
            running = running, started = started, color = tool.color, deep = tool.deep,
            onToggle = { running = !running }, onDone = { running = false; onFinish() },
        )
    }
}
