package com.eberhard.hntube

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import com.eberhard.hntube.data.Catalog
import com.eberhard.hntube.data.Marks
import com.eberhard.hntube.ui.App
import com.eberhard.hntube.ui.HNTubeTheme
import com.eberhard.hntube.ui.Loading

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val marks = Marks(applicationContext)
        setContent {
            HNTubeTheme {
                var catalog by remember { mutableStateOf(loaded) }
                LaunchedEffect(Unit) {
                    if (catalog == null) catalog = Catalog.load(applicationContext).also { loaded = it }
                }
                val c = catalog
                if (c == null) Loading() else App(c, marks)
            }
        }
    }

    private companion object {
        /** Parsed once per process; folding or rotating the screen must not reload 6,000 videos. */
        var loaded: Catalog? = null
    }
}
