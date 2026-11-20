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
    /** All app sound: voice guidance and bubble pops. Also toggled by the 🔊 button. */
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
    /** Bubble Pop movement, set by grown-ups only: 0 = still, 1 = calm, 2 = lively. */
    var bubbleMotion by mutableIntStateOf(prefs.getInt("bubble_motion", 1))
        private set

    fun updateTheme(v: ThemeMode) { theme = v; prefs.edit { putInt("theme", v.ordinal) } }
    fun updateVoice(v: Boolean) { voiceOn = v; prefs.edit { putBoolean("voice", v) } }
    fun updateVibration(v: Boolean) { vibrationOn = v; prefs.edit { putBoolean("vibration", v) } }
    fun updateBreaths(v: Int) { breathsPerSession = v; prefs.edit { putInt("breaths", v) } }
    /** Calm Drum (grown-ups only): the slow speed it ends on, and how long it takes to get there. */
    var drumEndBpm by mutableIntStateOf(prefs.getInt("drum_end_bpm", 60))
        private set
    var drumLength by mutableIntStateOf(prefs.getInt("drum_length", 1)) // 0 short, 1 normal, 2 long
        private set
    /** Audible tick on the guide beat. Off by default: the ring and buzz are enough. */
    var drumTick by mutableStateOf(prefs.getBoolean("drum_tick", false))
        private set
    fun updateDrumTick(v: Boolean) { drumTick = v; prefs.edit { putBoolean("drum_tick", v) } }
    fun updateDrumEndBpm(v: Int) { drumEndBpm = v; prefs.edit { putInt("drum_end_bpm", v) } }
    fun updateDrumLength(v: Int) { drumLength = v; prefs.edit { putInt("drum_length", v) } }
    /** Number of beats spent slowing down. */
    fun drumSlowBeats(): Int = when (drumLength) { 0 -> 24; 2 -> 60; else -> 40 }

    fun updateBubbleMotion(v: Int) { bubbleMotion = v; prefs.edit { putInt("bubble_motion", v) } }
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
