package com.calmcorner.app.data

import androidx.compose.ui.graphics.Color
import com.calmcorner.app.ui.theme.CalmColors

/**
 * Wording rule for the whole app: short, simple words. Most kids using this
 * may find reading hard, especially when upset. Emoji give a picture cue.
 */
enum class Tool(
    val title: String,
    val subtitle: String,
    val emoji: String,
    val color: Color,
    val deep: Color,
    /** Fine in class / on the bus (school mode hides the others). */
    val quiet: Boolean = true,
    /** Shown in tool lists (false for internal entries). */
    val listed: Boolean = true,
) {
    BoxBreathing("Box Breathing", "Follow the dot around a square", "🟦", CalmColors.Sky, CalmColors.SkyDeep),
    FingerBreathing("Finger Breathing", "Trace your fingers and breathe", "🖐️", CalmColors.Peach, CalmColors.PeachDeep),
    BalloonBreathing("Balloon Breathing", "Breathe deep into your tummy", "🎈", CalmColors.Rose, CalmColors.RoseDeep),
    Grounding("Five Senses", "Look, touch, listen", "👀", CalmColors.Leaf, CalmColors.LeafDeep),
    SqueezeRelax("Squeeze and Relax", "Squeeze tight, then relax", "💪", CalmColors.Lavender, CalmColors.LavenderDeep),
    CalmJar("Calm Jar", "Shake it. Watch the glitter fall", "✨", CalmColors.Gold, CalmColors.GoldDeep),
    BubblePop("Bubble Pop", "Tap the bubbles to pop them", "🔵", CalmColors.Aqua, CalmColors.AquaDeep),
    CalmDrum("Calm Drum", "Tap a steady beat. Slow down together", "🥁", CalmColors.Teal, CalmColors.TealDeep),
    CalmTimer("Calm Timer", "Rest quietly for a few minutes", "⏳", CalmColors.Slate, CalmColors.SlateDeep),
    BodyMoves("Body Moves", "Push, press and stretch", "🙌", CalmColors.Sky, CalmColors.SkyDeep, quiet = false),
    TraceBreathe("Trace and Breathe", "Trace a shape slowly with your finger", "♾️", CalmColors.Gold, CalmColors.GoldDeep),
    HeartbeatDrum("Heartbeat Drum", "A slow drum to breathe with", "💓", CalmColors.Rose, CalmColors.RoseDeep),
    CalmSounds("Calm Sounds", "Rain, waves or a soft hum", "🌧️", CalmColors.Aqua, CalmColors.AquaDeep, quiet = false),
    /** Not a tool in the list: used to record a personal coping card. */
    CopingCard("My idea", "One of my own calm ideas", "💛", CalmColors.Teal, CalmColors.TealDeep, listed = false),
}

/**
 * Four colour states for checking in (a common way schools teach regulation).
 * We deliberately don't use any trademarked programme name in the app.
 */
/** Where on the body a clue is felt (used to light up the figure). */
enum class BodySpot { Face, Heart, Hands, Tummy, Shoulders, Legs }

enum class Zone(
    val label: String,
    val emoji: String,
    val words: String,
    val message: String,
    val color: Color,
    val suggestions: List<Tool>,
) {
    Blue("Blue", "😔", "Sad, tired, slow", "Sad or tired? These can help.",
        Color(0xFF4A7FC1), listOf(Tool.BodyMoves, Tool.SqueezeRelax, Tool.BubblePop)),
    Green("Green", "🙂", "Calm, happy, ready", "You feel calm. Practise a tool now.",
        Color(0xFF4F9D5D), listOf(Tool.BoxBreathing, Tool.TraceBreathe)),
    Yellow("Yellow", "😬", "Worried, wiggly, silly", "These can help you feel calm.",
        Color(0xFFE7B416), listOf(Tool.CalmDrum, Tool.FingerBreathing, Tool.Grounding)),
    Red("Red", "😡", "Very angry or scared", "Stop. Breathe. You are safe.",
        Color(0xFFD9534F), listOf(Tool.SqueezeRelax, Tool.CalmDrum, Tool.CalmJar)),
}

/** "How does this colour feel in my body?" Literal, simple words with a picture cue. */
data class BodyClue(val emoji: String, val text: String)

val Zone.clues: List<BodyClue>
    get() = when (this) {
        Zone.Red -> listOf(BodyClue("🔥", "My face feels hot"), BodyClue("💓", "My heart beats fast"), BodyClue("✊", "My hands make fists"), BodyClue("📢", "I want to shout or hit"))
        Zone.Yellow -> listOf(BodyClue("🦋", "My tummy feels wobbly"), BodyClue("🌀", "My body wants to move"), BodyClue("😬", "My shoulders feel tight"), BodyClue("💭", "My thoughts go fast"))
        Zone.Blue -> listOf(BodyClue("😴", "My body feels heavy and tired"), BodyClue("💧", "My eyes might cry"), BodyClue("🐌", "I move slowly"), BodyClue("🙁", "I want to be alone"))
        Zone.Green -> listOf(BodyClue("😌", "My breathing is slow"), BodyClue("🙂", "My face feels soft"), BodyClue("👂", "I can listen"), BodyClue("👍", "I feel OK"))
    }

val Zone.spots: List<BodySpot>
    get() = when (this) {
        Zone.Red -> listOf(BodySpot.Face, BodySpot.Heart, BodySpot.Hands)
        Zone.Yellow -> listOf(BodySpot.Tummy, BodySpot.Shoulders, BodySpot.Legs)
        Zone.Blue -> listOf(BodySpot.Face, BodySpot.Shoulders, BodySpot.Legs)
        Zone.Green -> listOf(BodySpot.Face, BodySpot.Heart)
    }
