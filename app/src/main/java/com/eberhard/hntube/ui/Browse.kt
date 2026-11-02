package com.eberhard.hntube.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eberhard.hntube.data.Catalog
import com.eberhard.hntube.data.Marks
import com.eberhard.hntube.data.Video

private val tabs = listOf("Popular", "Recent", "Topics", "Later", "Search")

@Composable
fun BrowseScreen(catalog: Catalog, marks: Marks, open: (Screen) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    val later by marks.later.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("HN", color = Orange, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text("Tube", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(28.dp))
            tabs.forEachIndexed { i, name ->
                Chip(name, selected = tab == i) { tab = i }
                Spacer(Modifier.width(8.dp))
            }
        }
        when (tab) {
            0 -> VideoGrid(catalog.popular, marks, open)
            1 -> VideoGrid(catalog.recent, marks, open)
            2 -> TopicsList(catalog, open)
            3 -> {
                val videos = later.mapNotNull { catalog[it] }
                if (videos.isEmpty()) Hint("Nothing saved yet. Open a video and press “Save for later”.")
                else VideoGrid(videos, marks, open)
            }
            4 -> SearchTab(catalog, marks, query, { query = it }, open)
        }
    }
}

@Composable
fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val (interaction, focused) = rememberFocus()
    val shape = RoundedCornerShape(20.dp)
    Text(
        label,
        color = if (selected || focused.value) Color.White else TextDim,
        fontSize = 17.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        modifier = Modifier
            .focusFrame(interaction, shape, fill = true)
            .let { if (selected) it.background(Orange.copy(alpha = 0.25f), shape) else it }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
fun Hint(text: String) {
    Text(text, color = TextDim, fontSize = 17.sp, modifier = Modifier.padding(24.dp))
}

/** The thumbnail grid: as many 300 dp columns as fit (4 on a TV, 1–2 on a phone). */
@Composable
fun VideoGrid(videos: List<Video>, marks: Marks, open: (Screen) -> Unit, header: (@Composable () -> Unit)? = null) {
    val watched by marks.watched.collectAsStateWithLifecycle()
    LazyVerticalGrid(
        columns = GridCells.Adaptive(300.dp),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (header != null) item(span = { GridItemSpan(maxLineSpan) }) { header() }
        items(videos, key = { it.id }) { v ->
            VideoCard(v, watched = v.id in watched) { open(Screen.Detail(v)) }
        }
    }
}

@Composable
private fun TopicsList(catalog: Catalog, open: (Screen) -> Unit) {
    val state = rememberLazyListState()
    LazyColumn(state = state, contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp), modifier = Modifier.fillMaxSize()) {
        catalog.areas.forEach { area ->
            item(key = "area-" + area.id) {
                Text(area.name, color = Orange, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
            }
            items(area.categories, key = { it.id }) { cat ->
                val (interaction, _) = rememberFocus()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .focusFrame(interaction)
                        .clickable(interactionSource = interaction, indication = null) { open(Screen.Topic(cat.id)) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(cat.name, fontSize = 18.sp)
                    Text("${catalog.countIn(cat.id)}", color = TextDim, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun TopicScreen(catalog: Catalog, marks: Marks, categoryId: String, open: (Screen) -> Unit) {
    val cat = catalog.categories[categoryId]
    val videos = remember(categoryId) { catalog.inCategory(categoryId) }
    VideoGrid(videos, marks, open) {
        Column(Modifier.padding(top = 20.dp, bottom = 8.dp)) {
            Text(cat?.areaName ?: "", color = Orange, fontSize = 15.sp)
            Text(cat?.name ?: categoryId, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("${videos.size} videos HN recommends", color = TextDim, fontSize = 15.sp)
        }
    }
}

@Composable
private fun SearchTab(catalog: Catalog, marks: Marks, query: String, onQuery: (String) -> Unit, open: (Screen) -> Unit) {
    val results = remember(query) { if (query.length >= 2) catalog.search(query).take(200) else emptyList() }
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            singleLine = true,
            placeholder = { Text("Title, channel, topic…") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { }),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Orange, cursorColor = Orange),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        )
        Spacer(Modifier.height(8.dp))
        when {
            query.length < 2 -> Hint("Type at least two letters. Search also finds videos HN only cited.")
            results.isEmpty() -> Hint("Nothing for “$query”.")
            else -> VideoGrid(results, marks, open)
        }
    }
}
