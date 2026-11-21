package com.calmcorner.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmcorner.app.data.LocalPrefs
import com.calmcorner.app.data.Photos
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.calmcorner.app.data.Tool
import com.calmcorner.app.data.Zone
import com.calmcorner.app.ui.components.ZoneGrid
import com.calmcorner.app.ui.components.rememberCues

/**
 * Shown after every finished tool: a star for finishing (for effort, not for
 * "feeling better"), then "How do you feel now?". If still not calm, suggest
 * another tool. This teaches that the tools work and feeds the history.
 */
@Composable
fun CheckInScreen(
    tool: Tool,
    after: Zone?,
    onPick: (Zone?) -> Unit,
    onTryTool: (Tool) -> Unit,
    onHome: () -> Unit,
) {
    val cues = rememberCues()
    val prefs = LocalPrefs.current
    LaunchedEffect(Unit) {
        cues.say(
            when (prefs.rewardStyle) {
                1 -> "All done. A new flower is in your garden. How do you feel now?"
                else -> "All done. How do you feel now?"
            },
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        // Reward style is a grown-up setting. Neutral by default: no praise or reward,
        // because for some kids rewards feel like a demand.
        when (prefs.rewardStyle) {
            1 -> {
                Text("🌷", fontSize = 72.sp)
                Text("All done.", style = MaterialTheme.typography.displaySmall)
                Text("A new flower is in your garden.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            2 -> {
                Text("⭐", fontSize = 72.sp)
                Text("All done.", style = MaterialTheme.typography.displaySmall)
            }
            else -> {
                Text("🌿", fontSize = 72.sp)
                Text("All done.", style = MaterialTheme.typography.displaySmall)
            }
        }
        Spacer(Modifier.height(28.dp))

        if (after == null) {
            Text("How do you feel now?", style = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            ZoneGrid(selected = null, onPick = { z ->
                onPick(z)
                cues.say(if (z == Zone.Green) "Great. Your body feels calm." else "That's OK. Let's try another tool.")
            })
            TextButton(onClick = onHome) { Text("Skip", style = MaterialTheme.typography.titleMedium) }
        } else if (after == Zone.Green) {
            Text("Great! 💚", style = MaterialTheme.typography.headlineMedium)
            Text("Your body feels calm.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))
            HomeButton(tool.deep, onHome)
        } else {
            Text("That's OK.", style = MaterialTheme.typography.headlineMedium)
            Text("Let's try another tool.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            if (after == Zone.Red) {
                Text("Or talk to a grown-up you trust. 💛", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                val context = LocalContext.current
                val pic = remember(prefs.calmPicture) { prefs.calmPicture?.let { Photos.load(context, it) } }
                if (pic != null) {
                    Spacer(Modifier.height(12.dp))
                    Image(
                        pic.asImageBitmap(), contentDescription = prefs.calmPictureCaption, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1.6f).clip(RoundedCornerShape(16.dp)),
                    )
                    Text(prefs.calmPictureCaption, style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(20.dp))
            val visible = prefs.visibleTools()
            val next = after.suggestions.firstOrNull { it != tool && it in visible }
                ?: visible.firstOrNull { it != tool }
            if (next != null) {
                Button(
                    onClick = { onTryTool(next) },
                    colors = ButtonDefaults.buttonColors(containerColor = next.deep, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                ) { Text("${next.emoji}  Try ${next.title}", style = MaterialTheme.typography.labelLarge) }
                Spacer(Modifier.height(12.dp))
            }
            OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("🏠  Home", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun HomeButton(color: Color, onHome: () -> Unit) {
    Button(
        onClick = onHome,
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
        modifier = Modifier.fillMaxWidth().height(60.dp),
    ) { Text("🏠  Home", style = MaterialTheme.typography.labelLarge) }
}
