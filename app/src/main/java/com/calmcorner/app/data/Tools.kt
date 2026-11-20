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
    BoxBreathing("Box Breathing", "Breathe around a square", "🟦", CalmColors.Sky, CalmColors.SkyDeep),
    FingerBreathing("Finger Breathing", "Trace your hand", "🖐️", CalmColors.Peach, CalmColors.PeachDeep),
    BalloonBreathing("Balloon Breathing", "Fill your tummy balloon", "🎈", CalmColors.Rose, CalmColors.RoseDeep),
    Grounding("Five Senses", "Look, touch, listen", "👀", CalmColors.Leaf, CalmColors.LeafDeep),
    SqueezeRelax("Squeeze and Relax", "Squeeze tight, then let go", "💪", CalmColors.Lavender, CalmColors.LavenderDeep),
}

enum class Feeling(
    val label: String,
    val emoji: String,
    val message: String,
    val suggestions: List<Tool>,
) {
    Angry("Angry", "😠", "It's OK to feel angry. Let's cool down.", listOf(Tool.SqueezeRelax, Tool.BoxBreathing)),
    Worried("Worried", "😟", "Let's help your body feel safe.", listOf(Tool.FingerBreathing, Tool.Grounding)),
    Sad("Sad", "😢", "It's OK to feel sad. Be kind to you.", listOf(Tool.BalloonBreathing, Tool.FingerBreathing)),
    Overwhelmed("Too much", "🤯", "Let's slow down together.", listOf(Tool.Grounding, Tool.BoxBreathing)),
    Wiggly("Wiggly", "🌀", "Let's use up that energy.", listOf(Tool.SqueezeRelax, Tool.BalloonBreathing)),
    Calm("Calm", "😌", "Great! Practise while you feel calm.", listOf(Tool.BoxBreathing, Tool.FingerBreathing)),
}
