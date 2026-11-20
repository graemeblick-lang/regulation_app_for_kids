package com.calmcorner.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RhythmTest {
    @Test
    fun needs_enough_taps() {
        assertNull(Rhythm.estimateBpm(listOf(0f, 0.5f, 1f)))
    }

    @Test
    fun steady_taps_give_her_tempo() {
        val taps = List(8) { it * 0.5f } // a tap every half second = 120 bpm
        assertEquals(120f, Rhythm.estimateBpm(taps)!!, 0.5f)
    }

    @Test
    fun one_stray_tap_does_not_throw_it_off() {
        val taps = listOf(0f, 0.75f, 1.5f, 2.25f, 2.4f, 3.0f, 3.75f, 4.5f) // ~80 bpm with one extra tap
        val bpm = Rhythm.estimateBpm(taps)!!
        assertTrue("bpm was $bpm", bpm in 70f..95f)
    }

    @Test
    fun very_fast_taps_are_capped() {
        val taps = List(8) { it * 0.2f } // 300 bpm
        assertEquals(Rhythm.MAX_START_BPM, Rhythm.estimateBpm(taps)!!, 0.01f)
    }

    @Test
    fun tempo_slows_smoothly_to_target() {
        val start = 100f
        val target = 60f
        assertEquals(100f, Rhythm.tempoAt(0, start, target, 40), 0.01f)
        assertEquals(80f, Rhythm.tempoAt(20, start, target, 40), 0.5f)
        assertEquals(60f, Rhythm.tempoAt(40, start, target, 40), 0.01f)
        assertEquals(60f, Rhythm.tempoAt(99, start, target, 40), 0.01f)
        // Always getting slower, never faster
        var prev = Float.MAX_VALUE
        for (b in 0..40) {
            val t = Rhythm.tempoAt(b, start, target, 40)
            assertTrue(t <= prev)
            prev = t
        }
    }

    @Test
    fun already_slow_stays_put() {
        assertEquals(55f, Rhythm.tempoAt(10, 55f, 60f, 40), 0.01f)
    }

    @Test
    fun in_time_window() {
        assertTrue(Rhythm.inTime(10.05f, 10f, 60f))
        assertFalse(Rhythm.inTime(10.4f, 10f, 60f))
    }
}
