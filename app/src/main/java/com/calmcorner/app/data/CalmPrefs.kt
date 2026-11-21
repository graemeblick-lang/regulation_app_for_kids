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
    private val systemReducedMotion = try {
        android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    } catch (e: Exception) { false }

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

    /** Which voice speaks (grown-ups choose). */
    var voicePreset by mutableStateOf(VoicePreset.fromName(prefs.getString("voice_preset", null)))
        private set
    fun updateVoicePreset(v: VoicePreset) { voicePreset = v; prefs.edit { putString("voice_preset", v.name) } }

    fun updateBubbleMotion(v: Int) { bubbleMotion = v; prefs.edit { putInt("bubble_motion", v) } }
    fun updateBalloonColor(v: Int) { balloonColor = v; prefs.edit { putInt("balloon_color", v) } }

    /** Tools she has starred (shown first in "My calm plan"). */
    var favourites by mutableStateOf(prefs.getStringSet("favourites", emptySet())!!.toSet())
        private set
    fun toggleFavourite(tool: Tool) {
        val next = if (tool.name in favourites) favourites - tool.name else favourites + tool.name
        favourites = next
        prefs.edit { putStringSet("favourites", next) }
    }

    /** School mode: silent, and hides tools that aren't suited to class. */
    var schoolMode by mutableStateOf(prefs.getBoolean("school_mode", false))
        private set
    fun updateSchoolMode(v: Boolean) { schoolMode = v; prefs.edit { putBoolean("school_mode", v) } }

    /** All sound is allowed only if the sound switch is on and school mode is off. */
    val soundOn: Boolean get() = voiceOn && !schoolMode

    /** Less movement (defaults to the phone's "remove animations" setting). */
    var reduceMotion by mutableStateOf(prefs.getBoolean("reduce_motion", systemReducedMotion))
        private set
    fun updateReduceMotion(v: Boolean) { reduceMotion = v; prefs.edit { putBoolean("reduce_motion", v) } }

    /** Bigger text and buttons everywhere. */
    var bigger by mutableStateOf(prefs.getBoolean("bigger", false))
        private set
    fun updateBigger(v: Boolean) { bigger = v; prefs.edit { putBoolean("bigger", v) } }

    /** Daily check-in reminder slots that are switched on. */
    var reminders by mutableStateOf(prefs.getStringSet("reminders", emptySet())!!.toSet())
        private set
    fun setReminder(slot: String, on: Boolean) {
        val next = if (on) reminders + slot else reminders - slot
        reminders = next
        prefs.edit { putStringSet("reminders", next) }
    }

    /** Her calm picture (shown when she picks Red). Stored as a file name in app storage. */
    var calmPicture by mutableStateOf(prefs.getString("calm_picture", null))
        private set
    var calmPictureCaption by mutableStateOf(prefs.getString("calm_picture_caption", "My calm place") ?: "My calm place")
        private set
    fun updateCalmPicture(file: String?) { calmPicture = file; prefs.edit { putString("calm_picture", file) } }
    fun updateCalmPictureCaption(v: String) { calmPictureCaption = v; prefs.edit { putString("calm_picture_caption", v) } }

    /** Trace and Breathe shape: 0 = loop, 1 = star. */
    var traceShape by mutableIntStateOf(prefs.getInt("trace_shape", 0))
        private set
    fun updateTraceShape(v: Int) { traceShape = v; prefs.edit { putInt("trace_shape", v) } }

    fun setToolHidden(tool: Tool, hidden: Boolean) {
        val next = if (hidden) hiddenTools + tool.name else hiddenTools - tool.name
        // Always keep at least one tool visible.
        if (Tool.entries.filter { it.listed }.all { it.name in next }) return
        hiddenTools = next
        prefs.edit { putStringSet("hidden_tools", next) }
    }

    fun visibleTools(): List<Tool> = Tool.entries.filter {
        it.listed && it.name !in hiddenTools && (!schoolMode || it.quiet)
    }

    /** Speed is remembered per tool (seconds per breath). */
    fun speedFor(tool: Tool): Int = prefs.getInt("speed_${tool.name}", 4)
    fun saveSpeed(tool: Tool, seconds: Int) = prefs.edit { putInt("speed_${tool.name}", seconds) }

    /** Calm timer length in minutes, remembered. */
    fun timerMinutes(): Int = prefs.getInt("timer_minutes", 2)
    fun saveTimerMinutes(m: Int) = prefs.edit { putInt("timer_minutes", m) }
}

val LocalPrefs = staticCompositionLocalOf<CalmPrefs> { error("CalmPrefs not provided") }
