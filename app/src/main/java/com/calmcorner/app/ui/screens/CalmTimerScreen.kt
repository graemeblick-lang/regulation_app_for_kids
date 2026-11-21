package com.calmcorner.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmcorner.app.data.LocalPrefs
import com.calmcorner.app.data.Tool
import com.calmcorner.app.ui.components.KeepScreenOn
import com.calmcorner.app.ui.components.PhaseKind
import com.calmcorner.app.ui.components.StartPauseRow
import com.calmcorner.app.ui.components.TimedPhase
import com.calmcorner.app.ui.components.ToolScaffold
import com.calmcorner.app.ui.components.rememberCues
import com.calmcorner.app.ui.components.rememberPhaseTimer
import com.calmcorner.app.ui.components.tapToToggle

@Composable
fun CalmTimerScreen(onBack: () -> Unit, onFinish: () -> Unit) {
    val tool = Tool.CalmTimer
    val prefs = LocalPrefs.current
    val cues = rememberCues()
    var minutes by rememberSaveable { mutableIntStateOf(prefs.timerMinutes()) }
    var running by rememberSaveable { mutableStateOf(false) }
    val phases = remember(minutes) { listOf(TimedPhase("Rest", minutes * 60f, PhaseKind.Hold, spoken = "Rest quietly")) }
    val timer = rememberPhaseTimer(phases, running, loop = false, onPhaseStart = { cues.say("Rest quietly") })
    val started = running || timer.progress > 0f
    KeepScreenOn()
    LaunchedEffect(timer.finished) {
        if (timer.finished) { cues.buzz(); cues.say("Time is up. All done"); onFinish() }
    }

    // A slow "breathing" circle in the middle to rest your eyes on (5s in, 5s out).
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.55f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(5000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    val totalSeconds = minutes * 60
    val left = if (started) ((1f - timer.progress) * totalSeconds).toInt().coerceAtLeast(0) else totalSeconds

    ToolScaffold(
        title = tool.title,
        tip = "Sit still. Rest until the timer ends.",
        howTo = "Choose how long.\nSit somewhere comfy.\nWatch the circle. Breathe slowly.",
        color = tool.deep,
        showTip = !running,
        onBack = onBack,
    ) {
        Spacer(Modifier.weight(1f))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth(0.8f).aspectRatio(1f).tapToToggle { running = !running }
            .semantics { contentDescription = "Timer circle. Tap to start or pause." }) {
            Canvas(Modifier.matchParentSize()) {
                val stroke = 16.dp.toPx()
                val d = size.minDimension - stroke
                val tl = Offset((size.width - d) / 2f, (size.height - d) / 2f)
                drawArc(tool.color.copy(alpha = 0.25f), 0f, 360f, false, tl, Size(d, d), style = Stroke(stroke))
                // Ring empties as time passes
                drawArc(tool.deep, -90f, 360f * (1f - timer.progress), false, tl, Size(d, d), style = Stroke(stroke, cap = StrokeCap.Round))
                val r = (d / 2f - stroke) * (if (running && !prefs.reduceMotion) pulse else 0.7f)
                drawCircle(tool.color.copy(alpha = 0.35f), r)
            }
            Text(
                "%d:%02d".format(left / 60, left % 60),
                fontSize = 52.sp,
                style = MaterialTheme.typography.displaySmall,
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            if (running) "Breathe slowly…" else if (started) "Paused" else "Tap ▶ to start",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.weight(1f))
        if (!running) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(1, 2, 5).forEach { m ->
                    FilterChip(
                        selected = minutes == m,
                        onClick = { minutes = m; prefs.saveTimerMinutes(m); running = false },
                        label = { Text("$m min", style = MaterialTheme.typography.bodyMedium) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = tool.color.copy(alpha = 0.3f)),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        StartPauseRow(running, started, tool.deep, onToggle = { running = !running }, onDone = { running = false; onFinish() })
        Spacer(Modifier.height(8.dp))
    }
}
