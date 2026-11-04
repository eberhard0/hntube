package com.eberhard.hntube.yt

import android.content.Context
import com.eberhard.hntube.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Google sign-in for a screen with no keyboard (the glasses, a TV): the "limited input device"
 * flow. HNTube shows a short code, the user types it at google.com/device on their phone, and
 * HNTube polls Google until it is allowed in. Read-only YouTube access; the refresh token is
 * kept in the app's private preferences and never leaves the device except to Google.
 *
 * The OAuth client (type "TVs and Limited Input devices") is the owner's own Google Cloud
 * project, built in through BuildConfig from the CI secrets YT_CLIENT_ID / YT_CLIENT_SECRET.
 */
class YouTubeAuth(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("youtube_auth", Context.MODE_PRIVATE)

    sealed interface Status {
        data object NotConfigured : Status
        data object SignedOut : Status
        /** Show [code] and [url] to the user; we are polling meanwhile. */
        data class Code(val code: String, val url: String) : Status
        data object SignedIn : Status
        data class Failed(val message: String) : Status
    }

    private val _status = MutableStateFlow<Status>(
        when {
            !configured -> Status.NotConfigured
            refreshToken != null -> Status.SignedIn
            else -> Status.SignedOut
        },
    )
    val status: StateFlow<Status> = _status.asStateFlow()

    private var accessToken: String? = null
    private var accessExpiresAt = 0L

    private val refreshToken: String? get() = prefs.getString(KEY_REFRESH, null)

    /** Runs the whole device flow; returns when signed in, cancelled by the caller, or failed. */
    suspend fun signIn() {
        if (!configured) {
            _status.value = Status.NotConfigured
            return
        }
        try {
            val start = post(
                "https://oauth2.googleapis.com/device/code",
                mapOf("client_id" to BuildConfig.YT_CLIENT_ID, "scope" to SCOPE),
            )
            val deviceCode = start.getString("device_code")
            val interval = start.optLong("interval", 5L).coerceAtLeast(3L)
            val expiresAt = System.currentTimeMillis() + start.optLong("expires_in", 1800L) * 1000
            _status.value = Status.Code(
                start.getString("user_code"),
                start.optString("verification_url", "https://www.google.com/device"),
            )
            while (System.currentTimeMillis() < expiresAt) {
                delay(interval * 1000)
                val reply = post(
                    "https://oauth2.googleapis.com/token",
                    mapOf(
                        "client_id" to BuildConfig.YT_CLIENT_ID,
                        "client_secret" to BuildConfig.YT_CLIENT_SECRET,
                        "device_code" to deviceCode,
                        "grant_type" to "urn:ietf:params:oauth:grant-type:device_code",
                    ),
                    allowError = true,
                )
                when (reply.optString("error")) {
                    "" -> {
                        prefs.edit().putString(KEY_REFRESH, reply.getString("refresh_token")).apply()
                        accessToken = reply.getString("access_token")
                        accessExpiresAt = System.currentTimeMillis() + reply.optLong("expires_in", 3600L) * 1000 - 60_000
                        _status.value = Status.SignedIn
                        return
                    }
                    "authorization_pending" -> continue
                    "slow_down" -> delay(interval * 1000)
                    "access_denied" -> {
                        _status.value = Status.Failed("You declined on the phone. Try again when you like.")
                        return
                    }
                    else -> {
                        _status.value = Status.Failed("Google said: ${reply.optString("error_description", reply.optString("error"))}")
                        return
                    }
                }
            }
            _status.value = Status.Failed("The code expired. Sign in again for a new one.")
        } catch (e: IOException) {
            _status.value = Status.Failed("No connection to Google: ${e.message}")
        }
    }

    fun signOut() {
        prefs.edit().remove(KEY_REFRESH).apply()
        accessToken = null
        _status.value = if (configured) Status.SignedOut else Status.NotConfigured
    }

    /** A valid access token, refreshed when needed. Null when signed out. */
    suspend fun token(): String? {
        accessToken?.takeIf { System.currentTimeMillis() < accessExpiresAt }?.let { return it }
        val refresh = refreshToken ?: return null
        val reply = post(
            "https://oauth2.googleapis.com/token",
            mapOf(
                "client_id" to BuildConfig.YT_CLIENT_ID,
                "client_secret" to BuildConfig.YT_CLIENT_SECRET,
                "refresh_token" to refresh,
                "grant_type" to "refresh_token",
            ),
            allowError = true,
        )
        if (reply.has("error")) {
            // invalid_grant: the user revoked access, or a "testing" OAuth app's 7-day token ran out.
            signOut()
            _status.value = Status.Failed("Google signed you out (${reply.optString("error")}). Sign in again.")
            return null
        }
        accessToken = reply.getString("access_token")
        accessExpiresAt = System.currentTimeMillis() + reply.optLong("expires_in", 3600L) * 1000 - 60_000
        return accessToken
    }

    private suspend fun post(url: String, form: Map<String, String>, allowError: Boolean = false): JSONObject =
        withContext(Dispatchers.IO) {
            val body = form.entries.joinToString("&") { (k, v) -> k + "=" + URLEncoder.encode(v, "UTF-8") }
            val c = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 20_000
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }
            c.outputStream.use { it.write(body.toByteArray()) }
            val code = c.responseCode
            val text = (if (code < 400) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            c.disconnect()
            if (code >= 400 && !allowError) throw IOException("HTTP $code $text")
            runCatching { JSONObject(text) }.getOrElse { throw IOException("Unreadable reply from Google") }
        }

    companion object {
        private const val KEY_REFRESH = "refresh_token"
        private const val SCOPE = "https://www.googleapis.com/auth/youtube.readonly"
        val configured: Boolean get() = BuildConfig.YT_CLIENT_ID.isNotBlank() && BuildConfig.YT_CLIENT_SECRET.isNotBlank()
    }
}
