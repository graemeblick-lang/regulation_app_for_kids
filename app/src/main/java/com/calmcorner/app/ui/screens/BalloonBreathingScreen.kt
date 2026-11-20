package com.calmcorner.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.calmcorner.app.data.Tool
import com.calmcorner.app.data.rememberIntPref
import com.calmcorner.app.ui.components.BreathingControls
import com.calmcorner.app.ui.components.KeepScreenOn
import com.calmcorner.app.ui.components.PhaseKind
import com.calmcorner.app.ui.components.PhaseLabel
import com.calmcorner.app.ui.components.TimedPhase
import com.calmcorner.app.ui.components.ToolScaffold
import com.calmcorner.app.ui.components.doneText
import com.calmcorner.app.ui.components.rememberPhaseBuzz
import com.calmcorner.app.ui.components.rememberPhaseTimer
import com.calmcorner.app.ui.components.tapToToggle

private val balloonColors = listOf(
    "Pink" to Color(0xFFE57B8D),
    "Red" to Color(0xFFE5534B),
    "Orange" to Color(0xFFF0954A),
    "Yellow" to Color(0xFFF2C94C),
    "Green" to Color(0xFF5DB37E),
    "Blue" to Color(0xFF5B9BE0),
    "Purple" to Color(0xFF9C7BD6),
)

@Composable
fun BalloonBreathingScreen(onBack: () -> Unit) {
    val tool = Tool.BalloonBreathing
    var colorIndex by rememberIntPref("balloon_color", 0)
    val color = balloonColors.getOrElse(colorIndex) { balloonColors[0] }.second
    var pace by rememberSaveable { mutableIntStateOf(4) }
    var running by rememberSaveable { mutableStateOf(false) }
    // Longer out-breath than in-breath helps the body settle.
    val phases = remember(pace) {
        listOf(
            TimedPhase("Breathe in", pace.toFloat(), PhaseKind.BreatheIn),
            TimedPhase("Hold", 1.5f, PhaseKind.Hold),
            TimedPhase("Breathe out", pace + 2f, PhaseKind.BreatheOut),
        )
    }
    val timer = rememberPhaseTimer(phases, running, onPhaseStart = rememberPhaseBuzz())
    val started = running || timer.progress > 0f || timer.phaseIndex > 0 || timer.rounds > 0
    KeepScreenOn()

    val eased = FastOutSlowInEasing.transform(timer.progress)
    val fullness = when (timer.phase.kind) {
        PhaseKind.BreatheIn -> eased
        PhaseKind.BreatheOut -> 1f - eased
        else -> 1f
    }

    ToolScaffold(
        title = tool.title,
        tip = "Fill your tummy like a balloon.",
        howTo = "Put a hand on your tummy.\n👃 Breathe in through your nose. Feel it get big.\n👄 Breathe out slowly through your mouth.",
        color = tool.deep,
        showTip = !running,
        onBack = onBack,
    ) {
        Spacer(Modifier.weight(1f))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth(0.9f).aspectRatio(0.9f).tapToToggle { running = !running }) {
            val stringColor = MaterialTheme.colorScheme.onSurfaceVariant
            Canvas(Modifier.matchParentSize()) {
                val maxR = size.minDimension * 0.40f
                val minR = maxR * 0.42f
                val r = minR + (maxR - minR) * fullness
                val center = Offset(size.width / 2f, size.height * 0.45f)
                // String
                val bottom = center.y + r
                val string = Path().apply {
                    moveTo(center.x, bottom + 10f)
                    cubicTo(
                        center.x - 30f, bottom + 60f,
                        center.x + 30f, bottom + 110f,
                        center.x, size.height,
                    )
                }
                drawPath(string, stringColor.copy(alpha = 0.5f), style = Stroke(3.dp.toPx()))
                // Knot
                val knot = Path().apply {
                    moveTo(center.x, bottom - 4f)
                    lineTo(center.x - 14f, bottom + 16f)
                    lineTo(center.x + 14f, bottom + 16f)
                    close()
                }
                drawPath(knot, color)
                // Balloon (slightly taller than wide)
                drawOval(
                    color,
                    topLeft = Offset(center.x - r * 0.92f, center.y - r * 1.05f),
                    size = androidx.compose.ui.geometry.Size(r * 1.84f, r * 2.05f),
                )
                // Shine
                drawOval(
                    Color.White.copy(alpha = 0.35f),
                    topLeft = Offset(center.x - r * 0.55f, center.y - r * 0.75f),
                    size = androidx.compose.ui.geometry.Size(r * 0.35f, r * 0.55f),
                )
            }
        }
        PhaseLabel(
            label = if (started) timer.phase.label else "Tap ▶ to start",
            secondsLeft = timer.secondsLeft,
            showCount = running,
        )
        Spacer(Modifier.height(8.dp))
        Text(doneText(timer.rounds, "balloon"), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.weight(1f))
        if (!running) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                balloonColors.forEachIndexed { i, (name, c) ->
                    val chosen = i == colorIndex
                    Box(
                        Modifier
                            .size(if (chosen) 40.dp else 34.dp)
                            .border(if (chosen) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            .padding(if (chosen) 5.dp else 0.dp)
                            .background(c, CircleShape)
                            .semantics { contentDescription = "$name balloon"; selected = chosen }
                            .clickable(role = Role.RadioButton) { colorIndex = i },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        BreathingControls(
            pace = pace, onPace = { pace = it; running = false },
            running = running, started = started, color = tool.color, deep = tool.deep,
            onToggle = { running = !running }, onReset = { running = false; timer.reset() },
        )
    }
}
