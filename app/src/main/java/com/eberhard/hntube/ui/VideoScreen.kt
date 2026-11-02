package com.eberhard.hntube.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.eberhard.hntube.Player
import com.eberhard.hntube.data.Catalog
import com.eberhard.hntube.data.Marks
import com.eberhard.hntube.data.Video

@Composable
fun VideoCard(v: Video, watched: Boolean, onClick: () -> Unit) {
    val (interaction, _) = rememberFocus()
    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier
            .focusFrame(interaction, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(6.dp),
    ) {
        Box {
            AsyncImage(
                model = v.thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(6.dp)).background(Color.Black),
            )
            if (watched) Tag("WATCHED", Modifier.align(Alignment.TopEnd).padding(6.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(v.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp)
        Text(v.channel + " · " + v.hnLine, color = TextDim, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Tag(text: String, modifier: Modifier = Modifier) {
    Text(
        text, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        modifier = modifier.background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
fun ActionButton(label: String, primary: Boolean = false, focusRequester: FocusRequester? = null, onClick: () -> Unit) {
    val (interaction, focused) = rememberFocus()
    val shape = RoundedCornerShape(24.dp)
    val bg = when {
        primary && focused.value -> Color(0xFFFF8533)
        primary -> Orange
        focused.value -> SurfaceFocused
        else -> Surface
    }
    Text(
        label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
        modifier = Modifier
            .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
            .background(bg, shape)
            .focusFrame(interaction, shape, fill = false)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
    )
}

@Composable
fun VideoScreen(catalog: Catalog, marks: Marks, v: Video, open: (Screen) -> Unit) {
    val context = LocalContext.current
    val watched by marks.watched.collectAsStateWithLifecycle()
    val later by marks.later.collectAsStateWithLifecycle()
    val related = remember(v.id) { catalog.related(v) }
    val playFocus = remember { FocusRequester() }
    val playerName = remember { Player.playerName(context) }
    // The remote lands on Play straight away.
    LaunchedEffect(v.id) { runCatching { playFocus.requestFocus() } }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                AsyncImage(
                    model = v.thumbnail, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(0.45f).aspectRatio(16f / 9f).clip(RoundedCornerShape(10.dp)).background(Color.Black),
                )
                Column(Modifier.weight(1f)) {
                    Text(v.title, fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(v.channel, color = Orange, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        listOf(v.kind, v.level, v.topic).filter { it.isNotBlank() && it != "general" && it != "other" }.joinToString(" · ") + "\n" + v.hnLine,
                        color = TextDim, fontSize = 15.sp, lineHeight = 21.sp,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionButton("▶  Play in $playerName", primary = true, focusRequester = playFocus) {
                            marks.setWatched(v.id, true)
                            Player.play(context, v)
                        }
                        val saved = v.id in later
                        ActionButton(if (saved) "✓ Saved" else "Save for later") { marks.setLater(v.id, !saved) }
                        val seen = v.id in watched
                        ActionButton(if (seen) "Watched ✓" else "Mark watched") { marks.setWatched(v.id, !seen) }
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(24.dp))
            if (v.whyHn.isNotBlank()) {
                Text("Why Hacker News recommends it", color = Orange, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(v.whyHn, fontSize = 18.sp, lineHeight = 26.sp, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
            }
            if (v.summary.isNotBlank()) {
                Text("What it is", color = Orange, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(v.summary, fontSize = 17.sp, lineHeight = 25.sp, color = Color(0xFFE0E0E6), modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
            }
            if (v.quote.isNotBlank()) {
                Text("“${v.quote}”", fontSize = 19.sp, fontStyle = FontStyle.Italic, lineHeight = 27.sp)
                Text("— a Hacker News comment, ${v.quoteYear}", color = TextDim, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp, bottom = 16.dp))
            }
        }
        if (v.comments.isNotEmpty()) {
            item { Text("Hacker News says", color = Orange, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp)) }
            items(v.comments, key = { it.id }) { c ->
                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp).background(Surface, RoundedCornerShape(8.dp)).padding(14.dp)) {
                    if (c.quote.isNotBlank()) Text("“${c.quote}”", fontSize = 16.sp, lineHeight = 23.sp)
                    if (c.why.isNotBlank()) Text(c.why, color = Color(0xFFD0D0D8), fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = if (c.quote.isNotBlank()) 4.dp else 0.dp))
                    Text(
                        "${c.author} · ${c.month}" + (if (c.strong) " · strong recommendation" else "") + (if (c.story.isNotBlank()) " · in “${c.story}”" else ""),
                        color = TextDim, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
        if (related.isNotEmpty()) {
            item {
                Spacer(Modifier.height(20.dp))
                Text("People who recommended this also recommended", color = Orange, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(related, key = { it.id }) { r ->
                        Box(Modifier.width(280.dp)) { VideoCard(r, watched = r.id in watched) { open(Screen.Detail(r)) } }
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(28.dp))
            Text("Data: HackerTube (hackertube.org) by Andrej Karpathy, ODC-By 1.0 · ${catalog.generated}", color = TextDim, fontSize = 12.sp)
        }
    }
}
