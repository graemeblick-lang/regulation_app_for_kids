package com.calmcorner.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.calmcorner.app.data.Feeling
import com.calmcorner.app.data.Tool
import com.calmcorner.app.ui.screens.BalloonBreathingScreen
import com.calmcorner.app.ui.screens.BoxBreathingScreen
import com.calmcorner.app.ui.screens.FingerBreathingScreen
import com.calmcorner.app.ui.screens.GroundingScreen
import com.calmcorner.app.ui.screens.HomeScreen
import com.calmcorner.app.ui.screens.SqueezeRelaxScreen

@Composable
fun CalmApp() {
    // Stored as names so they survive rotation / process death.
    var toolName by rememberSaveable { mutableStateOf<String?>(null) }
    var feelingName by rememberSaveable { mutableStateOf<String?>(null) }
    val tool = toolName?.let { Tool.valueOf(it) }
    val goHome = { toolName = null }

    BackHandler(enabled = tool != null) { goHome() }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.safeDrawingPadding()) {
            AnimatedContent(
                targetState = tool,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen",
            ) { current ->
                when (current) {
                    null -> HomeScreen(
                        feeling = feelingName?.let { Feeling.valueOf(it) },
                        onFeelingChange = { feelingName = it?.name },
                        onOpenTool = { toolName = it.name },
                    )
                    Tool.BoxBreathing -> BoxBreathingScreen(goHome)
                    Tool.FingerBreathing -> FingerBreathingScreen(goHome)
                    Tool.BalloonBreathing -> BalloonBreathingScreen(goHome)
                    Tool.Grounding -> GroundingScreen(goHome)
                    Tool.SqueezeRelax -> SqueezeRelaxScreen(goHome)
                }
            }
        }
    }
}
