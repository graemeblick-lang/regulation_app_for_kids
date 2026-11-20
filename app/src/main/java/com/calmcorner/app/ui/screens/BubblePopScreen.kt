package com.calmcorner.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.calmcorner.app.data.Tool
import com.calmcorner.app.ui.components.ToolScaffold
import com.calmcorner.app.ui.components.rememberCues

private const val COLS = 5
private const val ROWS = 6

private val rowColors = listOf(
    Color(0xFFE57B8D), Color(0xFFF0954A), Color(0xFFF2C94C),
    Color(0xFF5DB37E), Color(0xFF5B9BE0), Color(0xFF9C7BD6),
)

@Composable
fun BubblePopScreen(onBack: () -> Unit, onFinish: () -> Unit) {
    val tool = Tool.BubblePop
    val cues = rememberCues()
    val popped = remember { mutableStateListOf<Boolean>().apply { repeat(COLS * ROWS) { add(false) } } }
    val left = popped.count { !it }
    val allPopped = left == 0

    ToolScaffold(
        title = tool.title,
        tip = "Pop the bubbles. Go slowly.",
        howTo = "Tap one bubble at a time.\nTake a slow breath between pops.",
        color = tool.deep,
        showTip = left == COLS * ROWS,
        onBack = onBack,
    ) {
        Spacer(Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth(0.95f)) {
            for (r in 0 until ROWS) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (c in 0 until COLS) {
                        val i = r * COLS + c
                        Bubble(
                            color = rowColors[r],
                            popped = popped[i],
                            onPop = {
                                if (!popped[i]) {
                                    popped[i] = true
                                    cues.buzz()
                                    if (popped.all { it }) cues.say("All popped. Well done")
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            if (allPopped) "All popped! ⭐" else "$left left",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            if (allPopped) {
                OutlinedButton(onClick = { for (i in popped.indices) popped[i] = false }, modifier = Modifier.height(60.dp)) {
                    Text("🔄 Again", style = MaterialTheme.typography.labelLarge, color = tool.deep)
                }
                Spacer(Modifier.width(12.dp))
            }
            if (allPopped || left < COLS * ROWS) {
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

@Composable
private fun Bubble(color: Color, popped: Boolean, onPop: () -> Unit, modifier: Modifier = Modifier) {
    // Squash down when popped, like pressing a pop-it.
    val depth by animateFloatAsState(if (popped) 1f else 0f, tween(220), label = "pop")
    Box(
        modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .semantics { contentDescription = if (popped) "Popped bubble" else "Bubble" }
            .clickable(onClick = onPop),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val c = Offset(size.width / 2f, size.height / 2f)
            if (depth < 1f) {
                // Round shiny bubble
                drawCircle(color.copy(alpha = 0.85f), r * (1f - 0.15f * depth), c)
                drawCircle(Color.White.copy(alpha = 0.55f * (1f - depth)), r * 0.22f, Offset(c.x - r * 0.35f, c.y - r * 0.35f))
            }
            if (depth > 0f) {
                // Pushed-in: flat, paler, with a soft ring
                drawCircle(color.copy(alpha = 0.30f * depth), r * 0.92f, c)
                drawCircle(color.copy(alpha = 0.7f * depth), r * 0.80f, c, style = Stroke(r * 0.08f))
            }
        }
    }
}
