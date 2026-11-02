package com.eberhard.hntube.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What the user did with videos: watched, and saved for later. Kept on the phone in
 * SharedPreferences, like HackerTube keeps its browsing state in the browser.
 */
class Marks(context: Context) {
    private val prefs = context.getSharedPreferences("marks", Context.MODE_PRIVATE)

    private val _watched = MutableStateFlow(prefs.getStringSet(KEY_WATCHED, emptySet())!!.toSet())
    val watched: StateFlow<Set<String>> = _watched.asStateFlow()

    // Newest first; a plain string keeps the order (a StringSet would not).
    private val _later = MutableStateFlow(prefs.getString(KEY_LATER, "")!!.split(',').filter { it.isNotBlank() })
    val later: StateFlow<List<String>> = _later.asStateFlow()

    fun setWatched(id: String, on: Boolean) {
        val next = if (on) _watched.value + id else _watched.value - id
        _watched.value = next
        prefs.edit().putStringSet(KEY_WATCHED, next).apply()
    }

    fun setLater(id: String, on: Boolean) {
        val next = if (on) listOf(id) + (_later.value - id) else _later.value - id
        _later.value = next
        prefs.edit().putString(KEY_LATER, next.joinToString(",")).apply()
    }

    private companion object {
        const val KEY_WATCHED = "watched"
        const val KEY_LATER = "later"
    }
}
