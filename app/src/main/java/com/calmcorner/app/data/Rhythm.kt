package com.calmcorner.app.data

import kotlin.math.cos
import kotlin.math.PI

/** Pure rhythm maths for Calm Drum (no Android), so it can be unit tested. */
object Rhythm {
    const val MIN_START_BPM = 50f
    const val MAX_START_BPM = 140f
    const val DEFAULT_START_BPM = 80f

    /**
     * Her tempo from tap times (seconds): median of the recent gaps, so one
     * stray tap doesn't throw it off. Null until there are enough taps.
     */
    fun estimateBpm(tapTimes: List<Float>, minTaps: Int = 6): Float? {
        if (tapTimes.size < minTaps) return null
        val gaps = tapTimes.takeLast(minTaps).zipWithNext { a, b -> b - a }.filter { it > 0.15f && it < 2.5f }
        if (gaps.size < minTaps - 2) return null
        val median = gaps.sorted()[gaps.size / 2]
        return (60f / median).coerceIn(MIN_START_BPM, MAX_START_BPM)
    }

    /** Tempo for beat [beat] while slowing from [start] to [target] over [slowBeats] beats (smooth ease). */
    fun tempoAt(beat: Int, start: Float, target: Float, slowBeats: Int): Float {
        if (start <= target) return start // already slow: never speed her up
        if (slowBeats <= 0 || beat >= slowBeats) return target
        return start + (target - start) * ease(beat.toFloat() / slowBeats)
    }

    private fun ease(x: Float): Float = ((1 - cos(PI * x)) / 2).toFloat()

    /** True if a tap is close enough to a beat to count as "in time" (only used for a subtle glow). */
    fun inTime(tap: Float, beat: Float, bpm: Float): Boolean =
        kotlin.math.abs(tap - beat) < minOf(0.15f, 60f / bpm * 0.2f)
}
