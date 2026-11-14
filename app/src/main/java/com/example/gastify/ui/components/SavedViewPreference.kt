package com.example.gastify.ui.components

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberSavedViewPreference(userId: String, name: String, defaultValue: Boolean): Pair<Boolean, (Boolean) -> Unit> {
    val context = LocalContext.current.applicationContext
    val preferences = remember(context) { context.getSharedPreferences("gastify_view_preferences", Context.MODE_PRIVATE) }
    val key = remember(userId, name) { "${userId.length}:$userId:$name" }
    var value by remember(preferences, key) { mutableStateOf(preferences.getBoolean(key, defaultValue)) }
    DisposableEffect(preferences, key) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == key) value = preferences.getBoolean(key, defaultValue)
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return value to { newValue ->
        value = newValue
        preferences.edit().putBoolean(key, newValue).apply()
    }
}