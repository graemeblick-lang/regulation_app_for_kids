package com.calmcorner.app.ui.screens

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmcorner.app.data.LocalPrefs
import com.calmcorner.app.data.Tool
import com.calmcorner.app.ui.components.CalmSound
import com.calmcorner.app.ui.components.KeepScreenOn
import com.calmcorner.app.ui.components.NoiseSynth
import com.calmcorner.app.ui.components.PhaseKind
import com.calmcorner.app.ui.components.StartPauseRow
import com.calmcorner.app.ui.components.TimedPhase
import com.calmcorner.app.ui.components.ToolScaffold
import com.calmcorner.app.ui.components.rememberPhaseTimer
import com.calmcorner.app.ui.components.tapToToggle
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun CalmSoundsScreen(onBack: () -> Unit, onFinish: () -> Unit) {
    val tool = Tool.CalmSounds
    val prefs = LocalPrefs.current
    var sound by rememberSaveable { mutableStateOf(CalmSound.Rain) }
    var minutes by rememberSaveable { mutableIntStateOf(5) }
    var running by rememberSaveable { mutableStateOf(false) }
    val phases = remember(minutes) { listOf(TimedPhase("Listen", minutes * 60f, PhaseKind.Hold, spoken = "")) }
    val timer = rememberPhaseTimer(phases, running, loop = false)
    val started = running || timer.progress > 0f
    KeepScreenOn()

    val synth = remember { NoiseSynth() }
    DisposableEffect(Unit) {
        synth.start()
        onDispose { synth.stop() }
    }
    synth.sound = sound
    // Fade out over the last 10 seconds; silent when paused or sound is off.
    val total = minutes * 60f
    val secondsLeft = (1f - timer.progress) * total
    synth.targetVolume = if (running && prefs.soundOn) (secondsLeft / 10f).coerceIn(0f, 1f) else 0f
    LaunchedEffect(timer.finished) { if (timer.finished) onFinish() }

    val wave by rememberInfiniteTransition(label = "scene").animateFloat(
        0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart), label = "scene",
    )
    val t = if (prefs.reduceMotion || !running) 0f else wave

    ToolScaffold(
        title = tool.title,
        tip = "Choose a sound. Close your eyes if you like.",
        howTo = "Pick rain, waves or a soft hum.\nPick how long.\nThe sound gets quiet at the end.",
        color = tool.deep,
        showTip = !running,
        onBack = onBack,
    ) {
        if (!running) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalmSound.entries.forEach { s ->
                    FilterChip(
                        selected = sound == s,
                        onClick = { sound = s },
                        label = { Text("${s.emoji} ${s.label}", style = MaterialTheme.typography.bodyMedium) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = tool.color.copy(alpha = 0.3f)),
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth(0.85f).aspectRatio(1f).tapToToggle { running = !running }) {
            Canvas(Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                drawCircle(tool.color.copy(alpha = 0.15f), w / 2f)
                when (sound) {
                    CalmSound.Rain -> {
                        // Slow falling lines
                        for (i in 0 until 14) {
                            val x = w * (0.15f + 0.7f * ((i * 37) % 14) / 13f)
                            val y = ((t * 3f + i / 14f) % 1f) * h * 0.7f + h * 0.12f
                            drawLine(tool.deep.copy(alpha = 0.45f), Offset(x, y), Offset(x - w * 0.015f, y + h * 0.07f), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
                        }
                    }
                    CalmSound.Waves -> {
                        for (k in 0 until 3) {
                            val path = Path()
                            val base = h * (0.45f + k * 0.12f)
                            for (i in 0..60) {
                                val x = w * i / 60f
                                val y = base + sin(2 * PI * (i / 30.0 + t + k * 0.3)).toFloat() * h * 0.035f
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawPath(path, tool.deep.copy(alpha = 0.5f - k * 0.12f), style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
                        }
                    }
                    CalmSound.Hum -> {
                        val pulse = 0.55f + 0.1f * sin(2 * PI * t).toFloat()
                        drawCircle(tool.color.copy(alpha = 0.35f), w * 0.5f * pulse)
                        drawCircle(tool.color.copy(alpha = 0.35f), w * 0.32f * pulse)
                    }
                }
            }
            if (started) {
                val left = secondsLeft.toInt().coerceAtLeast(0)
                Text("%d:%02d".format(left / 60, left % 60), fontSize = 40.sp, style = MaterialTheme.typography.displaySmall)
            } else {
                Text(sound.emoji, fontSize = 64.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            when {
                !prefs.soundOn -> if (prefs.schoolMode) "Sound is off in school mode." else "Sound is off. Tap 🔊 at the top to hear it."
                running -> "Listen… breathe slowly."
                started -> "Paused"
                else -> "Tap ▶ to start"
            },
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        if (!running) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 20).forEach { m ->
                    FilterChip(
                        selected = minutes == m,
                        onClick = { minutes = m; running = false },
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
