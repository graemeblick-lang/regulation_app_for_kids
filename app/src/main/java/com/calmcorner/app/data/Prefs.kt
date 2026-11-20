package com.calmcorner.app.data

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

/** Small on-device settings (never leave the phone). */
@Composable
fun rememberIntPref(key: String, default: Int): MutableIntState {
    val prefs = LocalContext.current.getSharedPreferences("calm_corner", Context.MODE_PRIVATE)
    val state = remember(key) { mutableIntStateOf(prefs.getInt(key, default)) }
    LaunchedEffect(key) {
        snapshotFlow { state.intValue }.collect { prefs.edit().putInt(key, it).apply() }
    }
    return state
}
