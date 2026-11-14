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

class PreferredAccountStore(context: Context, userId: String, workspaceId: String) {
    private val preferences = context.applicationContext.getSharedPreferences("preferred_accounts", Context.MODE_PRIVATE)
    private val key = "${userId.length}:$userId:$workspaceId"

    fun get(): String? = preferences.getString(key, null)
    fun set(accountId: String?) {
        preferences.edit().apply {
            if (accountId == null) remove(key) else putString(key, accountId)
        }.apply()
    }
    fun observe(onChange: (String?) -> Unit): SharedPreferences.OnSharedPreferenceChangeListener {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changed ->
            if (changed == key) onChange(get())
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        return listener
    }
    fun removeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        preferences.unregisterOnSharedPreferenceChangeListener(listener)
    }
}

@Composable
fun rememberPreferredAccount(userId: String, workspaceId: String): Pair<String?, (String?) -> Unit> {
    val context = LocalContext.current
    val store = remember(context, userId, workspaceId) { PreferredAccountStore(context, userId, workspaceId) }
    var preferredId by remember(store) { mutableStateOf(store.get()) }
    DisposableEffect(store) {
        val listener = store.observe { preferredId = it }
        onDispose { store.removeListener(listener) }
    }
    return preferredId to { store.set(it) }
}