// SPDX-License-Identifier: GPL-3.0-or-later
// Adapted from logie-github/Blizzard (wowinput/WowGestureView.kt) for WoW Forever's retail client.

package app.gamenative.wowinput

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import com.winlator.core.AppUtils
import com.winlator.math.XForm
import com.winlator.renderer.ViewTransformation
import com.winlator.winhandler.MouseEventFlags
import com.winlator.xserver.Cursor
import com.winlator.xserver.Pointer
import com.winlator.xserver.XKeycode
import com.winlator.xserver.XServer
import java.util.WeakHashMap

/**
 * The Blizzard launcher's World of Warcraft touch controls ([TouchGestures]) over the game screen, in place of
 * Winlator's touchpad. Gestures become the X server's pointer and keys; nothing is drawn over the game.
 *
 * - **Left half, swipe and hold:** walks like a stick (W/S forward and back, Q/E sideways, diagonals between).
 * - **Right half, drag:** turns the camera and the character (the right button held in the middle of the world).
 * - **Tap:** a click there; when the game's cursor offers more (talk, loot, attack, gather) a right click follows.
 *   **Double tap:** attacks a selected enemy, presses an action button again, otherwise jumps.
 *   **Hold then lift:** right click. **Hold then move:** drags with the left button.
 * - **Two fingers:** pinch zooms (the wheel), tap targets the nearest enemy (Tab), swipe up opens the chat.
 * - **Three-finger tap:** the keyboard. **Four-finger tap:** the app's menu.
 *
 * The Blizzard launcher's private-realm signals (what a tap selected, where the spellbook is, cutscenes) and its
 * 3.3.5a character-creation detection have no counterpart on Blizzard's live servers, so those parts fall back to
 * reading the game's cursor alone, as the original does before its realm has spoken.
 */
@SuppressLint("ViewConstructor")
class WowTouchView(
    context: Context,
    private val xServer: XServer,
    private val onKeyboard: () -> Unit,
    private val onMenu: () -> Unit,
) : View(context), GestureOutput {
    private val density = resources.displayMetrics.density
    private val gestures = TouchGestures(this, density)

    /** From the view's pixels to the game screen's. */
    private val xform = XForm.getInstance()
    /** The game screen's pixels per view pixel. */
    private var scale = 1f
    private var restX = 0f
    private var restY = 0f
    private val hold = Runnable { gestures.onHold(SystemClock.uptimeMillis()) }

    private val cursors = CursorKinds()
    private val fingerprints = WeakHashMap<Cursor, Long>()
    /** Counts the pointer's moves, so a tap's delayed click can tell another gesture took over. */
    private var pointerMoves = 0
    private var lastTapAt = Long.MIN_VALUE / 2
    private var lastTapX = 0f
    private var lastTapY = 0f
    private var lastTap: TapResult? = null

    init {
        isClickable = true
        isFocusable = false
        updateXform(AppUtils.getScreenWidth(), AppUtils.getScreenHeight())
        gestures.setSize(AppUtils.getScreenWidth().toFloat(), AppUtils.getScreenHeight().toFloat())
        // The login screen shows only the plain arrow: learn what the cursor looks like when it means a plain click.
        for (delay in ARROW_SAMPLES_MS) postDelayed({ cursors.learnArrow(cursorHere()) }, delay)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateXform(w, h)
        gestures.setSize(w.toFloat(), h.toFloat())
    }

    /** As Winlator's touchpad: the game screen is letterboxed in the view unless stretched to fill it. */
    private fun updateXform(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        val view = ViewTransformation()
        view.update(width, height, xServer.screenInfo.width.toInt(), xServer.screenInfo.height.toInt())
        scale = 1f / view.aspect
        if (xServer.renderer?.isFullscreen != true) {
            XForm.makeTranslation(xform, -view.viewOffsetX.toFloat(), -view.viewOffsetY.toFloat())
            XForm.scale(xform, scale, scale)
        } else {
            XForm.makeScale(xform, scale, scale)
        }
    }

    /** Whether the event is a finger on the screen (a mouse or trackpad goes to Winlator's own handling). */
    fun handles(event: MotionEvent): Boolean = !event.isFromSource(InputDevice.SOURCE_MOUSE)

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!handles(event)) return false
        val index = event.actionIndex
        val id = event.getPointerId(index)
        val now = event.eventTime
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                gestures.down(id, event.getX(index), event.getY(index), now)
                postDelayed(hold, TouchGestures.HOLD_MS)
            }
            MotionEvent.ACTION_MOVE -> for (i in 0 until event.pointerCount) {
                gestures.move(event.getPointerId(i), event.getX(i), event.getY(i))
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                gestures.up(id, event.getX(index), event.getY(index), now)
                if (event.actionMasked == MotionEvent.ACTION_UP) removeCallbacks(hold)
            }
            MotionEvent.ACTION_CANCEL -> {
                removeCallbacks(hold)
                gestures.cancel()
            }
        }
        return true
    }

    /** Lets go of every key and button (the game left the screen, or the controls were switched off). */
    fun release() {
        removeCallbacks(hold)
        gestures.cancel()
    }

    // Gestures ---------------------------------------------------------------------------------

    override fun pointerTo(x: Float, y: Float) {
        pointerMoves++
        val point = XForm.transformPoint(xform, x, y)
        xServer.injectPointerMove(point[0].toInt(), point[1].toInt())
    }

    /**
     * The pointer goes there; once the game has drawn its cursor for that spot, a left click; when the cursor
     * offered more (talk, attack, loot, gather) and still does after the click, a right click, as on a PC.
     */
    override fun tapAt(x: Float, y: Float) {
        val now = SystemClock.uptimeMillis()
        // The same spot tapped again, not quick enough for a double tap.
        val again = now - lastTapAt in TouchGestures.DOUBLE_TAP_MS..AGAIN_MS &&
            Math.hypot((x - lastTapX).toDouble(), (y - lastTapY).toDouble()) <= AGAIN_SLOP_DP * density
        lastTapAt = now
        lastTapX = x
        lastTapY = y
        val tap = TapResult(x, y)
        lastTap = tap
        pointerTo(x, y)
        val moves = pointerMoves
        postDelayed({
            if (moves != pointerMoves) return@postDelayed tap.done(Outcome.PLAIN)
            val before = cursorHere()
            val special = cursors.isSpecial(before)
            click(Button.LEFT)
            when {
                // An enemy: the click selected it; a second tap attacks.
                special && cursors.isEnemy(before) == true -> tap.done(Outcome.ENEMY)
                special -> postDelayed({ interactIfStill(before, moves, tap) }, CURSOR_SETTLE_MS)
                // Without a cursor to read: the first tap selects, the second talks (or attacks, loots).
                again && !cursors.readable -> postDelayed({
                    if (moves == pointerMoves) click(Button.RIGHT)
                    tap.done(Outcome.INTERACTED)
                }, CURSOR_SETTLE_MS)
                else -> tap.done(Outcome.PLAIN)
            }
        }, CURSOR_SETTLE_MS)
    }

    /** The right click, when the cursor that offered it is still there after the left click. */
    private fun interactIfStill(cursor: Long?, moves: Int, tap: TapResult) {
        if (moves == pointerMoves && cursors.confirms(cursor, cursorHere())) {
            click(Button.RIGHT)
            tap.done(Outcome.INTERACTED)
        } else {
            tap.done(Outcome.PLAIN)
        }
    }

    /** The second tap of a double tap: attack the enemy the first selected; on the action bars, press again; else jump. */
    override fun doubleTapAt(x: Float, y: Float) {
        if (ScreenZones.isActionBars(x, y, width.toFloat(), height.toFloat())) return tapAt(x, y)
        val first = lastTap ?: return jump()
        val far = Math.hypot((first.x - x).toDouble(), (first.y - y).toDouble()) > AGAIN_SLOP_DP * density
        first.whenDone { outcome ->
            when {
                outcome == Outcome.ENEMY && !far -> interactAt(x, y)
                outcome == Outcome.INTERACTED -> { if (far) tapAt(x, y) }
                else -> jump()
            }
        }
    }

    /** The jump as the second finger lands, when the first tap's click is done and found the open ground. */
    override fun doubleTapNow(x: Float, y: Float): Boolean {
        val first = lastTap ?: return false
        if (first.result != Outcome.PLAIN) return false
        if (ScreenZones.isActionBars(x, y, width.toFloat(), height.toFloat())) return false
        jump()
        return true
    }

    private fun jump() = tap(Key.SPACE)

    private enum class Outcome { PLAIN, ENEMY, INTERACTED }

    /** What a tap did, once it's known (its clicks wait for the game). */
    private class TapResult(val x: Float, val y: Float) {
        private var outcome: Outcome? = null
        val result: Outcome? get() = outcome
        private var waiting: ((Outcome) -> Unit)? = null

        fun done(result: Outcome) {
            outcome = result
            waiting?.invoke(result)
            waiting = null
        }

        fun whenDone(then: (Outcome) -> Unit) {
            val known = outcome
            if (known != null) then(known) else waiting = then
        }
    }

    /** Once the game knows the pointer is there, the right click. */
    private fun interactAt(x: Float, y: Float) {
        pointerTo(x, y)
        val moves = pointerMoves
        postDelayed({ if (moves == pointerMoves) click(Button.RIGHT) }, CURSOR_SETTLE_MS)
    }

    /** A fingerprint of the game's cursor where the pointer is, or null when there's none to see. */
    private fun cursorHere(): Long? = runCatching {
        xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.DRAWABLE_MANAGER, XServer.Lockable.INPUT_DEVICE).use {
            var window = xServer.inputDeviceManager.pointWindow
            var cursor: Cursor? = null
            while (window != null && cursor == null) {
                cursor = window.attributes.cursor
                window = window.parent
            }
            cursor?.let { fingerprints.getOrPut(it) { fingerprint(it) } }
        }
    }.getOrNull()

    private fun fingerprint(cursor: Cursor): Long {
        var hash = 1469598103934665603L
        fun add(value: Int) {
            hash = (hash xor value.toLong()) * 1099511628211L
        }
        add(cursor.hotSpotX)
        add(cursor.hotSpotY)
        val image = cursor.cursorImage ?: return hash
        add(image.width.toInt())
        add(image.height.toInt())
        val data = image.data?.duplicate() ?: return hash
        data.rewind()
        while (data.remaining() >= 4) add(data.int)
        return hash
    }

    override fun pointerBy(dx: Float, dy: Float) {
        // Whole pixels, keeping what's left over for the next move.
        restX += dx * scale * CAMERA_SPEED
        restY += dy * scale * CAMERA_SPEED
        val mx = restX.toInt()
        val my = restY.toInt()
        if (mx == 0 && my == 0) return
        restX -= mx
        restY -= my
        if (xServer.isRelativeMouseMovement) {
            xServer.winHandler.mouseEvent(MouseEventFlags.MOVE, mx, my, 0)
        } else {
            xServer.injectPointerMoveDelta(mx, my)
        }
    }

    override fun press(button: Button) {
        pointerMoves++
        restX = 0f
        restY = 0f
        xServer.injectPointerButtonPress(button.x)
    }

    override fun release(button: Button) = xServer.injectPointerButtonRelease(button.x)

    override fun click(button: Button) {
        xServer.injectPointerButtonPress(button.x)
        postDelayed({ xServer.injectPointerButtonRelease(button.x) }, CLICK_MS)
    }

    override fun keyDown(key: Key) = xServer.injectKeyPress(key.x)

    override fun keyUp(key: Key) = xServer.injectKeyRelease(key.x)

    override fun tap(key: Key) {
        xServer.injectKeyPress(key.x)
        postDelayed({ xServer.injectKeyRelease(key.x) }, KEY_MS)
    }

    override fun playing() {}

    /** Enter opens the game's chat box, then the phone's keyboard types into it. */
    override fun openChat() {
        tap(Key.ENTER)
        postDelayed({ onKeyboard() }, KEY_MS)
    }

    override fun showKeyboard() = onKeyboard()

    override fun targetNearestEnemy() = tap(Key.TAB)

    override fun showMenu() = onMenu()

    override fun onDetachedFromWindow() {
        release()
        super.onDetachedFromWindow()
    }

    companion object {
        /** Whether these controls replace the touchpad in game (the setup screen's switch). */
        const val PREF_ENABLED = "wow_touch_controls"

        private const val CLICK_MS = 30L
        private const val KEY_MS = 60L
        /** How long the game takes to show its cursor for where the pointer went. */
        private const val CURSOR_SETTLE_MS = 90L
        /** A second tap on the same spot within this talks to what the first one selected (without a cursor to read). */
        private const val AGAIN_MS = 1500L
        private const val AGAIN_SLOP_DP = 40f
        /** When the login screen's cursor is looked at, after the game appears. */
        private val ARROW_SAMPLES_MS = longArrayOf(4000L, 8000L, 12000L)
        /** The camera turns a little faster than the thumb moves. */
        private const val CAMERA_SPEED = 1.5f

        private val Button.x: Pointer.Button
            get() = when (this) {
                Button.LEFT -> Pointer.Button.BUTTON_LEFT
                Button.RIGHT -> Pointer.Button.BUTTON_RIGHT
                Button.WHEEL_UP -> Pointer.Button.BUTTON_SCROLL_UP
                Button.WHEEL_DOWN -> Pointer.Button.BUTTON_SCROLL_DOWN
            }

        private val Key.x: XKeycode
            get() = when (this) {
                Key.W -> XKeycode.KEY_W
                Key.S -> XKeycode.KEY_S
                Key.Q -> XKeycode.KEY_Q
                Key.E -> XKeycode.KEY_E
                Key.B -> XKeycode.KEY_B
                Key.M -> XKeycode.KEY_M
                Key.SPACE -> XKeycode.KEY_SPACE
                Key.ESCAPE -> XKeycode.KEY_ESC
                Key.ENTER -> XKeycode.KEY_ENTER
                Key.TAB -> XKeycode.KEY_TAB
            }
    }
}
