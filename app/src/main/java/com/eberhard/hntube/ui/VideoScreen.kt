package com.eberhard.hntube.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import coil3.compose.AsyncImage
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
        Spacer(Modifier.height(6.dp))
        Text(v.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 19.sp)
        Text(v.channel + " · " + v.hnLine, color = TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Tag(text: String, modifier: Modifier = Modifier) {
    Text(
        text, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        modifier = modifier.background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** A pill button. The primary one is white with black text so it reads on the green display. */
@Composable
fun ActionButton(label: String, primary: Boolean = false, focusRequester: FocusRequester? = null, onClick: () -> Unit) {
    val (interaction, focused) = rememberFocus()
    val shape = RoundedCornerShape(24.dp)
    val bg = when {
        primary -> Color.White
        focused.value -> SurfaceFocused
        else -> Surface
    }
    Text(
        label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = if (primary) Color.Black else Color.White,
        modifier = Modifier
            .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
            .background(bg, shape)
            .focusFrame(interaction, shape, fill = false)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = Heading, fontSize = 14.sp, fontWeight = FontWeight.Bold)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoScreen(catalog: Catalog, marks: Marks, v: Video, open: (Screen) -> Unit) {
    val context = LocalContext.current
    val watched by marks.watched.collectAsStateWithLifecycle()
    val later by marks.later.collectAsStateWithLifecycle()
    val related = remember(v.id) { catalog.related(v) }
    val playFocus = remember { FocusRequester() }
    val playerName = remember { Player.playerName(context) }
    val compact = isCompact()
    // Focus lands on Play straight away: one tap on the touchpad or remote starts the video.
    LaunchedEffect(v.id) { runCatching { playFocus.requestFocus() } }

    val facts: @Composable () -> Unit = {
        Text(v.title, fontSize = if (compact) 20.sp else 26.sp, fontWeight = FontWeight.Bold, lineHeight = if (compact) 25.sp else 32.sp)
        Spacer(Modifier.height(4.dp))
        Text(v.channel, color = Heading, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text(
            listOf(v.kind, v.level, v.topic).filter { it.isNotBlank() && it != "general" && it != "other" }.joinToString(" · ") + "\n" + v.hnLine,
            color = TextDim, fontSize = 14.sp, lineHeight = 20.sp,
        )
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    val thumb: @Composable (Modifier) -> Unit = { m ->
        AsyncImage(
            model = v.thumbnail, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = m.aspectRatio(16f / 9f).clip(RoundedCornerShape(10.dp)).background(Color.Black),
        )
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = gutter(), vertical = 16.dp)) {
        item {
            if (compact) {
                Column {
                    thumb(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    facts()
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    thumb(Modifier.widthIn(max = 480.dp).fillMaxWidth(0.45f))
                    Column(Modifier.weight(1f)) { facts() }
                }
            }
        }
        item {
            Spacer(Modifier.height(20.dp))
            if (v.whyHn.isNotBlank()) {
                SectionTitle("Why Hacker News recommends it")
                Text(v.whyHn, fontSize = 17.sp, lineHeight = 24.sp, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
            }
            if (v.summary.isNotBlank()) {
                SectionTitle("What it is")
                Text(v.summary, fontSize = 16.sp, lineHeight = 23.sp, color = Color(0xFFE0E0E0), modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
            }
            if (v.quote.isNotBlank()) {
                Text("“${v.quote}”", fontSize = 18.sp, fontStyle = FontStyle.Italic, lineHeight = 25.sp)
                Text("— a Hacker News comment, ${v.quoteYear}", color = TextDim, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp, bottom = 14.dp))
            }
        }
        if (v.comments.isNotEmpty()) {
            item { SectionTitle("Hacker News says"); Spacer(Modifier.height(4.dp)) }
            items(v.comments, key = { it.id }) { c ->
                Column(Modifier.fillMaxWidth().padding(vertical = 5.dp).background(Surface, RoundedCornerShape(8.dp)).padding(12.dp)) {
                    if (c.quote.isNotBlank()) Text("“${c.quote}”", fontSize = 15.sp, lineHeight = 21.sp)
                    if (c.why.isNotBlank()) Text(c.why, color = Color(0xFFD0D0D0), fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = if (c.quote.isNotBlank()) 4.dp else 0.dp))
                    Text(
                        "${c.author} · ${c.month}" + (if (c.strong) " · strong recommendation" else "") + (if (c.story.isNotBlank()) " · in “${c.story}”" else ""),
                        color = TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
        if (related.isNotEmpty()) {
            item {
                Spacer(Modifier.height(18.dp))
                SectionTitle("People who recommended this also recommended")
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(related, key = { it.id }) { r ->
                        Box(Modifier.width(if (compact) 200.dp else 280.dp)) { VideoCard(r, watched = r.id in watched) { open(Screen.Detail(r)) } }
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(24.dp))
            Text("Data: HackerTube (hackertube.org) by Andrej Karpathy, ODC-By 1.0 · ${catalog.generated}", color = TextDim, fontSize = 11.sp)
        }
    }
}
