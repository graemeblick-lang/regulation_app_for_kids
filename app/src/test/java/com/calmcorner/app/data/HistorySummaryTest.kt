package com.calmcorner.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistorySummaryTest {
    private val now = 1_800_000_000_000L
    private val hour = 60L * 60 * 1000

    private fun rec(hoursAgo: Long, tool: Tool, before: Zone?, after: Zone?) =
        SessionRecord(now - hoursAgo * hour, tool, before, after)

    @Test
    fun feltBetter_rules() {
        assertTrue(feltBetter(Zone.Yellow, Zone.Green))
        assertTrue(feltBetter(Zone.Red, Zone.Green))
        assertTrue(feltBetter(Zone.Red, Zone.Yellow))
        assertTrue(feltBetter(null, Zone.Green))
        assertFalse(feltBetter(Zone.Green, Zone.Green))
        assertFalse(feltBetter(Zone.Yellow, Zone.Red))
        assertFalse(feltBetter(Zone.Yellow, null))
        assertFalse(feltBetter(Zone.Blue, Zone.Yellow))
    }

    @Test
    fun summary_counts_only_this_week() {
        val records = listOf(
            rec(1, Tool.BoxBreathing, Zone.Yellow, Zone.Green),
            rec(2, Tool.BoxBreathing, Zone.Red, Zone.Red),
            rec(3, Tool.FingerBreathing, Zone.Yellow, Zone.Green),
            rec(5, Tool.BoxBreathing, Zone.Red, Zone.Green),
            rec(4, Tool.CalmJar, null, null),
            rec(24 * 8, Tool.BubblePop, Zone.Red, Zone.Green), // older than a week
        )
        val s = summarise(records, now)
        assertEquals(5, s.stars)
        assertEquals(Tool.BoxBreathing to 3, s.perTool.first())
        assertEquals(4, s.checkIns) // CalmJar had no colours
        assertEquals(3, s.feltBetter)
        assertEquals(Tool.BoxBreathing, s.mostHelpful)
    }

    @Test
    fun empty_history() {
        val s = summarise(emptyList(), now)
        assertEquals(0, s.stars)
        assertEquals(0, s.checkIns)
        assertNull(s.mostHelpful)
        assertNull(favouriteTool(emptyList()))
    }

    @Test
    fun favourite_is_most_used() {
        val records = listOf(
            rec(1, Tool.CalmJar, null, null),
            rec(2, Tool.BubblePop, null, null),
            rec(3, Tool.CalmJar, null, null),
        )
        assertEquals(Tool.CalmJar, favouriteTool(records))
    }
}
