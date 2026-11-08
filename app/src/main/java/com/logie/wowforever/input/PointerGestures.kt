// SPDX-License-Identifier: GPL-3.0-or-later

package com.logie.wowforever.input

import kotlin.math.abs
import kotlin.math.hypot

/**
 * Touch as a mouse, for Battle.net's screens and anything that isn't the game: one input per touch.
 *
 * - **Tap:** one left click where the finger is.
 * - **Hold, lift:** one right click there.
 * - **Drag:** the left button down where the finger came down, moved with it, up where it lifts (one drag).
 * - **Two fingers up or down:** the mouse wheel, a step for each [SCROLL_STEP_DP] the fingers travel (scrolling a
 *   list as a wheel does, following the fingers).
 * - **Three-finger tap:** the keyboard. **Four-finger tap:** the menu.
 */
class PointerGestures(private val out: GestureOutput, density: Float) {
    companion object {
        const val HOLD_MS = TouchGestures.HOLD_MS
        const val MULTI_TAP_MS = TouchGestures.MULTI_TAP_MS
        private const val TAP_SLOP_DP = 12f
        private const val SCROLL_STEP_DP = 32f
    }

    private enum class Role { NEW, HELD, DRAG, MULTI, DONE }

    private class Finger(val id: Int, val startX: Float, val startY: Float, val downAt: Long) {
        var x = startX
        var y = startY
        var role = Role.NEW
        fun travel() = hypot(x - startX, y - startY)
    }

    private val tapSlop = TAP_SLOP_DP * density
    private val scrollStep = SCROLL_STEP_DP * density
    private val fingers = LinkedHashMap<Int, Finger>()
    private var multi = 0
    private var multiAllTaps = true
    private var scrollBase = 0f
    private var scrolling = false

    fun setSize(@Suppress("UNUSED_PARAMETER") width: Float, @Suppress("UNUSED_PARAMETER") height: Float) {}

    fun down(id: Int, x: Float, y: Float, now: Long) {
        val finger = Finger(id, x, y, now)
        if (fingers.isNotEmpty() || multi > 0) {
            // A second finger: every finger down is a multi-finger gesture from now on (a drag in progress ends).
            for (other in fingers.values) {
                if (other.role == Role.DRAG) out.release(Button.LEFT)
                other.role = Role.MULTI
            }
            finger.role = Role.MULTI
            multi = maxOf(multi, fingers.size) + 1
            scrollBase = averageY() ?: y
            scrolling = false
        }
        fingers[id] = finger
    }

    fun move(id: Int, x: Float, y: Float) {
        val finger = fingers[id] ?: return
        finger.x = x
        finger.y = y
        when (finger.role) {
            Role.NEW, Role.HELD -> if (finger.travel() > tapSlop) {
                finger.role = Role.DRAG
                out.pointerTo(finger.startX, finger.startY)
                out.press(Button.LEFT)
                out.pointerTo(x, y)
            }
            Role.DRAG -> out.pointerTo(x, y)
            Role.MULTI -> {
                if (finger.travel() > tapSlop) multiAllTaps = false
                if (multi == 2) scroll()
            }
            Role.DONE -> {}
        }
    }

    fun up(id: Int, x: Float, y: Float, now: Long) {
        val finger = fingers[id] ?: return
        move(id, x, y)
        fingers.remove(id)
        when (finger.role) {
            Role.NEW -> if (now - finger.downAt <= HOLD_MS) {
                out.pointerTo(finger.x, finger.y)
                out.click(Button.LEFT)
            }
            Role.HELD -> {
                out.pointerTo(finger.startX, finger.startY)
                out.click(Button.RIGHT)
            }
            Role.DRAG -> out.release(Button.LEFT)
            Role.MULTI -> {
                if (now - finger.downAt > MULTI_TAP_MS && !scrolling) multiAllTaps = false
                if (fingers.values.none { it.role == Role.MULTI }) {
                    if (multiAllTaps && !scrolling) when (multi) {
                        3 -> out.showKeyboard()
                        4 -> out.showMenu()
                    }
                    multi = 0
                    multiAllTaps = true
                    scrolling = false
                }
            }
            Role.DONE -> {}
        }
    }

    /** [HOLD_MS] after a finger came down: a still finger becomes a right click when it lifts. */
    fun onHold(now: Long) {
        for (finger in fingers.values) {
            if (finger.role == Role.NEW && finger.travel() <= tapSlop && now - finger.downAt >= HOLD_MS) finger.role = Role.HELD
        }
    }

    fun cancel() {
        if (fingers.values.any { it.role == Role.DRAG }) out.release(Button.LEFT)
        fingers.clear()
        multi = 0
        multiAllTaps = true
        scrolling = false
    }

    private fun averageY(): Float? = fingers.values.takeIf { it.isNotEmpty() }?.map { it.y }?.average()?.toFloat()

    /** Two fingers moving together up or down: a wheel step for each [scrollStep] they travel. */
    private fun scroll() {
        val y = averageY() ?: return
        val first = fingers.values.firstOrNull() ?: return
        out.pointerTo(first.x, first.y)
        while (abs(y - scrollBase) >= scrollStep) {
            scrolling = true
            multiAllTaps = false
            // Fingers up: the page goes up, as on a phone (the wheel turned down shows what is further down).
            if (y < scrollBase) {
                out.click(Button.WHEEL_DOWN)
                scrollBase -= scrollStep
            } else {
                out.click(Button.WHEEL_UP)
                scrollBase += scrollStep
            }
        }
    }
}
