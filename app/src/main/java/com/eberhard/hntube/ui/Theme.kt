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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

val Orange = Color(0xFFFF6600)
val Background = Color(0xFF0E0E10)
val Surface = Color(0xFF1A1A1E)
val SurfaceFocused = Color(0xFF2A2A30)
val TextDim = Color(0xFFA0A0A8)

/** Always dark, like HackerTube; TVs and glasses look best that way. */
@Composable
fun HNTubeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Orange,
            onPrimary = Color.White,
            background = Background,
            surface = Surface,
            onBackground = Color.White,
            onSurface = Color.White,
            secondary = Orange,
        ),
        content = content,
    )
}

/** A focus frame for remote-control navigation: orange outline when the D-pad lands here. */
@Composable
fun Modifier.focusFrame(
    interaction: MutableInteractionSource,
    shape: Shape = RoundedCornerShape(10.dp),
    fill: Boolean = true,
): Modifier {
    val focused by interaction.collectIsFocusedAsState()
    val base = if (fill) background(if (focused) SurfaceFocused else Surface, shape) else this
    return if (focused) base.border(BorderStroke(3.dp, Orange), shape) else base
}

@Composable
fun rememberFocus(): Pair<MutableInteractionSource, State<Boolean>> {
    val source = remember { MutableInteractionSource() }
    return source to source.collectIsFocusedAsState()
}

@Composable
fun Loading() {
    Box(Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Orange)
    }
}
