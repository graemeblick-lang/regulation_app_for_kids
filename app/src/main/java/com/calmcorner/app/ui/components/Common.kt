package com.calmcorner.app.ui.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Standard layout for every tool: back arrow, title, short instruction, content. */
@Composable
fun ToolScaffold(
    title: String,
    instructions: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(title, style = MaterialTheme.typography.titleLarge)
        }
        Text(
            instructions,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 24.dp, vertical = 16.dp),
            content = content,
        )
    }
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

/** Gentle buzz at each phase change so kids can follow with eyes closed. */
@Composable
fun rememberPhaseBuzz(): (TimedPhase) -> Unit {
    val haptic = LocalHapticFeedback.current
    return { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
}

/** Lets a child pick a breathing speed that feels comfortable. */
@Composable
fun PaceSelector(seconds: Int, onChange: (Int) -> Unit, color: Color, enabled: Boolean = true) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Speed:", style = MaterialTheme.typography.bodyMedium)
        listOf(3 to "Quick", 4 to "Steady", 5 to "Slow").forEach { (value, label) ->
            FilterChip(
                selected = seconds == value,
                onClick = { onChange(value) },
                enabled = enabled,
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = color.copy(alpha = 0.25f),
                ),
            )
        }
    }
}

@Composable
fun StartPauseRow(
    running: Boolean,
    color: Color,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    startLabel: String = "Start",
) {
    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onToggle,
            colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
            modifier = Modifier.height(52.dp).width(150.dp),
        ) {
            Text(if (running) "Pause" else startLabel, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.width(12.dp))
        OutlinedButton(onClick = onReset, modifier = Modifier.height(52.dp)) {
            Text("Restart")
        }
    }
}

/** Big phase word plus a countdown number in the middle of a breathing shape. */
@Composable
fun PhaseLabel(label: String, secondsLeft: Int, showCount: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        if (showCount) {
            Text(
                "$secondsLeft",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

fun breathsText(rounds: Int, noun: String = "breath"): String =
    if (rounds == 0) " " else "$rounds ${if (rounds == 1) noun else noun + "s"} done ✨"
