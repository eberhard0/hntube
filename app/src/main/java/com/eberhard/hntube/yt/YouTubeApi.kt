package com.eberhard.hntube.yt

import com.eberhard.hntube.data.Video
import com.eberhard.hntube.data.plain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** A playlist of the signed-in account, including private ones. */
data class Playlist(val id: String, val title: String, val count: Int, val thumbnail: String?)

/**
 * The few YouTube Data API calls HNTube needs, all read-only. The API never returns Watch
 * Later or History (Google removed them in 2016); Liked videos comes from videos?myRating=like.
 */
class YouTubeApi(private val auth: YouTubeAuth) {

    suspend fun myPlaylists(): List<Playlist> {
        val out = ArrayList<Playlist>()
        var page: String? = null
        do {
            val json = get("playlists", mapOf("part" to "snippet,contentDetails", "mine" to "true", "maxResults" to "50"), page)
            val items = json.optJSONArray("items") ?: break
            for (i in 0 until items.length()) {
                val p = items.getJSONObject(i)
                val snippet = p.getJSONObject("snippet")
                out += Playlist(
                    id = p.getString("id"),
                    title = snippet.optString("title"),
                    count = p.optJSONObject("contentDetails")?.optInt("itemCount") ?: 0,
                    thumbnail = thumb(snippet.optJSONObject("thumbnails")),
                )
            }
            page = json.optString("nextPageToken").ifEmpty { null }
        } while (page != null)
        return out
    }

    /** Every video in [playlistId], in playlist order; deleted/private-to-others videos are skipped. */
    suspend fun playlistVideos(playlistId: String): List<Video> {
        val out = ArrayList<Video>()
        var page: String? = null
        do {
            val json = get(
                "playlistItems",
                mapOf("part" to "snippet,contentDetails", "playlistId" to playlistId, "maxResults" to "50"),
                page,
            )
            val items = json.optJSONArray("items") ?: break
            for (i in 0 until items.length()) {
                val s = items.getJSONObject(i).getJSONObject("snippet")
                val id = s.optJSONObject("resourceId")?.optString("videoId").orEmpty()
                val title = s.optString("title")
                if (id.isEmpty() || title == "Deleted video" || title == "Private video") continue
                out += Video.plain(
                    id = id,
                    title = title,
                    channel = s.optString("videoOwnerChannelTitle"),
                    summary = s.optString("description").take(600),
                    added = s.optString("publishedAt").take(7),
                )
            }
            page = json.optString("nextPageToken").ifEmpty { null }
        } while (page != null)
        return out
    }

    /** The account's Liked videos, newest first (what YouTube calls playlist LL). */
    suspend fun likedVideos(): List<Video> {
        val out = ArrayList<Video>()
        var page: String? = null
        do {
            val json = get("videos", mapOf("part" to "snippet", "myRating" to "like", "maxResults" to "50"), page)
            val items = json.optJSONArray("items") ?: break
            for (i in 0 until items.length()) {
                val v = items.getJSONObject(i)
                val s = v.getJSONObject("snippet")
                out += Video.plain(
                    id = v.getString("id"),
                    title = s.optString("title"),
                    channel = s.optString("channelTitle"),
                    summary = s.optString("description").take(600),
                    added = s.optString("publishedAt").take(7),
                )
            }
            page = json.optString("nextPageToken").ifEmpty { null }
            if (out.size >= 500) break
        } while (page != null)
        return out
    }

    private fun thumb(t: JSONObject?): String? =
        t?.optJSONObject("medium")?.optString("url") ?: t?.optJSONObject("default")?.optString("url")

    private suspend fun get(resource: String, params: Map<String, String>, pageToken: String?): JSONObject =
        withContext(Dispatchers.IO) {
            val token = auth.token() ?: throw IOException("Not signed in")
            val query = (params + (pageToken?.let { mapOf("pageToken" to it) } ?: emptyMap()))
                .entries.joinToString("&") { (k, v) -> k + "=" + URLEncoder.encode(v, "UTF-8") }
            val c = (URL("https://www.googleapis.com/youtube/v3/$resource?$query").openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("Authorization", "Bearer $token")
            }
            val code = c.responseCode
            val text = (if (code < 400) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            c.disconnect()
            if (code >= 400) {
                val reason = runCatching { JSONObject(text).getJSONObject("error").optString("message") }.getOrNull()
                throw IOException(reason ?: "YouTube replied $code")
            }
            JSONObject(text)
        }
}
