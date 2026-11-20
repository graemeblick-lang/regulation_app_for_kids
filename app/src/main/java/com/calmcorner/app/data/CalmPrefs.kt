package com.calmcorner.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.edit

enum class ThemeMode { System, Light, Dark }

/**
 * All settings and remembered choices. Stored only on this phone
 * (SharedPreferences); exposed as Compose state so the UI updates instantly.
 */
class CalmPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("calm_corner", Context.MODE_PRIVATE)

    var theme by mutableStateOf(ThemeMode.entries.getOrElse(prefs.getInt("theme", 0)) { ThemeMode.System })
        private set
    var voiceOn by mutableStateOf(prefs.getBoolean("voice", true))
        private set
    var vibrationOn by mutableStateOf(prefs.getBoolean("vibration", true))
        private set
    /** Breaths per session for the breathing tools. 0 = keep going until she stops. */
    var breathsPerSession by mutableIntStateOf(prefs.getInt("breaths", 5))
        private set
    var hiddenTools by mutableStateOf(prefs.getStringSet("hidden_tools", emptySet())!!.toSet())
        private set
    var balloonColor by mutableIntStateOf(prefs.getInt("balloon_color", 0))
        private set

    fun updateTheme(v: ThemeMode) { theme = v; prefs.edit { putInt("theme", v.ordinal) } }
    fun updateVoice(v: Boolean) { voiceOn = v; prefs.edit { putBoolean("voice", v) } }
    fun updateVibration(v: Boolean) { vibrationOn = v; prefs.edit { putBoolean("vibration", v) } }
    fun updateBreaths(v: Int) { breathsPerSession = v; prefs.edit { putInt("breaths", v) } }
    fun updateBalloonColor(v: Int) { balloonColor = v; prefs.edit { putInt("balloon_color", v) } }

    fun setToolHidden(tool: Tool, hidden: Boolean) {
        val next = if (hidden) hiddenTools + tool.name else hiddenTools - tool.name
        // Always keep at least one tool visible.
        if (Tool.entries.all { it.name in next }) return
        hiddenTools = next
        prefs.edit { putStringSet("hidden_tools", next) }
    }

    fun visibleTools(): List<Tool> = Tool.entries.filterNot { it.name in hiddenTools }

    /** Speed is remembered per tool (seconds per breath). */
    fun speedFor(tool: Tool): Int = prefs.getInt("speed_${tool.name}", 4)
    fun saveSpeed(tool: Tool, seconds: Int) = prefs.edit { putInt("speed_${tool.name}", seconds) }

    /** Calm timer length in minutes, remembered. */
    fun timerMinutes(): Int = prefs.getInt("timer_minutes", 2)
    fun saveTimerMinutes(m: Int) = prefs.edit { putInt("timer_minutes", m) }
}

val LocalPrefs = staticCompositionLocalOf<CalmPrefs> { error("CalmPrefs not provided") }
