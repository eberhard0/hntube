package com.eberhard.hntube

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import com.eberhard.hntube.data.Video

/**
 * Where the video plays. SmartTube first (the whole point on the Rokid Station), then the
 * YouTube TV or phone app, and only when neither exists YouTube's embedded player in a web view.
 */
object Player {
    private val preferred = listOf(
        "com.teamsmart.videomanager.tv",     // SmartTube (stable)
        "com.liskovsoft.smarttubetv.beta",   // SmartTube (beta)
        "com.google.android.youtube.tv",     // YouTube for Android TV
        "com.google.android.youtube",        // YouTube (phone)
    )

    /** Which app will play, for the button label: "Play in SmartTube". */
    fun playerName(context: Context): String = when (installed(context)) {
        "com.teamsmart.videomanager.tv", "com.liskovsoft.smarttubetv.beta" -> "SmartTube"
        "com.google.android.youtube.tv", "com.google.android.youtube" -> "YouTube"
        null -> if (anyHandler(context)) "YouTube app" else "web player"
        else -> "YouTube"
    }

    fun play(context: Context, video: Video) {
        val url = Uri.parse(video.youtubeUrl)
        val pkg = installed(context)
        val intent = Intent(Intent.ACTION_VIEW, url).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (pkg != null) setPackage(pkg)
        }
        if (pkg != null || anyHandler(context)) {
            runCatching { context.startActivity(intent) }.onSuccess { return }
        }
        context.startActivity(Intent(context, PlayerActivity::class.java).putExtra(PlayerActivity.EXTRA_URL, video.embedUrl))
    }

    private fun installed(context: Context): String? = preferred.firstOrNull { pkg ->
        runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess
    }

    private fun anyHandler(context: Context): Boolean {
        val probe = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        return context.packageManager.queryIntentActivities(probe, 0).any { it.activityInfo.packageName != context.packageName }
    }
}

/** YouTube's embedded player, full screen. The fallback when no YouTube app is installed. */
class PlayerActivity : ComponentActivity() {
    private var web: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val url = intent.getStringExtra(EXTRA_URL) ?: run { finish(); return }
        val w = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            webViewClient = object : WebViewClient() {
                // Stay on the player page; "Watch on YouTube" and the like open outside.
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val host = request.url.host ?: return false
                    if (host.endsWith("youtube-nocookie.com")) return false
                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, request.url)) }
                    return true
                }
            }
            loadUrl(url)
        }
        web = w
        setContentView(w)
    }

    override fun onDestroy() {
        web?.destroy()
        web = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_URL = "url"
    }
}
