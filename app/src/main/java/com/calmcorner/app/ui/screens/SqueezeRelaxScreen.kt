package com.calmcorner.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmcorner.app.data.Tool
import com.calmcorner.app.ui.components.KeepScreenOn
import com.calmcorner.app.ui.components.PhaseKind
import com.calmcorner.app.ui.components.StartPauseRow
import com.calmcorner.app.ui.components.TimedPhase
import com.calmcorner.app.ui.components.ToolScaffold
import com.calmcorner.app.ui.components.rememberPhaseBuzz
import com.calmcorner.app.ui.components.rememberPhaseTimer

private data class BodyPart(val emoji: String, val squeeze: String, val relax: String)

private val bodyParts = listOf(
    BodyPart("✊", "Squeeze your hands into tight fists, like squeezing a lemon", "Let go… drop the lemon. Feel your hands go soft"),
    BodyPart("💪", "Make strong arms, pull your elbows in tight", "Let your arms flop down like wet spaghetti"),
    BodyPart("🐢", "Lift your shoulders up to your ears like a turtle hiding", "Drop your shoulders down. The turtle comes out"),
    BodyPart("😖", "Scrunch up your whole face, like you smelled something yucky", "Let your face go smooth and floppy"),
    BodyPart("🐘", "Make your tummy hard, like an elephant might step on it", "Elephant's gone! Let your tummy relax"),
    BodyPart("🦶", "Push your toes down into the floor, like squishing mud", "Wiggle your toes and let them rest"),
)

private val squeezePhases = bodyParts.flatMap {
    listOf(
        TimedPhase("Squeeze", 5f, PhaseKind.Squeeze),
        TimedPhase("Relax", 8f, PhaseKind.Relax),
    )
}

@Composable
fun SqueezeRelaxScreen(onBack: () -> Unit) {
    val color = Tool.SqueezeRelax.color
    var running by rememberSaveable { mutableStateOf(false) }
    val timer = rememberPhaseTimer(squeezePhases, running, loop = false, onPhaseStart = rememberPhaseBuzz())
    KeepScreenOn()

    val part = bodyParts[timer.phaseIndex / 2]
    val squeezing = timer.phase.kind == PhaseKind.Squeeze
    val started = running || timer.progress > 0f || timer.phaseIndex > 0
    val scale by animateFloatAsState(
        targetValue = if (!started || timer.finished) 1f else if (squeezing) 0.75f else 1.15f,
        animationSpec = tween(900),
        label = "squeeze-scale",
    )

    ToolScaffold(
        title = Tool.SqueezeRelax.title,
        instructions = "Squeeze each part of your body tight for 5 seconds, then let it go completely. Notice how different relaxed feels.",
        onBack = onBack,
    ) {
        Spacer(Modifier.weight(1f))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
            CircularProgressIndicator(
                progress = { if (timer.finished) 1f else timer.progress },
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeWidth = 12.dp,
                modifier = Modifier.size(240.dp),
            )
            Text(if (timer.finished) "🌈" else part.emoji, fontSize = 96.sp, modifier = Modifier.scale(scale))
        }
        Spacer(Modifier.height(24.dp))
        when {
            timer.finished -> {
                Text("All done!", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Your whole body has had a squeeze and a rest. Notice how calm and heavy it feels.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
            !started -> Text("Ready when you are", style = MaterialTheme.typography.headlineMedium)
            else -> {
                Text(
                    if (squeezing) "Squeeze… ${timer.secondsLeft}" else "Relax…",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (squeezing) part.squeeze else part.relax,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        if (timer.finished) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text("Back to Calm Corner", style = MaterialTheme.typography.labelLarge) }
        } else {
            StartPauseRow(running, color, onToggle = { running = !running }, onReset = { running = false; timer.reset() })
        }
        Spacer(Modifier.height(8.dp))
    }
}
