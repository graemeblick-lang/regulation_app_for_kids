package com.calmcorner.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.calmcorner.app.data.LocalHistory
import com.calmcorner.app.data.LocalPrefs
import com.calmcorner.app.data.ThemeMode
import com.calmcorner.app.data.Tool
import com.calmcorner.app.data.Zone
import com.calmcorner.app.data.summarise
import com.calmcorner.app.ui.components.LocalVoice
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Settings and history for parents/carers. Opened by holding ⚙️ for 3 seconds. */
@Composable
fun GrownUpScreen(onBack: () -> Unit) {
    val prefs = LocalPrefs.current
    val history = LocalHistory.current
    val voice = LocalVoice.current
    val summary = summarise(history.records, System.currentTimeMillis())
    var confirmClear by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Grown-ups", style = MaterialTheme.typography.titleLarge)
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            // ---- This week ----
            Section("This week") {
                Text("⭐ ${summary.stars} finished ${if (summary.stars == 1) "exercise" else "exercises"}", style = MaterialTheme.typography.titleMedium)
                if (summary.checkIns > 0) {
                    Text(
                        "Felt calmer afterwards ${summary.feltBetter} of ${summary.checkIns} times",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                summary.mostHelpful?.let {
                    Text("Most helpful: ${it.emoji} ${it.title}", style = MaterialTheme.typography.bodyMedium)
                }
                if (summary.perTool.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    summary.perTool.forEach { (tool, count) ->
                        Text("${tool.emoji} ${tool.title}: $count", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (summary.stars == 0) {
                    Text("No exercises yet this week.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ---- Recent ----
            if (history.records.isNotEmpty()) {
                Section("Recent") {
                    val fmt = remember { SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault()) }
                    history.records.takeLast(15).asReversed().forEach { r ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("${r.tool.emoji} ${r.tool.title}", style = MaterialTheme.typography.bodyMedium)
                                Text(fmt.format(Date(r.time)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            ZoneDot(r.before)
                            Text("  →  ", style = MaterialTheme.typography.bodyMedium)
                            ZoneDot(r.after)
                        }
                    }
                    Text("Coloured dots: before → after. Grey = not chosen.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ---- Settings ----
            Section("Sound and vibration") {
                SwitchRow("Sound", "Voice guidance and bubble pops. There is also a 🔊 button on every exercise.", prefs.voiceOn) {
                    prefs.updateVoice(it); if (!it) voice?.stop()
                }
                SwitchRow("Vibration", "A gentle buzz at each step.", prefs.vibrationOn) { prefs.updateVibration(it) }
            }

            Section("Breathing") {
                Text("Breaths per session", style = MaterialTheme.typography.titleMedium)
                Text("Finger Breathing uses 5 breaths per hand.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                ChipRow(listOf(3 to "3", 5 to "5", 10 to "10", 0 to "Keep going"), prefs.breathsPerSession) { prefs.updateBreaths(it) }
            }

            Section("Bubble Pop") {
                Text("Bubble movement", style = MaterialTheme.typography.titleMedium)
                Text("How much the bubbles drift as fewer are left. Only grown-ups can change this.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                ChipRow(listOf(0 to "Still", 1 to "Calm", 2 to "Lively"), prefs.bubbleMotion) { prefs.updateBubbleMotion(it) }
            }

            Section("Calm Drum") {
                Text("The drum starts at her speed, then slowly gets slower.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text("Ends at", style = MaterialTheme.typography.titleMedium)
                ChipRow(listOf(50 to "Very slow", 60 to "Slow", 70 to "Gentle"), prefs.drumEndBpm) { prefs.updateDrumEndBpm(it) }
                Spacer(Modifier.height(8.dp))
                Text("How long it takes to slow down", style = MaterialTheme.typography.titleMedium)
                ChipRow(listOf(0 to "Short", 1 to "Normal", 2 to "Long"), prefs.drumLength) { prefs.updateDrumLength(it) }
                Spacer(Modifier.height(4.dp))
                SwitchRow("Tick sound on the beat", "Off: the beat is shown by the ring and a buzz.", prefs.drumTick) { prefs.updateDrumTick(it) }
            }

            Section("Look") {
                Text("Theme", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                ChipRow(
                    listOf(ThemeMode.System.ordinal to "Match phone", ThemeMode.Light.ordinal to "Light", ThemeMode.Dark.ordinal to "Dark"),
                    prefs.theme.ordinal,
                ) { prefs.updateTheme(ThemeMode.entries[it]) }
            }

            Section("Tools to show") {
                Text("Hide tools she doesn't use. Fewer choices can be easier.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Tool.entries.forEach { tool ->
                    SwitchRow("${tool.emoji} ${tool.title}", null, tool.name !in prefs.hiddenTools) { show ->
                        prefs.setToolHidden(tool, !show)
                    }
                }
            }

            Section("Privacy") {
                Text(
                    "Everything stays on this phone. The app has no internet access, no accounts, no ads and no tracking.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { confirmClear = true }, enabled = history.records.isNotEmpty()) {
                    Text("Clear history")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear history?") },
            text = { Text("This deletes all stars and history on this phone. It can't be undone.") },
            confirmButton = { TextButton(onClick = { history.clear(); confirmClear = false }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            content()
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ChipRow(options: List<Pair<Int, String>>, selected: Int, onSelect: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(label) })
        }
    }
}

@Composable
private fun ZoneDot(zone: Zone?) {
    Box(Modifier.size(18.dp).background(zone?.color ?: Color(0xFFBDBDBD), CircleShape))
}
