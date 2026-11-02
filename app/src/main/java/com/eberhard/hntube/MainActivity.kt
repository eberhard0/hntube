package com.eberhard.hntube

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.core.content.ContextCompat
import com.eberhard.hntube.data.Catalog
import com.eberhard.hntube.data.Marks
import com.eberhard.hntube.ui.App
import com.eberhard.hntube.ui.HNTubeTheme
import com.eberhard.hntube.ui.Loading

class MainActivity : ComponentActivity() {
    private var focusManager: FocusManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val marks = Marks(applicationContext)
        setContent {
            HNTubeTheme {
                focusManager = LocalFocusManager.current
                var catalog by remember { mutableStateOf(loaded) }
                LaunchedEffect(Unit) {
                    if (catalog == null) catalog = Catalog.load(applicationContext).also { loaded = it }
                }
                val c = catalog
                if (c == null) Loading() else App(c, marks)
            }
        }
    }

    /**
     * On the Rokid glasses the touchpad's two-finger swipes arrive as ordered broadcasts, not
     * key events (a single tap is Enter, the side button's double-click is Back). Forward
     * moves focus to the next item, back to the previous one, so the whole app can be driven
     * from the temple. Harmless anywhere else: nothing sends these actions.
     */
    private val gestures = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val direction = when (intent.action) {
                ACTION_SWIPE_FORWARD -> FocusDirection.Next
                ACTION_SWIPE_BACK -> FocusDirection.Previous
                else -> return
            }
            focusManager?.moveFocus(direction)
            if (isOrderedBroadcast) abortBroadcast()
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(ACTION_SWIPE_FORWARD)
            addAction(ACTION_SWIPE_BACK)
            priority = 100
        }
        ContextCompat.registerReceiver(this, gestures, filter, ContextCompat.RECEIVER_EXPORTED)
    }

    override fun onStop() {
        runCatching { unregisterReceiver(gestures) }
        super.onStop()
    }

    private companion object {
        const val ACTION_SWIPE_FORWARD = "com.android.action.ACTION_TWO_FINGER_SWIPE_FORWARD"
        const val ACTION_SWIPE_BACK = "com.android.action.ACTION_TWO_FINGER_SWIPE_BACK"

        /** Parsed once per process; folding or rotating the screen must not reload 6,000 videos. */
        var loaded: Catalog? = null
    }
}
