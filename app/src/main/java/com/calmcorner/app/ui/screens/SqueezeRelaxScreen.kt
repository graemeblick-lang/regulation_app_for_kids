package com.calmcorner.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.calmcorner.app.data.Tool
import com.calmcorner.app.ui.components.KeepScreenOn
import com.calmcorner.app.ui.components.PhaseKind
import com.calmcorner.app.ui.components.StartPauseRow
import com.calmcorner.app.ui.components.TimedPhase
import com.calmcorner.app.ui.components.ToolScaffold
import androidx.compose.runtime.LaunchedEffect
import com.calmcorner.app.ui.components.rememberCues
import com.calmcorner.app.ui.components.rememberPhaseTimer
import com.calmcorner.app.ui.components.tapToToggle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class BodyPart(val squeeze: String, val relax: String) {
    Hands("Squeeze your hands tight", "Open your hands. Make them soft."),
    Arms("Bend your arms. Make them tight.", "Put your arms down. Make them soft."),
    Shoulders("Lift your shoulders up high", "Put your shoulders down."),
    Face("Squeeze your eyes shut tight!", "Open your eyes. Smile."),
    Tummy("Pull your tummy in tight", "Make your tummy soft."),
    Feet("Push your feet down hard", "Wiggle your toes. Rest."),
}

private val squeezePhases = BodyPart.entries.flatMap {
    listOf(
        TimedPhase("Squeeze", 5f, PhaseKind.Squeeze, spoken = it.squeeze),
        TimedPhase("Relax", 8f, PhaseKind.Relax, spoken = it.relax),
    )
}

@Composable
fun SqueezeRelaxScreen(onBack: () -> Unit, onFinish: () -> Unit) {
    val tool = Tool.SqueezeRelax
    var running by rememberSaveable { mutableStateOf(false) }
    val cues = rememberCues()
    val timer = rememberPhaseTimer(squeezePhases, running, loop = false, onPhaseStart = cues::phase)
    val started = running || timer.progress > 0f || timer.phaseIndex > 0
    KeepScreenOn()
    LaunchedEffect(timer.finished) {
        if (timer.finished) { cues.say("All done"); onFinish() }
    }

    val part = BodyPart.entries[timer.phaseIndex / 2]
    val squeezing = started && !timer.finished && timer.phase.kind == PhaseKind.Squeeze
    // 0 = relaxed pose, 1 = squeezed pose; animated so the body moves smoothly.
    val tension by animateFloatAsState(if (squeezing) 1f else 0f, tween(600), label = "tension")

    ToolScaffold(
        title = tool.title,
        tip = "Squeeze tight. Then relax.",
        howTo = "We go through your body, one part at a time.\n💪 Squeeze for 5 seconds.\n😌 Then make it soft.",
        color = tool.deep,
        showTip = !running && !timer.finished,
        onBack = onBack,
    ) {
        Spacer(Modifier.weight(1f))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth(0.75f).aspectRatio(220f / 260f)
                .tapToToggle { if (!timer.finished) running = !running },
        ) {
            if (started && !timer.finished && part == BodyPart.Face) {
                // Zoom in on the face so the expression is big and easy to copy.
                BigFace(
                    tension = tension,
                    skin = tool.color.copy(alpha = 0.75f),
                    ink = Color(0xFF3A3350),
                    modifier = Modifier.matchParentSize(),
                )
            } else {
                BodyFigure(
                    part = if (started && !timer.finished) part else null,
                    tension = tension,
                    soft = tool.color.copy(alpha = 0.35f),
                    mid = tool.color,
                    deep = tool.deep,
                    face = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        when {
            timer.finished -> {
                Text("All done! 🌈", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "You squeezed and relaxed your whole body.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
            !started -> Text("Tap ▶ to start", style = MaterialTheme.typography.headlineMedium)
            else -> {
                Text(
                    if (squeezing) "Squeeze! ${timer.secondsLeft}" else "Relax…",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    if (squeezing) part.squeeze else part.relax,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { timer.progress },
                    color = tool.deep,
                    trackColor = tool.color.copy(alpha = 0.25f),
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier.fillMaxWidth(0.6f).height(10.dp),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        if (timer.finished) {
            Button(
                onClick = onFinish,
                colors = ButtonDefaults.buttonColors(containerColor = tool.deep, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(60.dp),
            ) { Text("🏠  Home", style = MaterialTheme.typography.labelLarge) }
        } else {
            StartPauseRow(running, started, tool.deep, onToggle = { running = !running }, onDone = { running = false; onFinish() })
        }
        Spacer(Modifier.height(8.dp))
    }
}

/**
 * A simple friendly person. The chosen body part is highlighted; when
 * squeezing it changes pose/size, gets "tension lines" and the face strains.
 * Ported from a browser mock-up that was checked visually.
 */
@Composable
private fun BodyFigure(
    part: BodyPart?,
    tension: Float,
    soft: Color,
    mid: Color,
    deep: Color,
    face: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val u = min(w, h)
        fun pt(x: Float, y: Float) = Offset(x * w, y * h)
        fun lerp(a: Float, b: Float) = a + (b - a) * tension
        fun lerpO(a: Offset, b: Offset) = Offset(lerp(a.x, b.x), lerp(a.y, b.y))
        // Highlight colour shifts from mid (relaxed) to deep (squeezed).
        val hi = Color(
            red = lerp(mid.red, deep.red), green = lerp(mid.green, deep.green),
            blue = lerp(mid.blue, deep.blue), alpha = 1f,
        )
        fun col(p: BodyPart) = if (p == part) hi else soft
        val isPart = { p: BodyPart -> p == part }

        val lift = if (isPart(BodyPart.Shoulders)) lerp(0f, 0.045f) else 0f

        // Legs
        listOf(floatArrayOf(.44f, .62f, .42f, .86f), floatArrayOf(.56f, .62f, .58f, .86f)).forEach { l ->
            drawLine(soft, pt(l[0], l[1]), pt(l[2], l[3]), strokeWidth = 0.085f * u, cap = StrokeCap.Round)
        }
        // Feet
        val footRx = if (isPart(BodyPart.Feet)) lerp(0.10f, 0.085f) else 0.10f
        val feet = listOf(pt(.395f, .905f), pt(.605f, .905f))
        feet.forEach { c ->
            drawOval(col(BodyPart.Feet), topLeft = Offset(c.x - footRx * w, c.y - 0.035f * h), size = Size(footRx * 2 * w, 0.07f * h))
        }

        // Arms: shoulder -> elbow -> hand. Squeezing arms = "strong arms" flex pose.
        val armT = if (isPart(BodyPart.Arms)) tension else 0f
        fun armLerp(a: Offset, b: Offset) = Offset(a.x + (b.x - a.x) * armT, a.y + (b.y - a.y) * armT)
        val sh = pt(.385f, .36f - lift)
        val elbow = armLerp(pt(.31f, .48f - lift), pt(.22f, .40f - lift))
        val hand = armLerp(pt(.27f, .60f - lift), pt(.24f, .25f - lift))
        fun mirror(o: Offset) = Offset(w - o.x, o.y)
        val arms = listOf(listOf(sh, elbow, hand), listOf(mirror(sh), mirror(elbow), mirror(hand)))
        arms.forEach { a ->
            val path = Path().apply { moveTo(a[0].x, a[0].y); lineTo(a[1].x, a[1].y); lineTo(a[2].x, a[2].y) }
            drawPath(path, col(BodyPart.Arms), style = Stroke(0.07f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        // Hands: shrink into fists when squeezing
        val handR = (if (isPart(BodyPart.Hands)) lerp(0.046f, 0.036f) else 0.046f) * u
        arms.forEach { a -> drawCircle(col(BodyPart.Hands), handR, a[2]) }

        // Torso (rises a little when shoulders squeeze)
        val torsoTop = (.31f - lift * 0.6f) * h
        val torso = RoundRect(Rect(.36f * w, torsoTop, .64f * w, torsoTop + (.33f + lift * 0.6f) * h), CornerRadius(0.08f * u))
        val torsoPath = Path().apply { addRoundRect(torso) }
        drawPath(torsoPath, soft)
        if (isPart(BodyPart.Shoulders)) {
            clipPath(torsoPath) {
                drawRect(hi, topLeft = Offset(.30f * w, (.28f - lift) * h), size = Size(.40f * w, .075f * h))
            }
        }
        if (isPart(BodyPart.Tummy)) {
            val r = lerp(0.09f, 0.075f) * u
            drawOval(hi, topLeft = Offset(.5f * w - r, .51f * h - r * 0.9f), size = Size(r * 2, r * 1.8f))
        }

        // Head + face
        val head = pt(.5f, .18f)
        // The face stays a lighter shade so features remain clear and friendly.
        drawCircle(if (isPart(BodyPart.Face)) mid else soft, 0.11f * u, head)
        drawFace(head, u, w, h, faceSqueeze = isPart(BodyPart.Face) && tension > 0.5f, straining = part != null && tension > 0.5f,
            ink = face)

        // Tension lines around the squeezed part
        if (part == BodyPart.Face && tension > 0.05f) {
            // Soft "squeeze" marks either side of the head (no spikes around the face).
            val arcColor = deep.copy(alpha = tension)
            listOf(0.15f, 0.185f).forEach { r ->
                val rr = r * u
                val tl = Offset(head.x - rr, head.y - rr)
                val sz = Size(rr * 2, rr * 2)
                drawArc(arcColor, 153f, 54f, false, tl, sz, style = Stroke(0.013f * u, cap = StrokeCap.Round))
                drawArc(arcColor, -27f, 54f, false, tl, sz, style = Stroke(0.013f * u, cap = StrokeCap.Round))
            }
        } else if (part != null && tension > 0.05f) {
            val centres: List<Offset> = when (part) {
                BodyPart.Hands -> arms.map { it[2] }
                BodyPart.Arms -> arms.map { it[1] }
                BodyPart.Shoulders -> listOf(pt(.33f, .33f - lift), pt(.67f, .33f - lift))
                BodyPart.Face -> listOf(head)
                BodyPart.Tummy -> listOf(pt(.5f, .51f))
                BodyPart.Feet -> feet
            }
            val rad = when (part) {
                BodyPart.Face -> .15f; BodyPart.Tummy -> .12f; BodyPart.Feet -> .08f
                BodyPart.Shoulders -> .06f; else -> .07f
            } * u
            val lineColor = deep.copy(alpha = tension)
            centres.forEach { c ->
                for (i in 0 until 8) {
                    val a = i * PI.toFloat() / 4f + 0.4f
                    val d = Offset(cos(a), sin(a))
                    drawLine(lineColor, c + d * rad, c + d * (rad + 0.03f * u), strokeWidth = 0.012f * u, cap = StrokeCap.Round)
                }
            }
        }
    }
}

private fun DrawScope.drawFace(head: Offset, u: Float, w: Float, h: Float, faceSqueeze: Boolean, straining: Boolean, ink: Color) {
    val ex = 0.04f * w
    val ey = head.y - 0.015f * h
    val sw = 0.012f * u
    val mouthY = head.y + 0.05f * h
    when {
        faceSqueeze -> {
            // Friendly scrunched face: rosy cheeks, eyes squeezed shut, little pucker.
            val cheek = Color(0xD9FFAABE)
            listOf(-1f, 1f).forEach { s ->
                val cx = head.x + s * 0.062f * w
                val cy = head.y + 0.025f * h
                drawOval(cheek, topLeft = Offset(cx - 0.022f * u, cy - 0.015f * u), size = Size(0.044f * u, 0.030f * u))
            }
            val er = 0.02f * u
            listOf(-1f, 1f).forEach { s ->
                val cx = head.x + s * ex
                val cy = ey - 0.012f * u
                drawArc(ink, startAngle = 45f, sweepAngle = 90f, useCenter = false,
                    topLeft = Offset(cx - er, cy - er), size = Size(er * 2, er * 2), style = Stroke(0.014f * u, cap = StrokeCap.Round))
            }
            val mx = 0.014f * u
            val my = 0.017f * u
            drawOval(ink, topLeft = Offset(head.x - mx, mouthY + 0.002f * h - my), size = Size(mx * 2, my * 2), style = Stroke(sw))
        }
        straining -> {
            // Concentrating: dot eyes, flat mouth
            drawCircle(ink, 0.011f * u, Offset(head.x - ex, ey))
            drawCircle(ink, 0.011f * u, Offset(head.x + ex, ey))
            drawLine(ink, Offset(head.x - 0.03f * w, mouthY), Offset(head.x + 0.03f * w, mouthY), strokeWidth = sw, cap = StrokeCap.Round)
        }
        else -> {
            // Relaxed: happy closed eyes and a smile
            val er = 0.014f * u
            listOf(-1f, 1f).forEach { s ->
                drawArc(ink, startAngle = 198f, sweepAngle = 144f, useCenter = false,
                    topLeft = Offset(head.x + s * ex - er, ey - er), size = Size(er * 2, er * 2), style = Stroke(sw, cap = StrokeCap.Round))
            }
            val mr = 0.035f * u
            val my = head.y + 0.025f * h
            drawArc(ink, startAngle = 36f, sweepAngle = 108f, useCenter = false,
                topLeft = Offset(head.x - mr, my - mr), size = Size(mr * 2, mr * 2), style = Stroke(sw, cap = StrokeCap.Round))
        }
    }
}

/**
 * Close-up face for the "face" step: a tight scrunch when squeezing
 * (eyes shut tight with crinkles, wrinkled nose, puckered lips) and a soft
 * smile when relaxing. Ported from a browser mock-up that was checked visually.
 */
@Composable
private fun BigFace(tension: Float, skin: Color, ink: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val u = min(size.width, size.height)
        val cx = size.width / 2f
        val cy = size.height * 0.52f
        val r = 0.40f * u
        val line = Stroke(0.025f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val thin = Stroke(0.016f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawCircle(skin, r, Offset(cx, cy))

        // Rosy cheeks, pushed up a little when scrunching
        val cheek = Color(0xFFFFA0B9).copy(alpha = 0.45f + 0.4f * tension)
        listOf(-1f, 1f).forEach { s ->
            val c = Offset(cx + s * 0.20f * u, cy + (0.09f - 0.03f * tension) * u)
            drawOval(cheek, topLeft = Offset(c.x - 0.06f * u, c.y - 0.04f * u), size = Size(0.12f * u, 0.08f * u))
        }

        val ex = 0.14f * u
        val ey = cy - 0.06f * u
        if (tension > 0.5f) {
            listOf(-1f, 1f).forEach { s ->
                val x = cx + s * ex
                val a = 0.045f * u
                val b = 0.035f * u
                // Eyes squeezed shut: > <
                drawPath(Path().apply { moveTo(x + s * a, ey - b); lineTo(x - s * a, ey); lineTo(x + s * a, ey + b) }, ink, style = line)
                // Crinkles at the outer corners
                listOf(-1f, 0f, 1f).forEach { k ->
                    drawLine(ink, Offset(x + s * 0.07f * u, ey + k * 0.03f * u), Offset(x + s * 0.10f * u, ey + k * 0.045f * u),
                        strokeWidth = 0.016f * u, cap = StrokeCap.Round)
                }
                // Eyebrows pulled down, kept level so it doesn't look cross
                drawLine(ink, Offset(cx + s * 0.07f * u, ey - 0.085f * u), Offset(cx + s * 0.20f * u, ey - 0.085f * u),
                    strokeWidth = 0.025f * u, cap = StrokeCap.Round)
            }
            // Wrinkled nose
            listOf(0f, 1f).forEach { k ->
                val y = cy + k * 0.025f * u
                drawPath(Path().apply { moveTo(cx - 0.03f * u, y); quadraticTo(cx, y - 0.012f * u, cx + 0.03f * u, y) }, ink, style = thin)
            }
            // Puckered lips
            drawOval(Color(0xFFC2577A), topLeft = Offset(cx - 0.035f * u, cy + 0.14f * u), size = Size(0.07f * u, 0.06f * u))
        } else {
            listOf(-1f, 1f).forEach { s ->
                // Soft closed happy eyes
                val er = 0.045f * u
                drawArc(ink, 207f, 126f, false, Offset(cx + s * ex - er, ey - er), Size(er * 2, er * 2), style = line)
                // Relaxed raised eyebrows
                drawPath(Path().apply {
                    moveTo(cx + s * 0.08f * u, ey - 0.12f * u)
                    quadraticTo(cx + s * 0.14f * u, ey - 0.15f * u, cx + s * 0.20f * u, ey - 0.12f * u)
                }, ink, style = line)
            }
            // Big smile
            val mr = 0.10f * u
            drawArc(ink, 36f, 108f, false, Offset(cx - mr, cy + 0.08f * u - mr), Size(mr * 2, mr * 2), style = line)
        }
    }
}
