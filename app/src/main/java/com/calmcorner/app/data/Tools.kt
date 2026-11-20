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
) {
    BoxBreathing("Box Breathing", "Follow the dot around a square", "🟦", CalmColors.Sky, CalmColors.SkyDeep),
    FingerBreathing("Finger Breathing", "Trace your fingers and breathe", "🖐️", CalmColors.Peach, CalmColors.PeachDeep),
    BalloonBreathing("Balloon Breathing", "Breathe deep into your tummy", "🎈", CalmColors.Rose, CalmColors.RoseDeep),
    Grounding("Five Senses", "Look, touch, listen", "👀", CalmColors.Leaf, CalmColors.LeafDeep),
    SqueezeRelax("Squeeze and Relax", "Squeeze tight, then relax", "💪", CalmColors.Lavender, CalmColors.LavenderDeep),
}

/**
 * Four colour states for checking in (a common way schools teach regulation).
 * We deliberately don't use any trademarked programme name in the app.
 */
enum class Zone(
    val label: String,
    val emoji: String,
    val words: String,
    val message: String,
    val color: Color,
    val suggestions: List<Tool>,
) {
    Blue("Blue", "😔", "Sad, tired, slow", "Sad or tired? These can help.",
        Color(0xFF4A7FC1), listOf(Tool.SqueezeRelax, Tool.BalloonBreathing)),
    Green("Green", "🙂", "Calm, happy, ready", "You feel calm. Practise a tool now.",
        Color(0xFF4F9D5D), listOf(Tool.BoxBreathing, Tool.FingerBreathing)),
    Yellow("Yellow", "😬", "Worried, wiggly, silly", "These can help you feel calm.",
        Color(0xFFE7B416), listOf(Tool.FingerBreathing, Tool.Grounding, Tool.BoxBreathing)),
    Red("Red", "😡", "Very angry or scared", "Stop. Breathe. You are safe.",
        Color(0xFFD9534F), listOf(Tool.SqueezeRelax, Tool.BoxBreathing)),
}
