package com.eberhard.hntube.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.eberhard.hntube.data.Marks
import com.eberhard.hntube.data.Video
import com.eberhard.hntube.yt.Fetch
import com.eberhard.hntube.yt.Playlist
import com.eberhard.hntube.yt.YouTubeAuth
import com.eberhard.hntube.yt.YouTubeStore

/** The sign-in card, or null content when signed in. Shared by the Playlists and Liked tabs. */
@Composable
private fun SignInGate(yt: YouTubeStore, signedIn: @Composable () -> Unit) {
    val status by yt.auth.status.collectAsStateWithLifecycle()
    when (val s = status) {
        YouTubeAuth.Status.SignedIn -> signedIn()
        YouTubeAuth.Status.NotConfigured -> Hint(
            "This build has no Google sign-in keys. Set the YT_CLIENT_ID and YT_CLIENT_SECRET secrets on the repo and rebuild.",
        )
        is YouTubeAuth.Status.Code -> Column(Modifier.padding(gutter())) {
            Text("On your phone, open", color = TextDim, fontSize = 16.sp)
            Text(s.url.removePrefix("https://"), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text("and enter this code:", color = TextDim, fontSize = 16.sp)
            Text(s.code, fontSize = 40.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.width(18.dp).height(18.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(Modifier.width(10.dp))
                Text("Waiting for Google…", color = TextDim, fontSize = 14.sp)
            }
            Spacer(Modifier.height(16.dp))
            ActionButton("Cancel") { yt.signOut() }
        }
        else -> Column(Modifier.padding(gutter())) {
            Text("Your YouTube playlists", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                "Sign in with Google to see your playlists, private ones included, and your Liked videos. " +
                    "HNTube only reads them. (YouTube doesn't let any app read Watch Later: save those videos to a playlist.)",
                color = TextDim, fontSize = 15.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
            )
            if (s is YouTubeAuth.Status.Failed) {
                Text(s.message, color = Color(0xFFFF8080), fontSize = 14.sp, modifier = Modifier.padding(bottom = 10.dp))
            }
            ActionButton("Sign in with Google", primary = true) { yt.startSignIn() }
        }
    }
}

@Composable
fun PlaylistsTab(yt: YouTubeStore, open: (Screen) -> Unit) {
    SignInGate(yt) {
        LaunchedEffect(Unit) { yt.loadPlaylists() }
        when (val p = yt.playlists) {
            Fetch.Idle, Fetch.Loading -> Loading()
            is Fetch.Failed -> Column(Modifier.padding(gutter())) {
                Text(p.message, color = Color(0xFFFF8080), fontSize = 15.sp)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton("Retry") { yt.loadPlaylists(force = true) }
                    ActionButton("Sign out") { yt.signOut() }
                }
            }
            is Fetch.Ready -> LazyColumn(contentPadding = PaddingValues(start = gutter(), end = gutter(), bottom = 32.dp), modifier = Modifier.fillMaxSize()) {
                if (p.value.isEmpty()) item { Hint("No playlists on this account yet.") }
                items(p.value, key = { it.id }) { pl -> PlaylistRow(pl) { open(Screen.PlaylistVideos(pl)) } }
                item {
                    Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton("Refresh") { yt.loadPlaylists(force = true) }
                        ActionButton("Sign out") { yt.signOut() }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistRow(pl: Playlist, onClick: () -> Unit) {
    val (interaction, _) = rememberFocus()
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .focusFrame(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = pl.thumbnail, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.width(120.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(6.dp)),
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Text(pl.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text("${pl.count} video${if (pl.count == 1) "" else "s"}", color = TextDim, fontSize = 13.sp)
        }
    }
}

@Composable
fun LikedTab(yt: YouTubeStore, marks: Marks, open: (Screen) -> Unit) {
    SignInGate(yt) {
        LaunchedEffect(Unit) { yt.loadLiked() }
        VideoList(yt.liked, marks, open, retry = { yt.loadLiked(force = true) }, title = "Liked videos")
    }
}

@Composable
fun PlaylistScreen(yt: YouTubeStore, marks: Marks, pl: Playlist, open: (Screen) -> Unit) {
    LaunchedEffect(pl.id) { yt.loadVideos(pl.id) }
    VideoList(yt.videosOf(pl.id), marks, open, retry = { yt.loadVideos(pl.id, force = true) }, title = pl.title)
}

@Composable
private fun VideoList(fetch: Fetch<List<Video>>, marks: Marks, open: (Screen) -> Unit, retry: () -> Unit, title: String) {
    when (fetch) {
        Fetch.Idle, Fetch.Loading -> Loading()
        is Fetch.Failed -> Column(Modifier.padding(gutter())) {
            Text(fetch.message, color = Color(0xFFFF8080), fontSize = 15.sp)
            Spacer(Modifier.height(10.dp))
            ActionButton("Retry", onClick = retry)
        }
        is Fetch.Ready -> VideoGrid(fetch.value, marks, open) {
            Box(Modifier.padding(top = 12.dp, bottom = 4.dp)) {
                Column {
                    Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("${fetch.value.size} videos · from your YouTube account", color = TextDim, fontSize = 13.sp)
                }
            }
        }
    }
}
