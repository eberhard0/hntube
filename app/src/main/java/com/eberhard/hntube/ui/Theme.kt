package com.eberhard.hntube.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

// The Rokid glasses show only the green channel, so everything that matters is drawn in
// luminance: white text and frames on black. Orange is only for the wordmark and headings.
val Orange = Color(0xFFFF6600)
val Background = Color(0xFF000000)
val Surface = Color(0xFF1C1C1C)
val SurfaceFocused = Color(0xFF3A3A3A)
val Focus = Color.White
val TextDim = Color(0xFFB4B4B4)
val Heading = Color(0xFFFFB380)

/** Always dark, like HackerTube; TVs and glasses look best that way. */
@Composable
fun HNTubeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color.White,
            onPrimary = Color.Black,
            background = Background,
            surface = Surface,
            onBackground = Color.White,
            onSurface = Color.White,
            secondary = Orange,
        ),
        content = content,
    )
}

/** True on the glasses (480 px wide) and on a folded phone: one column, stacked layouts. */
@Composable
fun isCompact(): Boolean = LocalConfiguration.current.screenWidthDp < 560

/** A focus frame for touchpad / remote navigation: white outline when focus lands here. */
@Composable
fun Modifier.focusFrame(
    interaction: MutableInteractionSource,
    shape: Shape = RoundedCornerShape(10.dp),
    fill: Boolean = true,
): Modifier {
    val focused by interaction.collectIsFocusedAsState()
    val base = if (fill) background(if (focused) SurfaceFocused else Surface, shape) else this
    return if (focused) base.border(BorderStroke(3.dp, Focus), shape) else base
}

@Composable
fun rememberFocus(): Pair<MutableInteractionSource, State<Boolean>> {
    val source = remember { MutableInteractionSource() }
    return source to source.collectIsFocusedAsState()
}

@Composable
fun Loading() {
    Box(Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color.White)
    }
}
