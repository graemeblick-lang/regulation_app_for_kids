package com.calmcorner.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmcorner.app.data.LocalHistory
import com.calmcorner.app.data.CopingCard
import com.calmcorner.app.data.LocalCopingCards
import com.calmcorner.app.data.Photos
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.calmcorner.app.data.LocalPrefs
import com.calmcorner.app.data.Tool
import com.calmcorner.app.data.Zone
import com.calmcorner.app.data.summarise
import com.calmcorner.app.ui.components.ZoneGrid
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    zone: Zone?,
    onZoneChange: (Zone?) -> Unit,
    onOpenTool: (Tool) -> Unit,
    onOpenGrownUps: () -> Unit,
    onOpenCard: (CopingCard) -> Unit,
    onOpenClues: (Zone) -> Unit,
) {
    val prefs = LocalPrefs.current
    val history = LocalHistory.current
    val cards = LocalCopingCards.current.cards
    val visible = prefs.visibleTools()
    val favourites = visible.filter { it.name in prefs.favourites }
    val suggested = zone?.suggestions.orEmpty().filter { it in visible }
    val others = visible.filterNot { it in suggested }
    val context = LocalContext.current
    val calmPicture = remember(prefs.calmPicture) { prefs.calmPicture?.let { Photos.load(context, it) } }
    val stars = summarise(history.records, System.currentTimeMillis()).stars

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Calm Corner", style = MaterialTheme.typography.displaySmall)
                    if (stars > 0) {
                        Text(
                            "⭐ $stars ${if (stars == 1) "star" else "stars"} this week",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text("Your calm place.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (prefs.schoolMode) {
                        Text("🏫 School mode: quiet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HoldToOpen(onOpen = onOpenGrownUps)
            }
            Spacer(Modifier.height(20.dp))
            Text("How do you feel? Pick a colour.", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(10.dp))
            ZoneGrid(selected = zone, onPick = { z -> onZoneChange(if (z == zone) null else z) })
            // Under the colours: body clues link, and her calm picture when she picks Red.
            if (zone != null) {
                TextButton(onClick = { onOpenClues(zone) }, contentPadding = PaddingValues(0.dp)) {
                    Text("👀 How does ${zone.label} feel in my body?", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            if (zone == Zone.Red && calmPicture != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Zone.Red.color.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Image(
                            calmPicture.asImageBitmap(), contentDescription = prefs.calmPictureCaption,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1.6f).clip(RoundedCornerShape(16.dp)),
                        )
                        Text(prefs.calmPictureCaption, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
        if (zone != null && suggested.isNotEmpty()) {
            item {
                // The colour's message leads straight into the tools that suit it.
                Card(
                    colors = CardDefaults.cardColors(containerColor = zone.color.copy(alpha = 0.18f)),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) {
                    Text(zone.message, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
                }
            }
            items(suggested.chunked(2), key = { row -> "s-" + row.joinToString { it.name } }) { row ->
                ToolRow(row, highlighted = true, onOpenTool)
            }
        }
        if (favourites.isNotEmpty() || cards.isNotEmpty()) {
            item { SectionTitle("⭐ My calm plan") }
            val planItems: List<Any> = favourites + cards
            items(planItems.chunked(2), key = { row -> "p-" + row.joinToString { if (it is Tool) it.name else (it as CopingCard).id } }) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
                    row.forEach { item ->
                        when (item) {
                            is Tool -> ToolTile(item, highlighted = true, starred = true, onStar = { prefs.toggleFavourite(item) }, onClick = { onOpenTool(item) }, modifier = Modifier.weight(1f).fillMaxHeight())
                            is CopingCard -> CardTile(item, onClick = { onOpenCard(item) }, modifier = Modifier.weight(1f).fillMaxHeight())
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        item { SectionTitle(if (zone != null && suggested.isNotEmpty()) "More tools" else "Calm tools") }
        items(others.chunked(2), key = { row -> row.joinToString { it.name } }) { row ->
            ToolRow(row, highlighted = false, onOpenTool)
        }
        item {
            Spacer(Modifier.height(12.dp))
            Text(
                "Still upset? Talk to a grown-up you trust. 💛",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Small ⚙️ that only opens when held for 3 seconds, so kids don't wander
 * into settings by accident. A ring fills while holding.
 */
@Composable
private fun HoldToOpen(onOpen: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var showHint by remember { mutableStateOf(false) }
    LaunchedEffect(showHint) {
        if (showHint) { delay(2500); showHint = false }
    }
    Column(horizontalAlignment = Alignment.End) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = "Grown-ups area. Press and hold for 3 seconds." }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { showHint = true },
                        onPress = {
                            val job = scope.launch {
                                progress.animateTo(1f, tween(3000, easing = LinearEasing))
                                onOpen()
                            }
                            tryAwaitRelease()
                            job.cancel()
                            scope.launch { progress.snapTo(0f) }
                        },
                    )
                },
        ) {
            CircularProgressIndicator(
                progress = { progress.value },
                strokeWidth = 3.dp,
                trackColor = Color.Transparent,
                modifier = Modifier.size(44.dp),
            )
            Text("⚙️", fontSize = 20.sp, modifier = Modifier.alpha(0.6f))
        }
        AnimatedVisibility(visible = showHint) {
            Text(
                "Grown-ups:\nhold for 3 seconds",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
}

@Composable
private fun ToolRow(row: List<Tool>, highlighted: Boolean, onOpenTool: (Tool) -> Unit) {
    val prefs = LocalPrefs.current
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
        row.forEach { ToolTile(it, highlighted, starred = it.name in LocalPrefs.current.favourites, onStar = { prefs.toggleFavourite(it) }, onClick = { onOpenTool(it) }, modifier = Modifier.weight(1f).fillMaxHeight()) }
        if (row.size == 1) Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun ToolTile(tool: Tool, highlighted: Boolean, starred: Boolean, onStar: () -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) tool.color.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (highlighted) 0.dp else 1.dp),
        modifier = Modifier.fillMaxSize().heightIn(min = 160.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(64.dp).background(tool.color.copy(alpha = 0.28f), CircleShape),
            ) { Text(tool.emoji, fontSize = 32.sp) }
            Spacer(Modifier.height(10.dp))
            Text(tool.title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, maxLines = 2)
            Text(
                tool.subtitle,
                minLines = 2,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
    // ☆ / ★ to add or remove from "My calm plan"
    IconButton(onClick = onStar, modifier = Modifier.align(Alignment.TopEnd)) {
        Text(
            if (starred) "★" else "☆",
            fontSize = 24.sp,
            color = if (starred) Color(0xFFE0A800) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { contentDescription = if (starred) "In my calm plan. Tap to remove." else "Add to my calm plan" },
        )
    }
    }
}

@Composable
private fun CardTile(card: CopingCard, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val photo = remember(card.photo) { card.photo?.let { Photos.load(context, it) } }
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Tool.CopingCard.color.copy(alpha = 0.22f)),
        modifier = modifier.heightIn(min = 160.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            if (photo != null) {
                Image(
                    photo.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.size(64.dp).clip(CircleShape),
                )
            } else {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp).background(Tool.CopingCard.color.copy(alpha = 0.3f), CircleShape)) {
                    Text("💛", fontSize = 32.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(card.text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        }
    }
}
