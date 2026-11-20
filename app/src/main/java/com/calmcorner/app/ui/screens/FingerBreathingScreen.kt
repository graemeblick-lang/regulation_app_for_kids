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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.calmcorner.app.data.Tool
import com.calmcorner.app.ui.components.KeepScreenOn
import com.calmcorner.app.ui.components.PaceSelector
import com.calmcorner.app.ui.components.PhaseKind
import com.calmcorner.app.ui.components.StartPauseRow
import com.calmcorner.app.ui.components.TimedPhase
import com.calmcorner.app.ui.components.ToolScaffold
import com.calmcorner.app.ui.components.breathsText
import com.calmcorner.app.ui.components.rememberPhaseBuzz
import com.calmcorner.app.ui.components.rememberPhaseTimer

private val fingerNames = listOf("thumb", "pointer finger", "middle finger", "ring finger", "little finger")

/** Finger geometry as fractions of the canvas: left, right, tip, base. */
private data class FingerSpec(val left: Float, val right: Float, val tip: Float, val base: Float)

private val fingers = listOf(
    FingerSpec(0.06f, 0.21f, 0.40f, 0.82f), // thumb
    FingerSpec(0.25f, 0.39f, 0.10f, 0.56f),
    FingerSpec(0.42f, 0.56f, 0.03f, 0.56f),
    FingerSpec(0.59f, 0.73f, 0.10f, 0.56f),
    FingerSpec(0.76f, 0.88f, 0.24f, 0.56f), // little finger
)

private fun fingerPath(spec: FingerSpec, w: Float, h: Float): Path {
    val l = spec.left * w
    val r = spec.right * w
    val tip = spec.tip * h
    val base = spec.base * h
    val d = r - l
    return Path().apply {
        moveTo(l, base)
        lineTo(l, tip + d / 2f)
        arcTo(Rect(l, tip, r, tip + d), 180f, 180f, false)
        lineTo(r, base)
    }
}

@Composable
fun FingerBreathingScreen(onBack: () -> Unit) {
    val color = Tool.FingerBreathing.color
    var pace by rememberSaveable { mutableIntStateOf(4) }
    var running by rememberSaveable { mutableStateOf(false) }
    val phases = remember(pace) {
        val s = pace.toFloat()
        List(5) { listOf(TimedPhase("Breathe in", s, PhaseKind.BreatheIn), TimedPhase("Breathe out", s, PhaseKind.BreatheOut)) }.flatten()
    }
    val timer = rememberPhaseTimer(phases, running, onPhaseStart = rememberPhaseBuzz())
    KeepScreenOn()

    val fingerIndex = timer.phaseIndex / 2
    val breathingIn = timer.phase.kind == PhaseKind.BreatheIn

    ToolScaffold(
        title = Tool.FingerBreathing.title,
        instructions = "Hold up one hand. Use a finger from your other hand to trace along with the dot: up as you breathe in, down as you breathe out.",
        onBack = onBack,
    ) {
        Spacer(Modifier.weight(1f))
        val skin = MaterialTheme.colorScheme.surfaceVariant
        Canvas(Modifier.fillMaxWidth(0.85f).aspectRatio(0.9f)) {
            val w = size.width
            val h = size.height
            val stroke = 9.dp.toPx()
            // Palm
            drawRoundRect(
                skin,
                topLeft = Offset(0.20f * w, 0.50f * h),
                size = Size(0.70f * w, 0.47f * h),
                cornerRadius = CornerRadius(0.12f * w),
            )
            fingers.forEachIndexed { i, spec ->
                val path = fingerPath(spec, w, h)
                drawPath(path, skin) // filled finger
                when {
                    i < fingerIndex -> {
                        drawPath(path, color.copy(alpha = 0.35f))
                        drawPath(path, color, style = Stroke(stroke, cap = StrokeCap.Round))
                    }
                    i == fingerIndex -> {
                        val measure = PathMeasure().apply { setPath(path, false) }
                        val half = measure.length / 2f
                        val dist = if (breathingIn) timer.progress * half else half + timer.progress * half
                        val traced = Path()
                        measure.getSegment(0f, dist, traced, true)
                        drawPath(traced, color, style = Stroke(stroke, cap = StrokeCap.Round))
                        val dot = measure.getPosition(dist)
                        drawCircle(color.copy(alpha = 0.25f), radius = stroke * 2.4f, center = dot)
                        drawCircle(color, radius = stroke * 1.3f, center = dot)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            if (running || timer.progress > 0f) "${timer.phase.label}…" else "Ready?",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            if (breathingIn) "up your ${fingerNames[fingerIndex]}" else "down your ${fingerNames[fingerIndex]}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(breathsText(timer.rounds, "hand"), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.weight(1f))
        PaceSelector(pace, { pace = it; running = false }, color)
        Spacer(Modifier.height(16.dp))
        StartPauseRow(running, color, onToggle = { running = !running }, onReset = { running = false; timer.reset() })
        Spacer(Modifier.height(8.dp))
    }
}
