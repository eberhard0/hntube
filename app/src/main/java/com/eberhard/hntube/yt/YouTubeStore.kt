package com.eberhard.hntube.yt

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eberhard.hntube.data.Video
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** What a fetch is up to, for one list. */
sealed interface Fetch<out T> {
    data object Idle : Fetch<Nothing>
    data object Loading : Fetch<Nothing>
    data class Ready<T>(val value: T) : Fetch<T>
    data class Failed(val message: String) : Fetch<Nothing>
}

/**
 * The signed-in account's playlists and their videos, fetched once per app run and kept in
 * memory so going back and forth doesn't call YouTube again. Pull-to-refresh is "Refresh".
 */
class YouTubeStore(context: Context) {
    val auth = YouTubeAuth(context)
    private val api = YouTubeApi(auth)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var playlists by mutableStateOf<Fetch<List<Playlist>>>(Fetch.Idle)
        private set
    var liked by mutableStateOf<Fetch<List<Video>>>(Fetch.Idle)
        private set
    private val videos = mutableStateMapOf<String, Fetch<List<Video>>>()
    private var signIn: Job? = null

    fun videosOf(playlistId: String): Fetch<List<Video>> = videos[playlistId] ?: Fetch.Idle

    fun startSignIn() {
        signIn?.cancel()
        signIn = scope.launch { auth.signIn() }
    }

    fun signOut() {
        signIn?.cancel()
        auth.signOut()
        playlists = Fetch.Idle
        liked = Fetch.Idle
        videos.clear()
    }

    fun loadPlaylists(force: Boolean = false) {
        if (!force && playlists !is Fetch.Idle && playlists !is Fetch.Failed) return
        playlists = Fetch.Loading
        scope.launch {
            playlists = runCatching { api.myPlaylists() }.fold({ Fetch.Ready(it) }, { Fetch.Failed(it.message ?: "Failed") })
        }
    }

    fun loadVideos(playlistId: String, force: Boolean = false) {
        val now = videos[playlistId]
        if (!force && now != null && now !is Fetch.Failed) return
        videos[playlistId] = Fetch.Loading
        scope.launch {
            videos[playlistId] = runCatching { api.playlistVideos(playlistId) }
                .fold({ Fetch.Ready(it) }, { Fetch.Failed(it.message ?: "Failed") })
        }
    }

    fun loadLiked(force: Boolean = false) {
        if (!force && liked !is Fetch.Idle && liked !is Fetch.Failed) return
        liked = Fetch.Loading
        scope.launch {
            liked = runCatching { api.likedVideos() }.fold({ Fetch.Ready(it) }, { Fetch.Failed(it.message ?: "Failed") })
        }
    }
}
