package com.calmcorner.app.data

/** Pure summary maths (no Android), so it can be unit tested. */
data class WeekSummary(
    val stars: Int,
    val perTool: List<Pair<Tool, Int>>,
    val checkIns: Int,
    val feltBetter: Int,
    val mostHelpful: Tool?,
)

const val WEEK_MS = 7L * 24 * 60 * 60 * 1000

/** "Felt better" = she ended Green, or moved from Red to Yellow. */
fun feltBetter(before: Zone?, after: Zone?): Boolean = when {
    after == null -> false
    after == Zone.Green -> before != Zone.Green
    before == Zone.Red && after == Zone.Yellow -> true
    else -> false
}

fun summarise(records: List<SessionRecord>, now: Long): WeekSummary {
    val week = records.filter { now - it.time in 0 until WEEK_MS }
    val perTool = week.groupingBy { it.tool }.eachCount().toList().sortedByDescending { it.second }
    val checked = week.filter { it.before != null && it.after != null }
    val better = checked.filter { feltBetter(it.before, it.after) }
    val mostHelpful = better.groupingBy { it.tool }.eachCount().maxByOrNull { it.value }?.key
    return WeekSummary(
        stars = week.size,
        perTool = perTool,
        checkIns = checked.size,
        feltBetter = better.size,
        mostHelpful = mostHelpful,
    )
}

/** The tool she uses most overall (used by the home-screen widget). */
fun favouriteTool(records: List<SessionRecord>): Tool? =
    records.groupingBy { it.tool }.eachCount().maxByOrNull { it.value }?.key
