package com.logie.wowforever

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.logie.wowforever.look.BlizzTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Play starts by itself only on a fresh open of the app, never when coming back from the game.
        val fresh = savedInstanceState == null && !startedOnce
        startedOnce = true
        setContent { BlizzTheme { Home(autoStartAllowed = fresh) } }
    }

    private companion object {
        /** Per process: leaving the game returns here without starting it again. */
        var startedOnce = false
    }
}
