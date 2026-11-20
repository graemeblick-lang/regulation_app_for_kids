package com.calmcorner.app.data

import androidx.compose.ui.graphics.Color
import com.calmcorner.app.ui.theme.CalmColors

enum class Tool(
    val title: String,
    val subtitle: String,
    val emoji: String,
    val color: Color,
) {
    BoxBreathing("Box Breathing", "Breathe around the square", "🟦", CalmColors.Sky),
    FingerBreathing("Five Finger Breathing", "Trace your hand slowly", "🖐️", CalmColors.Peach),
    BalloonBreathing("Balloon Breathing", "Fill your belly like a balloon", "🎈", CalmColors.Rose),
    Grounding("5-4-3-2-1 Senses", "Notice the world around you", "👀", CalmColors.Leaf),
    SqueezeRelax("Squeeze & Relax", "Tighten, then let your muscles go floppy", "💪", CalmColors.Lavender),
}

enum class Feeling(
    val label: String,
    val emoji: String,
    val message: String,
    val suggestions: List<Tool>,
) {
    Angry(
        "Angry", "😠",
        "Angry feelings are okay. Let's help your body cool down.",
        listOf(Tool.SqueezeRelax, Tool.BoxBreathing),
    ),
    Worried(
        "Worried", "😟",
        "Worries can feel big. Slow breaths tell your body it's safe.",
        listOf(Tool.FingerBreathing, Tool.Grounding),
    ),
    Sad(
        "Sad", "😢",
        "It's okay to feel sad. Let's be gentle with ourselves.",
        listOf(Tool.BalloonBreathing, Tool.FingerBreathing),
    ),
    Overwhelmed(
        "Too much", "🤯",
        "When everything feels like too much, let's come back to right here.",
        listOf(Tool.Grounding, Tool.BoxBreathing),
    ),
    Wiggly(
        "Wiggly", "🌀",
        "Lots of energy in your body? Let's give it somewhere to go.",
        listOf(Tool.SqueezeRelax, Tool.BalloonBreathing),
    ),
    Calm(
        "Calm", "😌",
        "Lovely! Practising when you're calm makes it easier when feelings get big.",
        listOf(Tool.BoxBreathing, Tool.FingerBreathing),
    ),
}
