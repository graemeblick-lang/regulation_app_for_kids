package com.calmcorner.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmcorner.app.data.BodySpot
import com.calmcorner.app.data.Zone
import com.calmcorner.app.data.clues
import com.calmcorner.app.data.spots
import com.calmcorner.app.ui.components.Pose
import com.calmcorner.app.ui.components.PoseFigure
import com.calmcorner.app.ui.components.SoundToggle
import com.calmcorner.app.ui.components.rememberCues

private val standingPose = Pose(
    Offset(.5f, .2f), Offset(.5f, .30f), Offset(.5f, .56f), Offset(.415f, .32f), Offset(.40f, .44f),
    Offset(.38f, .56f), Offset(.46f, .73f), Offset(.45f, .91f),
).mirrored()

/** "How does this colour feel in my body?" A figure with glowing spots plus simple words. */
@Composable
fun BodyCluesScreen(zone: Zone, onBack: () -> Unit) {
    val cues = rememberCues()
    LaunchedEffect(zone) {
        cues.say("When I am ${zone.label}: " + zone.clues.joinToString(". ") { it.text })
    }
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("${zone.emoji} ${zone.label}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            SoundToggle()
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        ) {
            Text("How does ${zone.label} feel in my body?", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth(0.6f).aspectRatio(1f)) {
                PoseFigure(
                    pose = standingPose, active = emptySet(), props = emptySet(),
                    body = zone.color.copy(alpha = 0.22f), limb = zone.color.copy(alpha = 0.35f),
                    deep = zone.color, prop = Color.Transparent, tension = 0f,
                    modifier = Modifier.matchParentSize(),
                )
                Canvas(Modifier.matchParentSize()) {
                    val s = size.minDimension
                    fun glow(x: Float, y: Float, r: Float) {
                        val c = Offset(x * s, y * s)
                        drawCircle(Brush.radialGradient(0f to zone.color.copy(alpha = 0.85f), 1f to Color.Transparent, center = c, radius = r * s * 1.6f), r * s * 1.6f, c)
                    }
                    zone.spots.forEach { spot ->
                        when (spot) {
                            BodySpot.Face -> glow(.5f, .2f, .11f)
                            BodySpot.Heart -> glow(.5f, .37f, .07f)
                            BodySpot.Hands -> { glow(.38f, .56f, .06f); glow(.62f, .56f, .06f) }
                            BodySpot.Tummy -> glow(.5f, .47f, .08f)
                            BodySpot.Shoulders -> { glow(.43f, .32f, .06f); glow(.57f, .32f, .06f) }
                            BodySpot.Legs -> { glow(.45f, .8f, .07f); glow(.55f, .8f, .07f) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            zone.clues.forEach { clue ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(clue.emoji, fontSize = 32.sp)
                    Spacer(Modifier.width(14.dp))
                    Text(clue.text, style = MaterialTheme.typography.titleLarge)
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = zone.color, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(60.dp),
            ) { Text("OK", style = MaterialTheme.typography.labelLarge) }
            Spacer(Modifier.height(16.dp))
        }
    }
}
