package com.calmcorner.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmcorner.app.data.CopingCard
import com.calmcorner.app.data.Photos
import com.calmcorner.app.data.Tool
import com.calmcorner.app.ui.components.SoundToggle
import com.calmcorner.app.ui.components.rememberCues

/** One of her own calm ideas, big and simple, read aloud. */
@Composable
fun CopingCardScreen(card: CopingCard, onBack: () -> Unit, onDone: () -> Unit) {
    val cues = rememberCues()
    val context = LocalContext.current
    val photo = remember(card.photo) { card.photo?.let { Photos.load(context, it) } }
    LaunchedEffect(card.id) { cues.say(card.text) }
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("My calm idea", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            SoundToggle()
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().padding(24.dp),
        ) {
            Spacer(Modifier.weight(1f))
            if (photo != null) {
                Image(
                    photo.asImageBitmap(), contentDescription = card.text, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(28.dp)),
                )
            } else {
                Text("💛", fontSize = 120.sp)
            }
            Spacer(Modifier.height(24.dp))
            Text(card.text, style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(containerColor = Tool.CopingCard.deep, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(64.dp),
            ) { Text("✓  I did it", style = MaterialTheme.typography.labelLarge) }
            Spacer(Modifier.height(8.dp))
        }
    }
}
