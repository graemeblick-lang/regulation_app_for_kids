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

/**
 * Spoken guidance using the phone's built-in text-to-speech engine.
 * Works offline on most phones; the app itself needs no internet permission.
 */
class Voice(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    private var ready = false

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        val result = tts.setLanguage(Locale.getDefault())
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.setLanguage(Locale.UK)
        }
        tts.setSpeechRate(0.85f) // a little slower than normal: calm and clear
        ready = true
    }

    fun speak(text: String) {
        if (ready) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "calm")
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
        if (prefs.voiceOn) voice?.speak(text)
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
