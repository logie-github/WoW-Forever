package com.logie.wowforever

/** Which X window is World of Warcraft's: its title, or the class its window is made with. */
object WindowNames {
    fun isWow(name: String?, className: String?): Boolean {
        val n = name.orEmpty().lowercase()
        val c = className.orEmpty().lowercase()
        if ("battle.net" in n || "battle.net" in c) return false
        return n == "world of warcraft" || n.startsWith("world of warcraft") ||
            "wow.exe" in c || "wow-64.exe" in c || "wowclassic.exe" in c || "gxwindowclass" in c
    }
}
