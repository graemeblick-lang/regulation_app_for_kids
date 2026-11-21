package com.calmcorner.app.ui.components

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

enum class CalmSound(val emoji: String, val label: String) {
    Rain("🌧️", "Rain"),
    Waves("🌊", "Waves"),
    Hum("🤍", "Soft hum"),
}

/**
 * Generates gentle background sound live (no recordings, nothing downloaded):
 * rain = soft hiss with tiny drops, waves = deep noise that swells every ~9s,
 * hum = warm "brown" noise. Fades in and out smoothly.
 */
class NoiseSynth {
    private val rate = 22050
    @Volatile private var running = false
    @Volatile var sound: CalmSound = CalmSound.Rain
    /** 0..1 target volume (used for fade in/out). */
    @Volatile var targetVolume = 0f
    private var thread: Thread? = null

    fun start() {
        if (running) return
        running = true
        thread = Thread({ loop() }, "calm-sounds").apply { start() }
    }

    fun stop() {
        running = false
        thread?.join(500)
        thread = null
    }

    private fun loop() {
        val minBuf = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(maxOf(minBuf, rate / 5 * 2))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track.play()
        val buf = ShortArray(1024)
        var volume = 0f
        var brown = 0.0
        var lp1 = 0.0
        var lp2 = 0.0
        var hp = 0.0
        var prev = 0.0
        var drop = 0.0
        var t = 0L
        while (running) {
            for (i in buf.indices) {
                volume += (targetVolume - volume) * 0.00005f // slow, smooth fades
                val white = Random.nextDouble() * 2 - 1
                val v = when (sound) {
                    CalmSound.Hum -> {
                        brown = (brown + white * 0.02).coerceIn(-1.0, 1.0) * 0.998
                        lp1 += 0.05 * (brown - lp1)
                        lp1 * 3.0
                    }
                    CalmSound.Waves -> {
                        brown = (brown + white * 0.03).coerceIn(-1.0, 1.0) * 0.997
                        lp2 += 0.08 * (brown - lp2)
                        val swell = 0.35 + 0.65 * (0.5 + 0.5 * sin(2 * PI * t / (rate * 9.0)))
                        lp2 * 2.6 * swell * swell
                    }
                    CalmSound.Rain -> {
                        // Soft hiss: lightly filtered white noise
                        lp1 += 0.35 * (white - lp1)
                        hp = 0.97 * (hp + lp1 - prev)
                        prev = lp1
                        // Occasional tiny drops
                        if (Random.nextInt(rate / 18) == 0) drop = 0.6 + Random.nextDouble() * 0.4
                        drop *= 0.992
                        hp * 0.35 + drop * white * 0.25
                    }
                }
                buf[i] = (v.coerceIn(-1.0, 1.0) * volume * 0.5 * Short.MAX_VALUE).toInt().toShort()
                t++
            }
            track.write(buf, 0, buf.size)
        }
        track.stop()
        track.release()
    }
}
