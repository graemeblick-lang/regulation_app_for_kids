package com.calmcorner.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.calmcorner.app.R
import com.calmcorner.app.data.LocalPrefs
import com.calmcorner.app.data.Rhythm
import com.calmcorner.app.data.Tool
import com.calmcorner.app.ui.components.KeepScreenOn
import com.calmcorner.app.ui.components.ToolScaffold
import com.calmcorner.app.ui.components.rememberCues
import com.calmcorner.app.ui.components.rememberSoundBank
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private enum class DrumStage { Find, Follow, Breathe, Done }

private const val BREATH_CYCLE = 6   // beats: in for 2, out for 4
private const val BREATH_CYCLES = 4
private const val FALLBACK_SECONDS = 8f

/** Everything the drum needs, updated every frame. */
private class DrumState {
    var time by mutableFloatStateOf(0f)
    var stage by mutableStateOf(DrumStage.Find)
    val taps = mutableListOf<Float>()
    var startBpm = Rhythm.DEFAULT_START_BPM
    var bpm = Rhythm.DEFAULT_START_BPM
    var beatIndex = 0                 // beats while slowing
    var breathBeat by mutableIntStateOf(0)
    var nextBeat = 0f
    var lastBeat = -10f
    var lastTap = -10f
    var lastTapPos = Offset.Zero
    var glow = -10f                   // last time a tap landed on the beat
}

/**
 * A steady drum for one hand. It listens to her own beat first, joins in at
 * her speed, then slowly slows down, ending with breathing in time with the drum.
 * Deliberately not a game: no score, no misses, a short fixed session that
 * always ends slow, then the usual check-in.
 */
@Composable
fun CalmDrumScreen(onBack: () -> Unit, onFinish: () -> Unit) {
    val tool = Tool.CalmDrum
    val prefs = LocalPrefs.current
    val cues = rememberCues()
    val sounds = rememberSoundBank(R.raw.drum, R.raw.tick)
    val s = remember { DrumState() }
    val target = prefs.drumEndBpm.toFloat()
    val slowBeats = prefs.drumSlowBeats()
    KeepScreenOn()

    fun startFollowing(bpm: Float, now: Float) {
        s.startBpm = bpm
        s.bpm = bpm
        s.beatIndex = 0
        s.nextBeat = now + 60f / bpm // land with her next tap
        s.stage = DrumStage.Follow
        cues.say("Follow the drum")
    }

    fun onBeat() {
        s.lastBeat = s.nextBeat
        if (prefs.voiceOn && prefs.drumTick) sounds.play(R.raw.tick, 0.5f)
        cues.buzz() // lets her feel the beat even with sound off (e.g. on the bus)
        when (s.stage) {
            DrumStage.Follow -> {
                s.beatIndex++
                if (s.beatIndex == slowBeats / 2) cues.say("Slower")
                s.bpm = Rhythm.tempoAt(s.beatIndex, s.startBpm, target, slowBeats)
                if (s.beatIndex >= slowBeats) { s.stage = DrumStage.Breathe; s.breathBeat = 0 }
            }
            DrumStage.Breathe -> {
                when (s.breathBeat % BREATH_CYCLE) {
                    0 -> cues.say("Breathe in")
                    2 -> cues.say("Breathe out")
                }
                s.breathBeat++
                if (s.breathBeat >= BREATH_CYCLE * BREATH_CYCLES) {
                    s.stage = DrumStage.Done
                    cues.say("All done")
                }
            }
            else -> {}
        }
        s.nextBeat += 60f / s.bpm
    }

    // Clock + beat scheduler.
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (isActive && s.stage != DrumStage.Done) {
            withFrameNanos { now -> s.time = (now - start) / 1_000_000_000f }
            when (s.stage) {
                DrumStage.Find -> if (s.time > FALLBACK_SECONDS && s.taps.size < 6) {
                    // Not enough taps yet: start a gentle beat for her.
                    startFollowing(Rhythm.estimateBpm(s.taps, minTaps = 3) ?: Rhythm.DEFAULT_START_BPM, s.time)
                }
                else -> if (s.time >= s.nextBeat) onBeat()
            }
        }
    }
    LaunchedEffect(s.stage) {
        if (s.stage == DrumStage.Done) onFinish()
    }

    ToolScaffold(
        title = tool.title,
        tip = "Tap the drum. Find your steady beat.",
        howTo = "Tap the drum with one hand.\nKeep a steady beat.\nThe drum joins in, then slowly gets slower.\nFollow it, then breathe with it.",
        color = tool.deep,
        showTip = s.stage == DrumStage.Find,
        onBack = onBack,
    ) {
        Spacer(Modifier.weight(1f))
        Canvas(
            Modifier
                .fillMaxWidth(0.85f)
                .aspectRatio(1f)
                .semantics { contentDescription = "Drum. Tap to play a beat." }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown() // react on touch-down for tight timing
                        val now = s.time
                        s.lastTap = now
                        s.lastTapPos = down.position
                        if (prefs.voiceOn) sounds.play(R.raw.drum, 0.9f)
                        when (s.stage) {
                            DrumStage.Find -> {
                                s.taps.add(now)
                                Rhythm.estimateBpm(s.taps)?.let { startFollowing(it, now) }
                            }
                            DrumStage.Follow, DrumStage.Breathe -> {
                                val nearest = if (s.nextBeat - now < now - s.lastBeat) s.nextBeat else s.lastBeat
                                if (Rhythm.inTime(now, nearest, s.bpm)) s.glow = now
                            }
                            else -> {}
                        }
                    }
                },
        ) {
            val t = s.time
            val c = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.minDimension / 2f
            // During breathing the drum gently grows (in) and shrinks (out).
            val breathScale = if (s.stage == DrumStage.Breathe || s.stage == DrumStage.Done) {
                val interval = 60f / s.bpm
                val frac = ((t - s.lastBeat) / interval).coerceIn(0f, 1f)
                val pos = ((s.breathBeat - 1 + BREATH_CYCLE) % BREATH_CYCLE + frac) / BREATH_CYCLE * 3f // 0..3 (in = 0..1, out = 1..3)
                if (pos < 1f) 0.86f + 0.12f * pos else 0.98f - 0.12f * ((pos - 1f) / 2f)
            } else 0.92f
            val r = maxR * 0.82f * breathScale

            // Guide beat: a soft ring that swells out from the drum on each beat.
            val bp = (t - s.lastBeat) / 0.7f
            if (bp in 0f..1f) {
                drawCircle(tool.deep.copy(alpha = 0.45f * (1f - bp)), r * (1.03f + 0.18f * bp), c, style = Stroke(10.dp.toPx() * (1f - bp) + 1f))
            }
            drawHandDrum(c, r)
            val skinR = r * 0.82f
            val skin = Path().apply { addOval(Rect(c, skinR)) }
            clipPath(skin) {
                // Beat flash: the skin brightens softly on each guide beat
                if (bp in 0f..0.4f) drawCircle(Color.White.copy(alpha = 0.28f * (1f - bp / 0.4f)), skinR, c)
                // Subtle glow when her tap lands in time (no score, just a nice feeling)
                val gp = (t - s.glow) / 0.35f
                if (gp in 0f..1f) drawCircle(Color.White.copy(alpha = 0.30f * (1f - gp)), skinR, c)
                // Where her finger hits: the skin dips (a soft dark spot that springs back)...
                val tp = (t - s.lastTap) / 0.6f
                if (tp in 0f..1f) {
                    val dentR = r * 0.30f * (1f - 0.4f * tp)
                    drawCircle(
                        Brush.radialGradient(
                            0f to Color(0xFF784F28).copy(alpha = 0.32f * (1f - tp)),
                            1f to Color.Transparent,
                            center = s.lastTapPos, radius = dentR,
                        ),
                        dentR, s.lastTapPos,
                    )
                    // ...and two ripples spread across the skin.
                    listOf(0f, 0.18f).forEach { delay ->
                        val q = (tp - delay) / 0.82f
                        if (q in 0f..1f) {
                            drawCircle(
                                Color(0xFF96693C).copy(alpha = 0.45f * (1f - q)),
                                r * 0.9f * q + 3f, s.lastTapPos,
                                style = Stroke(4.dp.toPx() * (1f - q) + 1f),
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        val breathIn = s.stage == DrumStage.Breathe && ((s.breathBeat - 1 + BREATH_CYCLE) % BREATH_CYCLE) < 2
        Text(
            when (s.stage) {
                DrumStage.Find -> if (s.taps.isEmpty()) "Tap the drum" else "Keep a steady beat…"
                DrumStage.Follow -> if (s.beatIndex < slowBeats / 2) "Follow the drum" else "Slower… slower…"
                DrumStage.Breathe -> if (s.breathBeat == 0) "Get ready to breathe…" else if (breathIn) "Breathe in…" else "Breathe out…"
                DrumStage.Done -> "All done ⭐"
            },
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        if (s.stage == DrumStage.Breathe) {
            Text("Keep tapping, or just breathe.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.weight(1f))
        if (s.stage != DrumStage.Find) {
            OutlinedButton(onClick = onFinish, modifier = Modifier.height(56.dp).width(160.dp)) {
                Text("✓ Done", style = MaterialTheme.typography.labelLarge, color = tool.deep)
            }
        } else {
            Spacer(Modifier.height(56.dp))
        }
        Spacer(Modifier.height(8.dp))
    }
}

/**
 * A wooden hand drum seen from above: shadow, wood rim with grain,
 * real-looking skin and metal tacks. [r] is the outer radius of the rim.
 */
private fun DrawScope.drawHandDrum(c: Offset, r: Float) {
    // Shadow
    drawOval(Color.Black.copy(alpha = 0.15f), topLeft = Offset(c.x - r * 0.95f + 6f, c.y + r * 0.98f - r * 0.16f), size = Size(r * 1.9f, r * 0.32f))
    // Wooden rim
    drawCircle(
        Brush.radialGradient(
            0f to Color(0xFFD9A066), 0.8f to Color(0xFFA9693A), 1f to Color(0xFF7A4A28),
            center = Offset(c.x - r * 0.3f, c.y - r * 0.3f), radius = r * 1.35f,
        ),
        r, c,
    )
    for (i in 0 until 5) {
        drawCircle(Color(0xFF5A3214).copy(alpha = 0.18f), r * (0.88f + i * 0.022f), c, style = Stroke(1.5f))
    }
    // Skin
    val skinR = r * 0.82f
    drawCircle(
        Brush.radialGradient(
            0f to Color(0xFFFFF8EC), 0.7f to Color(0xFFF1E2C6), 1f to Color(0xFFD8C29C),
            center = Offset(c.x - r * 0.25f, c.y - r * 0.3f), radius = skinR * 1.25f,
        ),
        skinR, c,
    )
    // Metal tacks around the rim
    val tackR = r * 0.034f
    for (i in 0 until 16) {
        val a = i / 16f * 2f * PI.toFloat()
        val tc = Offset(c.x + cos(a) * r * 0.91f, c.y + sin(a) * r * 0.91f)
        drawCircle(
            Brush.radialGradient(0f to Color.White, 1f to Color(0xFF9A9A9A), center = Offset(tc.x - tackR * 0.4f, tc.y - tackR * 0.4f), radius = tackR * 1.2f),
            tackR, tc,
        )
    }
}
