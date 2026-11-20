package com.calmcorner.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmcorner.app.data.Tool

private data class SenseStep(val count: Int, val emoji: String, val sense: String, val hint: String)

private val senseSteps = listOf(
    SenseStep(5, "👀", "see", "Look around slowly. A cloud, a book, your shoes, something blue…"),
    SenseStep(4, "✋", "touch", "Your clothes, the floor, a soft toy, your own hair…"),
    SenseStep(3, "👂", "hear", "Listen carefully. Birds, a fan, cars, your own breathing…"),
    SenseStep(2, "👃", "smell", "Your sleeve, the air, soap on your hands… or a smell you love."),
    SenseStep(1, "👅", "taste", "What can you taste right now? Or think of your favourite food."),
)

@Composable
fun GroundingScreen(onBack: () -> Unit) {
    val color = Tool.Grounding.color
    var step by rememberSaveable { mutableIntStateOf(0) }
    var found by rememberSaveable { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current

    com.calmcorner.app.ui.components.ToolScaffold(
        title = Tool.Grounding.title,
        instructions = "Use your senses to come back to right here, right now. Tap a circle each time you notice something.",
        onBack = onBack,
    ) {
        AnimatedContent(targetState = step, label = "grounding-step", modifier = Modifier.weight(1f)) { s ->
            if (s >= senseSteps.size) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    Spacer(Modifier.height(48.dp))
                    Text("🌟", fontSize = 88.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("You did it!", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "You noticed 15 things around you. Take one more big, slow breath. How does your body feel now?",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                val item = senseSteps[s]
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Spacer(Modifier.height(32.dp))
                    Text(item.emoji, fontSize = 80.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Find ${item.count} ${if (item.count == 1) "thing" else "things"} you can ${item.sense.uppercase()}",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        item.hint,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(32.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(item.count) { i ->
                            val filled = i < found
                            val bg by animateColorAsState(if (filled) color else Color.Transparent, label = "dot")
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(bg, CircleShape)
                                    .border(3.dp, color, CircleShape)
                                    .semantics { contentDescription = if (filled) "Found ${i + 1}" else "Not found yet" }
                                    .clickable {
                                        if (i == found && found < item.count) {
                                            found++
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        } else if (i == found - 1) {
                                            found-- // let kids undo a mis-tap
                                        }
                                    },
                            ) {
                                if (filled) Text("✓", color = Color.White, fontSize = 24.sp)
                            }
                        }
                    }
                }
            }
        }
        val current = senseSteps.getOrNull(step)
        val done = step >= senseSteps.size
        Button(
            onClick = {
                if (done) onBack()
                else if (current != null && found < current.count) {
                    found++
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                } else { step++; found = 0 }
            },
            colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text(
                when {
                    done -> "Back to Calm Corner"
                    current != null && found < current.count -> "I found one!"
                    step == senseSteps.lastIndex -> "Finish"
                    else -> "Next sense →"
                },
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}
