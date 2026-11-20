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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.calmcorner.app.data.LocalHistory
import com.calmcorner.app.data.SessionRecord
import com.calmcorner.app.data.Tool
import com.calmcorner.app.data.Zone
import com.calmcorner.app.ui.components.LocalVoice
import com.calmcorner.app.ui.screens.BalloonBreathingScreen
import com.calmcorner.app.ui.screens.BoxBreathingScreen
import com.calmcorner.app.ui.screens.BubblePopScreen
import com.calmcorner.app.ui.screens.CalmDrumScreen
import com.calmcorner.app.ui.screens.CalmJarScreen
import com.calmcorner.app.ui.screens.CalmTimerScreen
import com.calmcorner.app.ui.screens.CheckInScreen
import com.calmcorner.app.ui.screens.FingerBreathingScreen
import com.calmcorner.app.ui.screens.GroundingScreen
import com.calmcorner.app.ui.screens.GrownUpScreen
import com.calmcorner.app.ui.screens.HomeScreen
import com.calmcorner.app.ui.screens.SqueezeRelaxScreen

/** Screens, stored as a simple string so they survive rotation. */
private sealed interface Route {
    data object Home : Route
    data object GrownUps : Route
    data class Exercise(val tool: Tool) : Route
    data class CheckIn(val tool: Tool) : Route

    fun encode(): String = when (this) {
        Home -> "home"
        GrownUps -> "grownups"
        is Exercise -> "tool:${tool.name}"
        is CheckIn -> "checkin:${tool.name}"
    }

    companion object {
        fun decode(s: String): Route = when {
            s == "grownups" -> GrownUps
            s.startsWith("tool:") -> Tool.entries.firstOrNull { it.name == s.removePrefix("tool:") }?.let { Exercise(it) } ?: Home
            s.startsWith("checkin:") -> Tool.entries.firstOrNull { it.name == s.removePrefix("checkin:") }?.let { CheckIn(it) } ?: Home
            else -> Home
        }
    }
}

@Composable
fun CalmApp(openTool: Tool? = null, onOpenToolHandled: () -> Unit = {}) {
    val history = LocalHistory.current
    val voice = LocalVoice.current
    var routeName by rememberSaveable { mutableStateOf("home") }
    var zoneName by rememberSaveable { mutableStateOf<String?>(null) }
    var afterName by rememberSaveable { mutableStateOf<String?>(null) }
    val route = Route.decode(routeName)
    val zone = zoneName?.let { n -> Zone.entries.firstOrNull { it.name == n } }

    fun go(r: Route) {
        voice?.stop()
        routeName = r.encode()
    }
    fun openExercise(t: Tool) { afterName = null; go(Route.Exercise(t)) }
    val goHome = { go(Route.Home) }

    // Opened from the home-screen widget.
    LaunchedEffect(openTool) {
        if (openTool != null) { openExercise(openTool); onOpenToolHandled() }
    }

    BackHandler(enabled = route != Route.Home) { goHome() }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.safeDrawingPadding()) {
            AnimatedContent(
                targetState = route,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen",
            ) { current ->
                when (current) {
                    Route.Home -> HomeScreen(
                        zone = zone,
                        onZoneChange = { zoneName = it?.name },
                        onOpenTool = { openExercise(it) },
                        onOpenGrownUps = { go(Route.GrownUps) },
                    )
                    Route.GrownUps -> GrownUpScreen(onBack = goHome)
                    is Route.CheckIn -> CheckInScreen(
                        tool = current.tool,
                        after = afterName?.let { n -> Zone.entries.firstOrNull { it.name == n } },
                        onPick = { z ->
                            history.setAfter(z)
                            afterName = z?.name
                            zoneName = z?.name // home now shows the new colour
                        },
                        onTryTool = { openExercise(it) },
                        onHome = goHome,
                    )
                    is Route.Exercise -> {
                        val finish: () -> Unit = {
                            // Guard: only record once, even if two "finished" signals arrive.
                            if (routeName == current.encode()) {
                                history.add(SessionRecord(System.currentTimeMillis(), current.tool, before = zone, after = null))
                                go(Route.CheckIn(current.tool))
                            }
                        }
                        when (current.tool) {
                            Tool.BoxBreathing -> BoxBreathingScreen(goHome, finish)
                            Tool.FingerBreathing -> FingerBreathingScreen(goHome, finish)
                            Tool.BalloonBreathing -> BalloonBreathingScreen(goHome, finish)
                            Tool.Grounding -> GroundingScreen(goHome, finish)
                            Tool.SqueezeRelax -> SqueezeRelaxScreen(goHome, finish)
                            Tool.CalmJar -> CalmJarScreen(goHome, finish)
                            Tool.BubblePop -> BubblePopScreen(goHome, finish)
                            Tool.CalmTimer -> CalmTimerScreen(goHome, finish)
                            Tool.CalmDrum -> CalmDrumScreen(goHome, finish)
                        }
                    }
                }
            }
        }
    }
}
