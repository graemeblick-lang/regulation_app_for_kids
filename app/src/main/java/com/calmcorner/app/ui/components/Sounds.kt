package com.calmcorner.app.ui.components

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** Short sound effects (generated in-house, no third-party audio) with low latency. */
class SoundBank(context: Context, rawIds: List<Int>) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val ids = rawIds.associateWith { pool.load(context, it, 1) }

    fun play(rawId: Int, volume: Float = 0.8f, rate: Float = 1f) {
        ids[rawId]?.let { pool.play(it, volume, volume, 1, 0, rate) }
    }

    fun release() = pool.release()
}

@Composable
fun rememberSoundBank(vararg rawIds: Int): SoundBank {
    val context = LocalContext.current
    val bank = remember { SoundBank(context.applicationContext, rawIds.toList()) }
    DisposableEffect(Unit) { onDispose { bank.release() } }
    return bank
}
