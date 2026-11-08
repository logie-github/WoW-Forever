package com.logie.wowforever

import com.logie.wowforever.input.directionKeys
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Hard rule, Blizzard's EULA: one touch is one input. The stick never holds two movement keys. */
class OnePressOneInputTest {
    @Test
    fun theStickHoldsOneKeyInEveryDirection() {
        for (angle in 0 until 360) {
            val r = Math.toRadians(angle.toDouble())
            assertEquals(1, directionKeys(kotlin.math.cos(r).toFloat(), kotlin.math.sin(r).toFloat()).size, "angle $angle")
        }
    }

    @Test
    fun wowWindowIsKnownByItsTitle() {
        assertTrue(WindowNames.isWow("World of Warcraft", ""))
        assertTrue(WindowNames.isWow("", "Wow.exe"))
        assertFalse(WindowNames.isWow("Battle.net", "Battle.net.exe"))
        assertFalse(WindowNames.isWow("Battle.net Setup", ""))
    }
}
