package com.eberhard.hntube.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.eberhard.hntube.data.Catalog
import com.eberhard.hntube.data.Marks
import com.eberhard.hntube.data.Video

/** Where the user is. A plain back stack: Browse is always at the bottom. */
sealed interface Screen {
    data object Browse : Screen
    data class Topic(val categoryId: String) : Screen
    data class Detail(val video: Video) : Screen
}

@Composable
fun App(catalog: Catalog, marks: Marks) {
    val stack = remember { mutableStateListOf<Screen>(Screen.Browse) }
    val top = stack.last()
    BackHandler(enabled = stack.size > 1) { stack.removeAt(stack.lastIndex) }
    val open: (Screen) -> Unit = { s -> stack += s }

    Box(Modifier.fillMaxSize().background(Background).safeDrawingPadding()) {
        when (top) {
            Screen.Browse -> BrowseScreen(catalog, marks, open)
            is Screen.Topic -> TopicScreen(catalog, marks, top.categoryId, open)
            is Screen.Detail -> VideoScreen(catalog, marks, top.video, open)
        }
    }
}
