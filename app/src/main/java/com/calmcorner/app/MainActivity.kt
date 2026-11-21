package com.calmcorner.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.calmcorner.app.data.CalmPrefs
import com.calmcorner.app.data.HistoryStore
import com.calmcorner.app.data.CopingCardStore
import com.calmcorner.app.data.LocalCopingCards
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.calmcorner.app.data.LocalHistory
import com.calmcorner.app.data.LocalPrefs
import com.calmcorner.app.data.ThemeMode
import com.calmcorner.app.data.Tool
import com.calmcorner.app.data.favouriteTool
import com.calmcorner.app.ui.CalmApp
import com.calmcorner.app.ui.components.LocalVoice
import com.calmcorner.app.ui.components.Voice
import com.calmcorner.app.ui.theme.CalmTheme

class MainActivity : ComponentActivity() {
    private lateinit var prefs: CalmPrefs
    private lateinit var history: HistoryStore
    private lateinit var cards: CopingCardStore
    private var voice: Voice? = null
    private var pendingTool by mutableStateOf<Tool?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        prefs = CalmPrefs(this)
        history = HistoryStore(this)
        cards = CopingCardStore(this)
        Reminders.reschedule(this, prefs.reminders) // keep alarms in step with settings
        voice = Voice(this)
        if (savedInstanceState == null) handleIntent(intent)

        setContent {
            val dark = when (prefs.theme) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            // "Bigger text and buttons" scales everything a little.
            val density = LocalDensity.current
            val scaled = if (prefs.bigger) Density(density.density * 1.12f, density.fontScale * 1.1f) else density
            CompositionLocalProvider(
                LocalPrefs provides prefs,
                LocalHistory provides history,
                LocalCopingCards provides cards,
                LocalVoice provides voice,
                LocalDensity provides scaled,
            ) {
                CalmTheme(darkTheme = dark) {
                    CalmApp(openTool = pendingTool, onOpenToolHandled = { pendingTool = null })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** The home-screen widget opens her favourite (most used) tool directly. */
    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_FAVOURITE, false) == true) {
            val visible = prefs.visibleTools()
            pendingTool = favouriteTool(history.records)?.takeIf { it in visible }
                ?: Tool.BoxBreathing.takeIf { it in visible }
                ?: visible.first()
        }
    }

    override fun onDestroy() {
        voice?.shutdown()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_OPEN_FAVOURITE = "open_favourite"
    }
}
