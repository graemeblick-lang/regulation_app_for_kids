package com.calmcorner.app.ui.components

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.calmcorner.app.data.CalmPrefs
import com.calmcorner.app.data.LocalPrefs
import java.util.Locale
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.calmcorner.app.data.VoicePreset
import com.calmcorner.app.data.guessGender

/** How well a voice option can be honoured on this phone. */
enum class VoiceMatch { Exact, Closest, Missing }

/**
 * Spoken guidance using the phone's built-in text-to-speech engine.
 * Works offline on most phones; the app itself needs no internet permission.
 * A grown-up picks a [VoicePreset]; we map it onto an installed voice.
 */
class Voice(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    /** Compose state so settings update once the engine is ready. */
    var ready by mutableStateOf(false)
        private set
    private var applied: VoicePreset? = null

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        ready = true
        apply(VoicePreset.PhoneDefault)
    }

    private fun offlineEnglishVoices(): List<android.speech.tts.Voice> = try {
        tts.voices.orEmpty().filter {
            it.locale.language == "en" &&
                !it.isNetworkConnectionRequired &&
                TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features
        }
    } catch (e: Exception) {
        emptyList()
    }

    /** Finds the installed voice for a preset, and how good the match is. */
    fun resolve(preset: VoicePreset): Pair<android.speech.tts.Voice?, VoiceMatch> {
        if (preset.country == null) return null to VoiceMatch.Exact
        val all = offlineEnglishVoices()
        val inAccent = all.filter { it.locale.country.equals(preset.country, ignoreCase = true) }
        val pool = if (inAccent.isEmpty() && preset.anyAccentOk) all else inAccent
        if (pool.isEmpty()) return null to VoiceMatch.Missing
        val gendered = pool.filter { guessGender(it.name) == preset.gender }
        val best = (gendered.ifEmpty { pool }).maxByOrNull { it.quality }
        val exact = gendered.isNotEmpty() && (inAccent.isNotEmpty() || preset.anyAccentOk)
        return best to if (exact) VoiceMatch.Exact else VoiceMatch.Closest
    }

    fun matchFor(preset: VoicePreset): VoiceMatch = if (!ready) VoiceMatch.Missing else resolve(preset).second

    private fun apply(preset: VoicePreset) {
        if (!ready || preset == applied) return
        val (voice, _) = resolve(preset)
        if (voice != null) {
            tts.voice = voice
        } else {
            val result = tts.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) tts.setLanguage(Locale.UK)
        }
        tts.setPitch(preset.pitch)
        tts.setSpeechRate(0.85f * preset.rate) // a little slower than normal: calm and clear
        applied = preset
    }

    fun speak(text: String, preset: VoicePreset) {
        if (!ready) return
        apply(preset)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "calm")
    }

    fun stop() {
        if (ready) tts.stop()
    }

    fun shutdown() = tts.shutdown()
}

val LocalVoice = staticCompositionLocalOf<Voice?> { null }

/** Voice + vibration cues that respect the user's settings (and the quick mute button). */
class Cues(private val voice: Voice?, private val prefs: CalmPrefs, private val haptic: HapticFeedback) {
    fun buzz() {
        if (prefs.vibrationOn) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun say(text: String) {
        if (prefs.voiceOn) voice?.speak(text, prefs.voicePreset)
    }

    /** Called at the start of each breathing / squeeze phase. */
    fun phase(p: TimedPhase) {
        buzz()
        say(p.spoken ?: p.label)
    }
}

@Composable
fun rememberCues(): Cues {
    val voice = LocalVoice.current
    val prefs = LocalPrefs.current
    val haptic = LocalHapticFeedback.current
    return remember(voice, prefs, haptic) { Cues(voice, prefs, haptic) }
}
