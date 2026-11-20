package com.calmcorner.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.isActive
import kotlin.math.ceil

enum class PhaseKind { BreatheIn, Hold, BreatheOut, Squeeze, Relax }

data class TimedPhase(
    val label: String,
    val seconds: Float,
    val kind: PhaseKind,
    /** What the voice says at the start of this phase (defaults to [label]). No emoji. */
    val spoken: String? = null,
)

/** Shared, frame-accurate timer that steps through a list of phases. */
@Stable
class PhaseTimerState(val phases: List<TimedPhase>) {
    var phaseIndex by mutableIntStateOf(0)
        internal set
    /** 0..1 progress through the current phase. */
    var progress by mutableFloatStateOf(0f)
        internal set
    /** Number of times the full list has been completed. */
    var rounds by mutableIntStateOf(0)
        internal set
    var finished by mutableStateOf(false)
        internal set

    val phase: TimedPhase get() = phases[phaseIndex]

    /** Whole seconds left in this phase, for a friendly countdown. */
    val secondsLeft: Int get() = ceil((1f - progress) * phase.seconds).toInt().coerceAtLeast(1)

    fun reset() {
        phaseIndex = 0
        progress = 0f
        rounds = 0
        finished = false
    }
}

@Composable
fun rememberPhaseTimer(
    phases: List<TimedPhase>,
    running: Boolean,
    loop: Boolean = true,
    /** Stop after this many full rounds (0 = no limit). */
    maxRounds: Int = 0,
    onPhaseStart: (TimedPhase) -> Unit = {},
): PhaseTimerState {
    val state = remember(phases) { PhaseTimerState(phases) }
    val latestOnPhaseStart by rememberUpdatedState(onPhaseStart)

    LaunchedEffect(state, running, maxRounds) {
        if (!running || state.finished) return@LaunchedEffect
        if (state.progress == 0f) latestOnPhaseStart(state.phase)
        var last = withFrameNanos { it }
        while (isActive && !state.finished) {
            val now = withFrameNanos { it }
            val dt = (now - last) / 1_000_000_000f
            last = now
            var p = state.progress + dt / state.phase.seconds
            if (p >= 1f) {
                p = 0f
                val next = state.phaseIndex + 1
                if (next >= state.phases.size) {
                    state.rounds++
                    if (!loop || (maxRounds > 0 && state.rounds >= maxRounds)) {
                        state.progress = 1f
                        state.finished = true
                        break
                    }
                    state.phaseIndex = 0
                } else {
                    state.phaseIndex = next
                }
                latestOnPhaseStart(state.phase)
            }
            state.progress = p
        }
    }
    return state
}
