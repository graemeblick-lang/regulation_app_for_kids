package com.calmcorner.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.calmcorner.app.data.Tool
import com.calmcorner.app.ui.components.KeepScreenOn
import com.calmcorner.app.ui.components.PhaseKind
import com.calmcorner.app.ui.components.Pose
import com.calmcorner.app.ui.components.PoseFigure
import com.calmcorner.app.ui.components.PosePart
import com.calmcorner.app.ui.components.Prop
import com.calmcorner.app.ui.components.StartPauseRow
import com.calmcorner.app.ui.components.TimedPhase
import com.calmcorner.app.ui.components.ToolScaffold
import com.calmcorner.app.ui.components.rememberCues
import com.calmcorner.app.ui.components.rememberPhaseTimer
import com.calmcorner.app.ui.components.tapToToggle

/** One "heavy work" move: a rest pose, a push pose, and plain literal words. */
private class Move(
    val title: String,
    val push: String,
    val rest: String,
    val restPose: Pose,
    val pushPose: Pose,
    val active: Set<PosePart>,
    val props: Set<Prop>,
)

private fun o(x: Float, y: Float) = Offset(x, y)

private val standing = Pose(o(.5f, .2f), o(.5f, .30f), o(.5f, .56f), o(.415f, .32f), o(.40f, .44f), o(.38f, .56f), o(.46f, .73f), o(.45f, .91f)).mirrored()

private val moves = listOf(
    Move(
        "Wall push", "Push the wall hard", "Rest",
        Pose(o(.66f, .22f), o(.62f, .30f), o(.48f, .58f), o(.62f, .32f), o(.71f, .34f), o(.80f, .36f), o(.42f, .74f), o(.36f, .91f), kneeR = o(.44f, .74f), footR = o(.40f, .91f)),
        Pose(o(.74f, .25f), o(.70f, .33f), o(.53f, .59f), o(.70f, .35f), o(.72f, .45f), o(.80f, .37f), o(.45f, .75f), o(.36f, .91f), kneeR = o(.47f, .75f), footR = o(.40f, .91f)),
        setOf(PosePart.Arms, PosePart.Hands), setOf(Prop.Wall, Prop.Floor),
    ),
    Move(
        "Hand press", "Press your hands together hard", "Rest",
        standing.copy(elL = o(.37f, .44f), haL = o(.48f, .40f)).mirrored(),
        standing.copy(elL = o(.32f, .38f), haL = o(.49f, .38f)).mirrored(),
        setOf(PosePart.Arms, PosePart.Hands), setOf(Prop.Floor),
    ),
    Move(
        "Chair push", "Push down and lift your body", "Sit down",
        Pose(o(.48f, .21f), o(.47f, .30f), o(.47f, .58f), o(.47f, .33f), o(.45f, .46f), o(.42f, .61f), o(.64f, .60f), o(.66f, .90f)),
        Pose(o(.48f, .13f), o(.47f, .22f), o(.47f, .50f), o(.47f, .25f), o(.445f, .43f), o(.42f, .61f), o(.64f, .56f), o(.66f, .90f)),
        setOf(PosePart.Arms, PosePart.Hands), setOf(Prop.Chair, Prop.Floor),
    ),
    Move(
        "Big hug", "Hug yourself tight", "Open your arms",
        standing.copy(elL = o(.31f, .34f), haL = o(.20f, .28f)).mirrored(),
        standing.copy(elL = o(.38f, .45f), haL = o(.58f, .34f)).mirrored(),
        setOf(PosePart.Arms, PosePart.Hands), setOf(Prop.Floor),
    ),
    Move(
        "Big stretch", "Reach up high", "Arms down",
        standing,
        Pose(o(.5f, .17f), o(.5f, .27f), o(.5f, .53f), o(.415f, .29f), o(.42f, .16f), o(.42f, .04f), o(.46f, .71f), o(.46f, .89f)).mirrored(),
        setOf(PosePart.Arms, PosePart.Hands), setOf(Prop.Floor),
    ),
)

private const val REPS = 5

private val movePhases = moves.flatMap { m ->
    (1..REPS).flatMap { rep ->
        listOf(
            TimedPhase("Push", 2.5f, PhaseKind.Squeeze, spoken = if (rep == 1) m.push else "$rep"),
            TimedPhase("Rest", 2f, PhaseKind.Relax, spoken = if (rep == 1) m.rest else ""),
        )
    }
}

@Composable
fun BodyMovesScreen(onBack: () -> Unit, onFinish: () -> Unit) {
    val tool = Tool.BodyMoves
    val cues = rememberCues()
    var running by rememberSaveable { mutableStateOf(false) }
    val timer = rememberPhaseTimer(movePhases, running, loop = false, onPhaseStart = cues::phase)
    val started = running || timer.progress > 0f || timer.phaseIndex > 0
    KeepScreenOn()
    LaunchedEffect(timer.finished) {
        if (timer.finished) { cues.say("All done"); onFinish() }
    }

    val perMove = REPS * 2
    val moveIndex = (timer.phaseIndex / perMove).coerceAtMost(moves.lastIndex)
    val rep = (timer.phaseIndex % perMove) / 2 + 1
    val move = moves[moveIndex]
    val pushing = started && timer.phase.kind == PhaseKind.Squeeze
    val t by animateFloatAsState(if (pushing) 1f else 0f, tween(500), label = "pose")

    ToolScaffold(
        title = tool.title,
        tip = "Push, press and stretch. It helps your body feel calm.",
        howTo = "Copy the picture.\nPush hard for a few seconds.\nThen rest.\nWe do each move 5 times.",
        color = tool.deep,
        showTip = !running,
        onBack = onBack,
    ) {
        Text(
            "${moveIndex + 1} of ${moves.size}: ${move.title}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth(0.8f).aspectRatio(1f).tapToToggle { running = !running }) {
            PoseFigure(
                pose = move.restPose.lerp(move.pushPose, t),
                active = if (started) move.active else emptySet(),
                props = move.props,
                body = tool.color.copy(alpha = 0.30f),
                limb = tool.color.copy(alpha = 0.55f),
                deep = tool.deep,
                prop = Color(0xFFD9CBB5),
                tension = if (move.title == "Hand press") t else 0f,
                modifier = Modifier.matchParentSize(),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            when {
                !started -> "Tap ▶ to start"
                pushing -> "${move.push}! ${timer.secondsLeft}"
                else -> move.rest
            },
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        // One dot per push in this move
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            for (i in 1..REPS) {
                val done = started && (i < rep || (i == rep && !pushing && timer.phaseIndex % 2 == 1))
                Box(Modifier.size(14.dp).background(if (done) tool.deep else tool.color.copy(alpha = 0.3f), CircleShape))
            }
        }
        Spacer(Modifier.weight(1f))
        StartPauseRow(running, started, tool.deep, onToggle = { running = !running }, onDone = { running = false; onFinish() })
        Spacer(Modifier.height(8.dp))
    }
}
