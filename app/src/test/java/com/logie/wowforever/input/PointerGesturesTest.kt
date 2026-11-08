package com.logie.wowforever.input

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PointerGesturesTest {
    private class Log : GestureOutput {
        val events = mutableListOf<String>()
        override fun pointerTo(x: Float, y: Float) { events += "to ${x.toInt()},${y.toInt()}" }
        override fun tapAt(x: Float, y: Float) { events += "tapAt" }
        override fun targetNearestEnemy() { events += "tab" }
        override fun doubleTapAt(x: Float, y: Float) { events += "doubleTapAt" }
        override fun pointerBy(dx: Float, dy: Float) { events += "by" }
        override fun press(button: Button) { events += "press $button" }
        override fun release(button: Button) { events += "release $button" }
        override fun click(button: Button) { events += "click $button" }
        override fun keyDown(key: Key) { events += "down $key" }
        override fun keyUp(key: Key) { events += "up $key" }
        override fun tap(key: Key) { events += "tap $key" }
        override fun playing() {}
        override fun openChat() { events += "chat" }
        override fun showKeyboard() { events += "keyboard" }
        override fun showMenu() { events += "menu" }
    }

    private val log = Log()
    private val gestures = PointerGestures(log, density = 1f)

    @Test
    fun aTapIsOneLeftClick() {
        gestures.down(0, 100f, 200f, 0)
        gestures.up(0, 101f, 200f, 100)
        assertEquals(listOf("to 101,200", "click LEFT"), log.events)
    }

    @Test
    fun aHoldIsOneRightClick() {
        gestures.down(0, 100f, 200f, 0)
        gestures.onHold(PointerGestures.HOLD_MS)
        gestures.up(0, 100f, 200f, 900)
        assertEquals(listOf("to 100,200", "click RIGHT"), log.events)
    }

    @Test
    fun aDragIsOneLeftButtonDrag() {
        gestures.down(0, 100f, 200f, 0)
        gestures.move(0, 150f, 200f)
        gestures.move(0, 200f, 220f)
        gestures.up(0, 200f, 220f, 900)
        assertEquals(listOf("to 100,200", "press LEFT", "to 150,200", "to 200,220", "to 200,220", "release LEFT"), log.events)
    }

    @Test
    fun twoFingersScrollAStepPerDistance() {
        gestures.down(0, 100f, 400f, 0)
        gestures.down(1, 140f, 400f, 20)
        gestures.move(0, 100f, 330f)
        gestures.move(1, 140f, 330f)
        gestures.up(0, 100f, 330f, 500)
        gestures.up(1, 140f, 330f, 510)
        val steps = log.events.filter { it.startsWith("click") }
        assertTrue(steps.isNotEmpty() && steps.all { it == "click WHEEL_DOWN" }, log.events.toString())
        assertTrue("keyboard" !in log.events && "menu" !in log.events)
    }

    @Test
    fun threeAndFourFingerTaps() {
        for (i in 0 until 3) gestures.down(i, 100f + i * 40, 300f, i.toLong())
        for (i in 0 until 3) gestures.up(i, 100f + i * 40, 300f, 100L + i)
        assertEquals(listOf("keyboard"), log.events)
        log.events.clear()
        for (i in 0 until 4) gestures.down(i, 100f + i * 40, 300f, 1000L + i)
        for (i in 0 until 4) gestures.up(i, 100f + i * 40, 300f, 1100L + i)
        assertEquals(listOf("menu"), log.events)
    }
}
