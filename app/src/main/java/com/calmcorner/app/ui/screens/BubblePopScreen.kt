package com.calmcorner.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.calmcorner.app.R
import com.calmcorner.app.data.LocalPrefs
import com.calmcorner.app.data.Tool
import com.calmcorner.app.ui.components.ToolScaffold
import com.calmcorner.app.ui.components.rememberCues
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

private const val COLS = 5
private const val ROWS = 6
private const val TOTAL = COLS * ROWS
private const val BURST_SECONDS = 0.55f
private const val SQUISH = 0.14f // first part of the pop: a quick squish

private val rowColors = listOf(
    Color(0xFFE57B8D), Color(0xFFF0954A), Color(0xFFF2C94C),
    Color(0xFF5DB37E), Color(0xFF5B9BE0), Color(0xFF9C7BD6),
)

/** All bubble state in one place; drawn every frame on a single canvas. */
private class BubbleField {
    var time by mutableFloatStateOf(0f)              // seconds since the screen opened
    var left by mutableIntStateOf(TOTAL)
    val popped = BooleanArray(TOTAL)
    val popTime = FloatArray(TOTAL) { -10f }
    val spawnTime = FloatArray(TOTAL)
    // Random phases/speeds so every bubble drifts differently.
    val ph = FloatArray(TOTAL * 4) { Random.nextFloat() }

    /** Grown-up setting: 0 = still, 1 = calm, 2 = lively. */
    var motion = 1

    /** How far bubbles drift: a little at first, more as fewer are left. */
    fun driftAmount(cell: Float): Float {
        val gone = 1f - left.toFloat() / TOTAL
        val scale = when (motion) { 0 -> 0f; 2 -> 1.7f; else -> 1f }
        return cell * (0.035f + 0.17f * gone) * scale
    }

    fun centre(i: Int, cell: Float): Offset {
        val c = i % COLS
        val r = i / COLS
        val amp = driftAmount(cell)
        val t = time
        val dx = amp * sin(t * (0.55f + ph[i * 4] * 0.5f) + ph[i * 4 + 1] * 2f * PI.toFloat())
        val dy = amp * cos(t * (0.45f + ph[i * 4 + 2] * 0.5f) + ph[i * 4 + 3] * 2f * PI.toFloat())
        return Offset((c + 0.5f) * cell + dx, (r + 0.5f) * cell + dy)
    }

    fun refill() {
        for (i in 0 until TOTAL) {
            popped[i] = false
            popTime[i] = -10f
            spawnTime[i] = time + i * 0.03f // bounce back in one after another
        }
        left = TOTAL
    }
}

@Composable
fun BubblePopScreen(onBack: () -> Unit, onFinish: () -> Unit) {
    val tool = Tool.BubblePop
    val cues = rememberCues()
    val prefs = LocalPrefs.current
    val field = remember { BubbleField() }
    field.motion = if (prefs.reduceMotion) 0 else prefs.bubbleMotion
    val popSound = rememberPopSound()
    val allPopped = field.left == 0

    // Animation clock.
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (isActive) {
            withFrameNanos { now -> field.time = (now - start) / 1_000_000_000f }
        }
    }

    ToolScaffold(
        title = tool.title,
        tip = "Pop the bubbles. Go slowly.",
        howTo = "Tap one bubble at a time.\nTake a slow breath between pops.",
        color = tool.deep,
        showTip = field.left == TOTAL,
        onBack = onBack,
    ) {
        Spacer(Modifier.weight(1f))
        Canvas(
            Modifier
                .fillMaxWidth(0.95f)
                .aspectRatio(COLS.toFloat() / ROWS)
                .semantics { contentDescription = "${field.left} bubbles left. Tap a bubble to pop it." }
                .pointerInput(Unit) {
                    detectTapGestures { pos ->
                        val cell = size.width.toFloat() / COLS
                        val r = cell * 0.40f
                        // Pop the closest bubble under the finger (generous hit area).
                        var best = -1
                        var bestD = Float.MAX_VALUE
                        for (i in 0 until TOTAL) {
                            if (field.popped[i]) continue
                            val c = field.centre(i, cell)
                            val d = hypot(c.x - pos.x, c.y - pos.y)
                            if (d < r * 1.25f && d < bestD) { best = i; bestD = d }
                        }
                        if (best >= 0) {
                            field.popped[best] = true
                            field.popTime[best] = field.time
                            field.left--
                            cues.buzz()
                            if (prefs.soundOn) popSound.play()
                            if (field.left == 0) cues.say("All popped. Well done")
                        }
                    }
                },
        ) {
            val t = field.time // read: redraw every frame
            val cell = size.width / COLS
            val r = cell * 0.40f
            for (i in 0 until TOTAL) {
                val color = rowColors[i / COLS]
                val centre = field.centre(i, cell)
                if (!field.popped[i]) {
                    val grow = easeOutBack(((t - field.spawnTime[i]) / 0.35f).coerceIn(0f, 1f))
                    val wobble = if (field.motion == 0) 1f else 1f + 0.035f * sin(t * 2.2f + field.ph[i * 4] * 6f)
                    drawJelly(centre, r * grow * wobble, color)
                } else {
                    val p = (t - field.popTime[i]) / BURST_SECONDS
                    if (p in 0f..SQUISH) {
                        val k = p / SQUISH
                        drawJelly(centre, r, color, sx = 1f + 0.15f * k, sy = 1f - 0.18f * k)
                    } else if (p in SQUISH..1f) {
                        drawBurst(centre, r, color, (p - SQUISH) / (1f - SQUISH), field.ph[i * 4 + 1])
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            if (allPopped) "All popped! ⭐" else "${field.left} left",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            if (allPopped) {
                OutlinedButton(onClick = { field.refill() }, modifier = Modifier.height(60.dp)) {
                    Text("🔄 Again", style = MaterialTheme.typography.labelLarge, color = tool.deep)
                }
                Spacer(Modifier.width(12.dp))
            }
            if (field.left < TOTAL) {
                Button(
                    onClick = onFinish,
                    colors = ButtonDefaults.buttonColors(containerColor = tool.deep, contentColor = Color.White),
                    modifier = Modifier.height(60.dp).width(160.dp),
                ) { Text("✓ Done", style = MaterialTheme.typography.labelLarge) }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** Overshoots slightly then settles: a soft "boing". */
private fun easeOutBack(x: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    val y = x - 1f
    return 1f + c3 * y * y * y + c1 * y * y
}

/**
 * A glossy "jelly ball": shaded body, soft rim light, shine and a shadow,
 * so it looks like a real squishy object. [sx]/[sy] squash it before popping.
 */
private fun DrawScope.drawJelly(c: Offset, r: Float, color: Color, sx: Float = 1f, sy: Float = 1f, alpha: Float = 1f) {
    if (r <= 0f) return
    // Shadow on the "floor"
    drawOval(
        Color.Black.copy(alpha = 0.12f * alpha),
        topLeft = Offset(c.x - r * 0.75f * sx + r * 0.08f, c.y + r * 0.92f * sy - r * 0.18f),
        size = Size(r * 1.5f * sx, r * 0.36f),
    )
    scale(sx, sy, pivot = c) {
        drawCircle(
            Brush.radialGradient(
                0f to lerp(color, Color.White, 0.45f),
                0.55f to color,
                1f to lerp(color, Color.Black, 0.25f),
                center = Offset(c.x - r * 0.35f, c.y - r * 0.4f),
                radius = r * 1.3f,
            ),
            r, c, alpha = alpha,
        )
        // Soft light bouncing up from below
        drawCircle(
            Brush.radialGradient(
                0f to Color.White.copy(alpha = 0.25f),
                1f to Color.Transparent,
                center = Offset(c.x, c.y + r * 0.55f),
                radius = r * 0.7f,
            ),
            r, c, alpha = alpha,
        )
        // Shine
        val hl = Offset(c.x - r * 0.38f, c.y - r * 0.45f)
        rotate(-34f, pivot = hl) {
            drawOval(Color.White.copy(alpha = 0.85f * alpha), topLeft = Offset(hl.x - r * 0.26f, hl.y - r * 0.15f), size = Size(r * 0.52f, r * 0.30f))
        }
        drawCircle(Color.White.copy(alpha = 0.6f * alpha), r * 0.06f, Offset(c.x - r * 0.12f, c.y - r * 0.62f))
    }
}

/**
 * The pop (p goes 0 -> 1 over half a second): the bubble swells and vanishes,
 * a ring ripples outwards, and droplets fly off and fade.
 */
private fun DrawScope.drawBurst(c: Offset, r: Float, color: Color, p: Float, phase: Float) {
    val fade = 1f - p
    // Swell and vanish (quickly)
    if (p < 0.15f) {
        val q = p / 0.15f
        drawJelly(c, r * (1f + 0.2f * q), color, alpha = 1f - q)
    }
    // Ripple ring
    drawCircle(color.copy(alpha = 0.8f * fade), r * (1f + 0.9f * p), c, style = Stroke(r * 0.14f * fade + 1f))
    // Droplets
    val count = 9
    for (k in 0 until count) {
        val a = (k.toFloat() / count + phase) * 2f * PI.toFloat()
        val dist = r * (0.95f + 1.6f * p) // start at the rim, spray outwards past the ring
        val d = Offset(c.x + cos(a) * dist, c.y + sin(a) * dist + r * 0.4f * p * p) // tiny fall, like real droplets
        drawCircle(color.copy(alpha = fade), r * 0.17f * fade + 0.5f, d)
    }
    // Sparkle in the middle
    if (p < 0.5f) {
        val s = r * 0.35f * (1f - p * 2f)
        drawLine(Color.White.copy(alpha = 0.9f * fade), Offset(c.x - s, c.y), Offset(c.x + s, c.y), strokeWidth = r * 0.07f)
        drawLine(Color.White.copy(alpha = 0.9f * fade), Offset(c.x, c.y - s), Offset(c.x, c.y + s), strokeWidth = r * 0.07f)
    }
}

/** A soft "bloop" (generated tone, no recording), played at a slightly different pitch each time. */
private class PopSound(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val id = pool.load(context, R.raw.pop, 1)

    fun play() {
        pool.play(id, 0.7f, 0.7f, 1, 0, 0.85f + Random.nextFloat() * 0.4f)
    }

    fun release() = pool.release()
}

@Composable
private fun rememberPopSound(): PopSound {
    val context = LocalContext.current
    val sound = remember { PopSound(context.applicationContext) }
    DisposableEffect(Unit) { onDispose { sound.release() } }
    return sound
}
