package com.calmcorner.app.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.calmcorner.app.data.Tool
import com.calmcorner.app.ui.components.KeepScreenOn
import com.calmcorner.app.ui.components.ToolScaffold
import com.calmcorner.app.ui.components.rememberCues
import com.calmcorner.app.ui.components.tapToToggle
import kotlinx.coroutines.isActive
import kotlin.math.sqrt
import kotlin.random.Random

private const val GLITTER = 170

private val glitterColors = listOf(
    Color(0xFFF2C14E), Color(0xFFE57B8D), Color(0xFF6C9BD2),
    Color(0xFF9C84C9), Color(0xFF5BC0BE), Color(0xFFFFFFFF),
)

/** Glitter particles in unit coordinates inside the jar (0..1 both ways). */
private class Glitter {
    val x = FloatArray(GLITTER) { Random.nextFloat() }
    val rest = FloatArray(GLITTER) { 1f - Random.nextFloat() * 0.22f } // where each one settles
    val y = FloatArray(GLITTER) { rest[it] }
    val vx = FloatArray(GLITTER)
    val vy = FloatArray(GLITTER)
    val drag = FloatArray(GLITTER) { 1.6f + Random.nextFloat() * 1.6f } // varied, so they settle at different times
    val radius = FloatArray(GLITTER) { 0.010f + Random.nextFloat() * 0.012f }
    val color = IntArray(GLITTER) { Random.nextInt(glitterColors.size) }

    fun shake() {
        for (i in 0 until GLITTER) {
            vx[i] = (Random.nextFloat() - 0.5f) * 2.4f
            vy[i] = -0.6f - Random.nextFloat() * 2.2f
        }
    }

    /** Returns true while anything is still moving. */
    fun step(dt: Float): Boolean {
        var moving = false
        for (i in 0 until GLITTER) {
            if (y[i] >= rest[i] && vy[i] >= 0f && kotlin.math.abs(vx[i]) < 0.01f) continue
            moving = true
            vy[i] += 0.16f * dt // gentle gravity
            val k = 1f - drag[i] * dt // thick-liquid drag: slow, floaty fall
            vx[i] *= k
            vy[i] *= k
            x[i] += vx[i] * dt
            y[i] += vy[i] * dt
            if (x[i] < 0f) { x[i] = 0f; vx[i] = -vx[i] * 0.5f }
            if (x[i] > 1f) { x[i] = 1f; vx[i] = -vx[i] * 0.5f }
            if (y[i] < 0f) { y[i] = 0f; vy[i] = 0f }
            if (y[i] >= rest[i] && vy[i] > 0f) { y[i] = rest[i]; vy[i] = 0f; vx[i] *= 0.3f }
        }
        return moving
    }
}

@Composable
fun CalmJarScreen(onBack: () -> Unit, onFinish: () -> Unit) {
    val tool = Tool.CalmJar
    val cues = rememberCues()
    val glitter = remember { Glitter() }
    var frame by remember { mutableLongStateOf(0L) }
    var shaken by remember { mutableStateOf(false) }
    var moving by remember { mutableStateOf(false) }
    KeepScreenOn()

    val shake = {
        glitter.shake()
        moving = true
        cues.buzz()
        if (!shaken) cues.say("Watch the glitter fall. Breathe slowly.")
        shaken = true
    }
    val latestShake by rememberUpdatedState(shake)

    // Shake the phone to stir the glitter.
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var lastShake = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val (ax, ay, az) = Triple(e.values[0], e.values[1], e.values[2])
                val g = sqrt(ax * ax + ay * ay + az * az)
                val now = System.currentTimeMillis()
                if (g > 17f && now - lastShake > 600) { // well above gravity (9.8)
                    lastShake = now
                    latestShake()
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (accel != null) sm.registerListener(listener, accel, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(listener) }
    }

    // Physics loop (runs only while glitter is moving).
    LaunchedEffect(moving) {
        if (!moving) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (isActive && moving) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
            last = now
            moving = glitter.step(dt)
            frame++
        }
    }

    ToolScaffold(
        title = tool.title,
        tip = "Shake your device. Watch the glitter fall.",
        howTo = "Shake your device, or tap ✨ Shake.\nWatch the glitter slowly fall.\nBreathe slowly while you watch.",
        color = tool.deep,
        showTip = !moving,
        onBack = onBack,
    ) {
        Spacer(Modifier.weight(1f))
        Canvas(Modifier.fillMaxWidth(0.62f).aspectRatio(0.7f).tapToToggle { shake() }
            .semantics { contentDescription = "Glitter jar. Tap to shake it." }) {
            frame // read so the jar redraws every frame
            val w = size.width
            val h = size.height
            val glass = RoundRect(Rect(0.04f * w, 0.14f * h, 0.96f * w, 0.98f * h), CornerRadius(0.16f * w))
            val glassPath = Path().apply { addRoundRect(glass) }
            // Lid
            drawRoundRect(tool.deep, topLeft = Offset(0.16f * w, 0.02f * h), size = Size(0.68f * w, 0.10f * h), cornerRadius = CornerRadius(0.04f * w))
            // Liquid
            drawPath(glassPath, tool.color.copy(alpha = 0.18f))
            // Glitter (inside the glass only)
            val inner = Rect(0.10f * w, 0.18f * h, 0.90f * w, 0.95f * h)
            clipPath(glassPath) {
                for (i in 0 until GLITTER) {
                    val c = Offset(inner.left + glitter.x[i] * inner.width, inner.top + glitter.y[i] * inner.height)
                    val col = glitterColors[glitter.color[i]]
                    drawCircle(if (col == Color.White) tool.color.copy(alpha = 0.5f) else col, glitter.radius[i] * w, c)
                }
            }
            // Glass outline and shine
            drawPath(glassPath, tool.deep.copy(alpha = 0.7f), style = Stroke(4.dp.toPx()))
            drawRoundRect(Color.White.copy(alpha = 0.35f), topLeft = Offset(0.12f * w, 0.24f * h), size = Size(0.05f * w, 0.45f * h), cornerRadius = CornerRadius(0.03f * w))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            when {
                !shaken -> "Shake your device!"
                moving -> "Watch the glitter fall…"
                else -> "All still. ✨"
            },
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        if (moving) {
            Text("Breathe slowly.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { shake() },
                colors = ButtonDefaults.buttonColors(containerColor = tool.deep, contentColor = Color.White),
                modifier = Modifier.height(60.dp).width(170.dp),
            ) { Text("✨  Shake", style = MaterialTheme.typography.labelLarge) }
            if (shaken) {
                Spacer(Modifier.width(12.dp))
                OutlinedButton(onClick = onFinish, modifier = Modifier.height(60.dp)) {
                    Text("✓ Done", style = MaterialTheme.typography.labelLarge, color = tool.deep)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
