package com.logie.wowforever

import android.content.Context

/** The app's own settings (none of them are the game's: those stay in the game's own options). */
class Settings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("wowforever", Context.MODE_PRIVATE)

    /** Windows' screen, which Battle.net and the game draw at (scaled to the phone's screen). */
    var screenSize: String
        get() = prefs.getString("screen_size", SCREEN_SIZES.first()) ?: SCREEN_SIZES.first()
        set(value) = prefs.edit().putString("screen_size", value).apply()

    /** Winlator's Box64 preset: Performance (its default) or Stability (if Battle.net or the game crashes). */
    var box64Preset: String
        get() = prefs.getString("box64_preset", BOX64_PERFORMANCE) ?: BOX64_PERFORMANCE
        set(value) = prefs.edit().putString("box64_preset", value).apply()

    /** The code Battle.net's own `--exec="launch <code>"` option starts the game with. */
    var gameCode: String
        get() = prefs.getString("game_code", DEFAULT_GAME_CODE)?.takeIf { it.isNotBlank() } ?: DEFAULT_GAME_CODE
        set(value) = prefs.edit().putString("game_code", value.trim()).apply()

    /** Play opens Battle.net instead of starting the game through it (the toggle back to the launcher). */
    var openBattleNet: Boolean
        get() = prefs.getBoolean("open_battlenet", false)
        set(value) = prefs.edit().putBoolean("open_battlenet", value).apply()

    /** Opening the app starts Play by itself after a short countdown. */
    var autoStart: Boolean
        get() = prefs.getBoolean("auto_start", true)
        set(value) = prefs.edit().putBoolean("auto_start", value).apply()

    companion object {
        val SCREEN_SIZES = listOf("1280x720", "1600x900", "1920x1080")
        const val BOX64_PERFORMANCE = "PERFORMANCE"
        const val BOX64_STABILITY = "STABILITY"
        /** WoW Forever's product code on Blizzard's servers. */
        const val DEFAULT_GAME_CODE = "wowf"
    }
}
