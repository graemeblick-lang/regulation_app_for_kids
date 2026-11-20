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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.calmcorner.app.data.Tool
import com.calmcorner.app.ui.components.KeepScreenOn
import com.calmcorner.app.ui.components.PaceSelector
import com.calmcorner.app.ui.components.PhaseKind
import com.calmcorner.app.ui.components.PhaseLabel
import com.calmcorner.app.ui.components.StartPauseRow
import com.calmcorner.app.ui.components.TimedPhase
import com.calmcorner.app.ui.components.ToolScaffold
import com.calmcorner.app.ui.components.breathsText
import com.calmcorner.app.ui.components.rememberPhaseBuzz
import com.calmcorner.app.ui.components.rememberPhaseTimer

@Composable
fun BoxBreathingScreen(onBack: () -> Unit) {
    val color = Tool.BoxBreathing.color
    var pace by rememberSaveable { mutableIntStateOf(4) }
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
    val timer = rememberPhaseTimer(phases, running, onPhaseStart = rememberPhaseBuzz())
    KeepScreenOn()

    ToolScaffold(
        title = Tool.BoxBreathing.title,
        instructions = "Follow the dot around the square. Each side is the same length, so each part takes the same time.",
        onBack = onBack,
    ) {
        Spacer(Modifier.weight(1f))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth(0.85f).aspectRatio(1f)) {
            val track = MaterialTheme.colorScheme.surfaceVariant
            Canvas(Modifier.matchParentSize()) {
                val stroke = 14.dp.toPx()
                val inset = stroke * 1.5f
                val l = inset
                val t = inset
                val r = size.width - inset
                val b = size.height - inset
                // Start bottom-left: up (in), across (hold), down (out), back (hold).
                val square = Path().apply {
                    moveTo(l, b); lineTo(l, t); lineTo(r, t); lineTo(r, b); close()
                }
                drawPath(square, track, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

                val measure = PathMeasure().apply { setPath(square, false) }
                val side = measure.length / 4f
                val distance = (timer.phaseIndex + timer.progress) * side
                val traced = Path()
                measure.getSegment(0f, distance, traced, true)
                drawPath(traced, color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

                val dot: Offset = measure.getPosition(distance)
                drawCircle(color.copy(alpha = 0.25f), radius = stroke * 2.2f, center = dot)
                drawCircle(color, radius = stroke * 1.2f, center = dot)
            }
            PhaseLabel(
                label = if (running || timer.progress > 0f) timer.phase.label else "Ready?",
                secondsLeft = timer.secondsLeft,
                showCount = running,
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(breathsText(timer.rounds, "box"), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.weight(1f))
        PaceSelector(pace, { pace = it; running = false }, color)
        Spacer(Modifier.height(16.dp))
        StartPauseRow(running, color, onToggle = { running = !running }, onReset = { running = false; timer.reset() })
        Spacer(Modifier.height(8.dp))
    }
}
