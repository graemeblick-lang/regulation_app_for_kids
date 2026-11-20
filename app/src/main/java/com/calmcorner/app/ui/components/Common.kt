package com.calmcorner.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.calmcorner.app.data.LocalPrefs
import com.calmcorner.app.data.Zone

/**
 * Standard layout for every tool: back arrow, title, one short tip, and an
 * optional "How?" with a few more simple sentences. The tip hides while the
 * exercise runs so only the animation is on screen.
 */
@Composable
fun ToolScaffold(
    title: String,
    tip: String,
    howTo: String,
    color: Color,
    showTip: Boolean,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    var showHow by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            SoundToggle()
            AnimatedVisibility(visible = showTip, enter = fadeIn(), exit = fadeOut()) {
                TextButton(onClick = { showHow = !showHow }) {
                    Text(if (showHow) "Close" else "How? ❓", color = color, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        AnimatedVisibility(
            visible = showTip,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Text(
                    tip,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                AnimatedVisibility(visible = showHow) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    ) {
                        Text(
                            howTo,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 24.dp, vertical = 16.dp),
            content = content,
        )
    }
}

/** Lets the animation itself act as a big start / pause button (no ripple). */
@Composable
fun Modifier.tapToToggle(onToggle: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    return this.clickable(interactionSource = source, indication = null, onClickLabel = "Start or pause", onClick = onToggle)
}

/** Keeps the screen awake while an exercise is open. */
@Composable
fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

/** Speed picker with picture cues for kids who find reading hard. */
@Composable
fun PaceSelector(seconds: Int, onChange: (Int) -> Unit, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(3 to "🐇 Fast", 4 to "🚶 Medium", 5 to "🐢 Slow").forEach { (value, label) ->
            FilterChip(
                selected = seconds == value,
                onClick = { onChange(value) },
                label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = color.copy(alpha = 0.25f)),
            )
        }
    }
}

/** Big Start/Pause button. When paused part-way, "Done" finishes the exercise. */
@Composable
fun StartPauseRow(
    running: Boolean,
    started: Boolean,
    deep: Color,
    onToggle: () -> Unit,
    onDone: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onToggle,
            colors = ButtonDefaults.buttonColors(containerColor = deep, contentColor = Color.White),
            modifier = Modifier.height(60.dp).width(190.dp),
        ) {
            Text(
                when {
                    running -> "⏸  Pause"
                    started -> "▶  Continue"
                    else -> "▶  Start"
                },
                style = MaterialTheme.typography.labelLarge,
            )
        }
        AnimatedVisibility(visible = started && !running) {
            Row {
                Spacer(Modifier.width(12.dp))
                OutlinedButton(onClick = onDone, modifier = Modifier.height(60.dp)) {
                    Text("✓ Done", style = MaterialTheme.typography.labelLarge, color = deep)
                }
            }
        }
    }
}

/** 🔊 / 🔇 quick switch for voice guidance (e.g. on the bus). Remembered. */
@Composable
fun SoundToggle() {
    val prefs = LocalPrefs.current
    val voice = LocalVoice.current
    IconButton(onClick = {
        val on = !prefs.voiceOn
        prefs.updateVoice(on)
        if (!on) voice?.stop()
    }) {
        Text(
            if (prefs.voiceOn) "🔊" else "🔇",
            fontSize = 24.sp,
            modifier = Modifier.semantics { contentDescription = if (prefs.voiceOn) "Sound is on. Tap to turn off." else "Sound is off. Tap to turn on." },
        )
    }
}

/** Big phase word plus a countdown number. */
@Composable
fun PhaseLabel(label: String, secondsLeft: Int, showCount: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        if (showCount) {
            Text("$secondsLeft", fontSize = 44.sp, style = MaterialTheme.typography.displaySmall)
        }
    }
}

/** Shared bottom area for breathing tools: speed picker (hidden while running) + buttons. */
@Composable
fun BreathingControls(
    pace: Int,
    onPace: (Int) -> Unit,
    running: Boolean,
    started: Boolean,
    color: Color,
    deep: Color,
    onToggle: () -> Unit,
    onDone: () -> Unit,
) {
    AnimatedVisibility(visible = !running) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PaceSelector(pace, onPace, color)
            Spacer(Modifier.height(16.dp))
        }
    }
    StartPauseRow(running, started, deep, onToggle, onDone)
    Spacer(Modifier.height(8.dp))
}

/** "⭐ 2 of 5" when there is a target, or "⭐ 2 boxes" when she keeps going. */
fun doneText(rounds: Int, noun: String, target: Int = 0): String = when {
    target > 0 -> "⭐ $rounds of $target"
    rounds == 0 -> " "
    else -> "⭐ $rounds $noun${if (rounds == 1) "" else "s"}"
}

/** One of the four colour check-in tiles (used on Home and after each tool). */
@Composable
fun ZoneTile(zone: Zone, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = zone.color.copy(alpha = if (selected) 0.40f else 0.16f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = if (selected) BorderStroke(3.dp, zone.color) else null,
        modifier = modifier.semantics { this.selected = selected },
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(40.dp).background(zone.color, CircleShape),
                ) { Text(zone.emoji, fontSize = 22.sp) }
                Spacer(Modifier.width(10.dp))
                Text(zone.label, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(6.dp))
            Text(zone.words, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** The four colour tiles in a 2 x 2 grid. */
@Composable
fun ZoneGrid(selected: Zone?, onPick: (Zone) -> Unit) {
    Column {
        Zone.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                row.forEach { z ->
                    ZoneTile(z, z == selected, onClick = { onPick(z) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
