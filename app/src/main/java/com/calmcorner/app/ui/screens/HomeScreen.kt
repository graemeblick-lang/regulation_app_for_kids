package com.calmcorner.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmcorner.app.data.Zone
import com.calmcorner.app.data.Tool

@Composable
fun HomeScreen(
    zone: Zone?,
    onZoneChange: (Zone?) -> Unit,
    onOpenTool: (Tool) -> Unit,
) {
    val suggested = zone?.suggestions.orEmpty()
    val others = Tool.entries.filterNot { it in suggested }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text("Calm Corner", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(4.dp))
            Text(
                "Your calm place.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Text("What colour are you?", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(10.dp))
            Zone.entries.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                    row.forEach { z ->
                        ZoneTile(
                            zone = z,
                            selected = z == zone,
                            onClick = { onZoneChange(if (z == zone) null else z) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            AnimatedVisibility(visible = zone != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = (zone?.color ?: Color.Transparent).copy(alpha = 0.18f)),
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                ) {
                    Text(
                        zone?.message.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
        if (suggested.isNotEmpty()) {
            item { SectionTitle("Try these") }
            items(suggested, key = { "s-" + it.name }) { ToolCard(it, highlighted = true, onClick = { onOpenTool(it) }) }
            item { SectionTitle("More") }
        } else {
            item { SectionTitle("Calm tools") }
        }
        items(others, key = { it.name }) { ToolCard(it, highlighted = false, onClick = { onOpenTool(it) }) }
        item {
            Spacer(Modifier.height(12.dp))
            Text(
                "Still feel big feelings? Talk to a grown-up you trust. 💛",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ZoneTile(zone: Zone, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
        Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
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

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
}

@Composable
private fun ToolCard(tool: Tool, highlighted: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) tool.color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (highlighted) 0.dp else 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(56.dp).background(tool.color.copy(alpha = 0.25f), CircleShape),
            ) {
                Text(tool.emoji, fontSize = 28.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(tool.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    tool.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
