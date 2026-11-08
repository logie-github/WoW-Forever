// SPDX-License-Identifier: GPL-3.0-or-later
// The touch controls over Winlator's game screen (patches/winlator/0004), in place of its touchpad.

package com.logie.wowforever

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.SystemClock
import android.text.InputType
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.logie.wowforever.input.Button
import com.logie.wowforever.input.GestureOutput
import com.logie.wowforever.input.Key
import com.logie.wowforever.input.PointerGestures
import com.logie.wowforever.input.ScreenZones
import com.logie.wowforever.input.TouchGestures
import com.winlator.math.XForm
import com.winlator.renderer.ViewTransformation
import com.winlator.winhandler.MouseEventFlags
import com.winlator.xserver.Pointer
import com.winlator.xserver.Window
import com.winlator.xserver.XKeycode
import com.winlator.xserver.XServer

/**
 * Touch over the game screen. Hard rule, Blizzard's End User License Agreement first: every touch is one input,
 * as a mouse or keyboard would give it (one click, one key, one held key or button). Nothing is chained,
 * repeated, timed or sent on the player's behalf, and nothing reads or changes the game's memory or files: the
 * view only sends the X server pointer and key events, which Wine passes to the program as a mouse and keyboard.
 *
 * Two layouts, chosen by the window on top:
 * - **World of Warcraft** ([TouchGestures]): left half swipe and hold walks (one movement key at a time), right
 *   half drag turns the camera (right button held), tap clicks, double tap's second tap is one right click on what
 *   the first found, one more action-bar press or one jump, hold right-clicks, two-finger pinch zooms, two-finger
 *   tap is Tab, two-finger swipe up opens the chat.
 * - **Battle.net and everything else** ([PointerGestures]): tap clicks, hold right-clicks, drag drags with the
 *   left button, two fingers up and down scroll.
 *
 * Both: three-finger tap = the keyboard, four-finger tap or Back twice = the menu, Back = Escape.
 */
@SuppressLint("ViewConstructor")
class GestureView(
    private val activity: AppCompatActivity,
    private val xServer: XServer,
    private val onLeave: Runnable,
) : View(activity), GestureOutput {
    private val density = resources.displayMetrics.density
    private val game = TouchGestures(this, density)
    private val pointer = PointerGestures(this, density)
    private var inGame = false

    /** From the view's pixels to the game screen's. */
    private val xform = XForm.getInstance()
    private var scale = 1f
    private var restX = 0f
    private var restY = 0f
    private val hold = Runnable {
        val now = SystemClock.uptimeMillis()
        if (inGame) game.onHold(now) else pointer.onHold(now)
    }

    private val cursors = com.logie.wowforever.input.CursorKinds()
    private val fingerprints = java.util.WeakHashMap<com.winlator.xserver.Cursor, Long>()
    private var pointerMoves = 0
    private var lastTap: TapResult? = null
    private var lastBackAt = 0L

    private val windowCheck = object : Runnable {
        override fun run() {
            val now = isWowOnTop()
            if (now != inGame) {
                // Lets go of everything the other layout held before switching.
                if (inGame) game.cancel() else pointer.cancel()
                inGame = now
            }
            postDelayed(this, WINDOW_CHECK_MS)
        }
    }

    init {
        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        isClickable = true
        isFocusable = false
        // The game's screen never changes size for the keyboard: text goes through the prompt instead.
        activity.window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING or WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN,
        )
        post(windowCheck)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return
        val view = ViewTransformation()
        view.update(w, h, xServer.screenInfo.width.toInt(), xServer.screenInfo.height.toInt())
        scale = 1f / view.aspect
        if (xServer.renderer?.isFullscreen != true) {
            XForm.makeTranslation(xform, -view.viewOffsetX.toFloat(), -view.viewOffsetY.toFloat())
            XForm.scale(xform, scale, scale)
        } else {
            XForm.makeScale(xform, scale, scale)
        }
        game.setSize(w.toFloat(), h.toFloat())
        pointer.setSize(w.toFloat(), h.toFloat())
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // A mouse or trackpad goes to the game as it is (Winlator's own touchpad, under this view).
        if (event.isFromSource(InputDevice.SOURCE_MOUSE)) return false
        val index = event.actionIndex
        val id = event.getPointerId(index)
        val now = event.eventTime
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (inGame) game.down(id, event.getX(index), event.getY(index), now) else pointer.down(id, event.getX(index), event.getY(index), now)
                postDelayed(hold, TouchGestures.HOLD_MS)
            }
            MotionEvent.ACTION_MOVE -> for (i in 0 until event.pointerCount) {
                if (inGame) game.move(event.getPointerId(i), event.getX(i), event.getY(i)) else pointer.move(event.getPointerId(i), event.getX(i), event.getY(i))
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (inGame) game.up(id, event.getX(index), event.getY(index), now) else pointer.up(id, event.getX(index), event.getY(index), now)
                if (event.actionMasked == MotionEvent.ACTION_UP) removeCallbacks(hold)
            }
            MotionEvent.ACTION_CANCEL -> {
                removeCallbacks(hold)
                game.cancel()
                pointer.cancel()
            }
        }
        return true
    }

    /** The game's first window is up (Winlator's loading screen closed). */
    fun onGameWindowShown() {
        // The game's own cursor is drawn: a finger is the pointer, so it stays visible for Battle.net's screens.
        for (delay in ARROW_SAMPLES_MS) postDelayed({ if (inGame) cursors.learnArrow(cursorHere()) }, delay)
    }

    /** Back: the game's Escape; twice quickly, the app's menu. */
    fun backPressed() {
        val now = SystemClock.uptimeMillis()
        if (now - lastBackAt <= BACK_TWICE_MS) {
            lastBackAt = 0L
            showMenu()
            return
        }
        lastBackAt = now
        tap(Key.ESCAPE)
    }

    fun escape() = tap(Key.ESCAPE)

    /** Whether World of Warcraft's window is the one on top (its title), not Battle.net's or another. */
    private fun isWowOnTop(): Boolean = runCatching {
        xServer.lock(XServer.Lockable.WINDOW_MANAGER).use {
            val focused: Window? = xServer.windowManager.focusedWindow
            var window = focused
            while (window != null) {
                if (WindowNames.isWow(window.name, window.className)) return@use true
                window = window.parent
            }
            false
        }
    }.getOrDefault(false)

    // Output ------------------------------------------------------------------------------------

    override fun pointerTo(x: Float, y: Float) {
        pointerMoves++
        val point = XForm.transformPoint(xform, x, y)
        xServer.injectPointerMove(point[0].toInt(), point[1].toInt())
    }

    /** One left click there, once the game has drawn its cursor for that spot: nothing more. */
    override fun tapAt(x: Float, y: Float) {
        val tap = TapResult(x, y)
        lastTap = tap
        pointerTo(x, y)
        val moves = pointerMoves
        postDelayed({
            if (moves != pointerMoves) return@postDelayed tap.done(false)
            // What the cursor offered before the click (talk, attack, loot), for the second tap to use.
            val special = cursors.isSpecial(cursorHere())
            click(Button.LEFT)
            tap.done(special)
        }, CURSOR_SETTLE_MS)
    }

    /** The second tap: one right click on what the first found, one more action-bar press, or one jump. */
    override fun doubleTapAt(x: Float, y: Float) {
        if (ScreenZones.isActionBars(x, y, width.toFloat(), height.toFloat())) return tapAt(x, y)
        val first = lastTap ?: return jump()
        val far = Math.hypot((first.x - x).toDouble(), (first.y - y).toDouble()) > AGAIN_SLOP_DP * density
        first.whenDone { special ->
            if (special && !far) {
                pointerTo(x, y)
                val moves = pointerMoves
                postDelayed({ if (moves == pointerMoves) click(Button.RIGHT) }, CURSOR_SETTLE_MS)
            } else {
                jump()
            }
        }
    }

    override fun doubleTapNow(x: Float, y: Float): Boolean {
        val first = lastTap ?: return false
        if (first.result != false) return false
        if (ScreenZones.isActionBars(x, y, width.toFloat(), height.toFloat())) return false
        jump()
        return true
    }

    private fun jump() = tap(Key.SPACE)

    private class TapResult(val x: Float, val y: Float) {
        var result: Boolean? = null
            private set
        private var waiting: ((Boolean) -> Unit)? = null

        fun done(special: Boolean) {
            result = special
            waiting?.invoke(special)
            waiting = null
        }

        fun whenDone(then: (Boolean) -> Unit) {
            val known = result
            if (known != null) then(known) else waiting = then
        }
    }

    private fun cursorHere(): Long? = runCatching {
        xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.DRAWABLE_MANAGER, XServer.Lockable.INPUT_DEVICE).use {
            var window = xServer.inputDeviceManager.pointWindow
            var cursor: com.winlator.xserver.Cursor? = null
            while (window != null && cursor == null) {
                cursor = window.attributes.cursor
                window = window.parent
            }
            cursor?.let { fingerprints.getOrPut(it) { fingerprint(it) } }
        }
    }.getOrNull()

    private fun fingerprint(cursor: com.winlator.xserver.Cursor): Long {
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

    /** Enter opens the game's chat box; the prompt then takes the line the player types. */
    override fun openChat() {
        tap(Key.ENTER)
        postDelayed({ showKeyboard() }, KEY_MS)
    }

    override fun targetNearestEnemy() = tap(Key.TAB)

    /** The prompt: what the player types is typed into the window on top, a key per character. */
    override fun showKeyboard() {
        val field = EditText(activity).apply {
            hint = activity.getString(R.string.wf_prompt_hint)
            setSingleLine()
        }
        val hidden = CheckBox(activity).apply {
            text = activity.getString(R.string.wf_prompt_hide)
            setOnCheckedChangeListener { _, on ->
                field.inputType = InputType.TYPE_CLASS_TEXT or (if (on) InputType.TYPE_TEXT_VARIATION_PASSWORD else InputType.TYPE_TEXT_VARIATION_NORMAL)
                field.setSelection(field.text.length)
            }
        }
        val box = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (20 * density).toInt()
            setPadding(pad, pad / 2, pad, 0)
            addView(field)
            addView(hidden)
        }
        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.wf_prompt_title)
            .setView(box)
            .setPositiveButton(R.string.wf_prompt_enter) { _, _ -> type(field.text.toString(), enter = true) }
            .setNeutralButton(R.string.wf_prompt_type) { _, _ -> type(field.text.toString(), enter = false) }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        dialog.show()
        field.requestFocus()
    }

    override fun showMenu() {
        game.cancel()
        pointer.cancel()
        val items = arrayOf(
            activity.getString(R.string.wf_menu_keyboard),
            activity.getString(R.string.wf_menu_backspace),
            activity.getString(R.string.wf_menu_escape),
            activity.getString(R.string.wf_menu_leave),
        )
        AlertDialog.Builder(activity)
            .setTitle(R.string.app_name)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> showKeyboard()
                    1 -> typeKey(KeyEvent.KEYCODE_DEL)
                    2 -> tap(Key.ESCAPE)
                    3 -> onLeave.run()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // Text ---------------------------------------------------------------------------------------

    private val characters: KeyCharacterMap by lazy { KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD) }
    private val typing = ArrayDeque<() -> Unit>()
    private var typingRuns = false
    private val typeNext = object : Runnable {
        override fun run() {
            val next = typing.removeFirstOrNull()
            if (next == null) {
                typingRuns = false
                return
            }
            next()
            postDelayed(this, TYPE_STEP_MS)
        }
    }

    /** The characters the player typed, one key each, in order; then Enter when they chose it. */
    private fun type(text: String, enter: Boolean) {
        for (c in text) {
            typing.addLast {
                val events = characters.getEvents(charArrayOf(c))
                if (events != null) {
                    for (event in events) xServer.keyboard.onKeyEvent(event)
                } else {
                    xServer.keyboard.onKeyEvent(KeyEvent(SystemClock.uptimeMillis(), c.toString(), KeyCharacterMap.VIRTUAL_KEYBOARD, 0))
                }
            }
        }
        if (enter) typing.addLast { key(KeyEvent.KEYCODE_ENTER) }
        startTyping()
    }

    private fun typeKey(code: Int) {
        typing.addLast { key(code) }
        startTyping()
    }

    private fun startTyping() {
        if (!typingRuns) {
            typingRuns = true
            post(typeNext)
        }
    }

    private fun key(code: Int) {
        val time = SystemClock.uptimeMillis()
        xServer.keyboard.onKeyEvent(KeyEvent(time, time, KeyEvent.ACTION_DOWN, code, 0))
        xServer.keyboard.onKeyEvent(KeyEvent(time, time, KeyEvent.ACTION_UP, code, 0))
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(hold)
        removeCallbacks(windowCheck)
        removeCallbacks(typeNext)
        game.cancel()
        pointer.cancel()
        super.onDetachedFromWindow()
    }

    private companion object {
        const val CLICK_MS = 30L
        const val KEY_MS = 60L
        const val TYPE_STEP_MS = 15L
        const val CURSOR_SETTLE_MS = 90L
        const val AGAIN_SLOP_DP = 40f
        const val CAMERA_SPEED = 1.5f
        const val WINDOW_CHECK_MS = 1000L
        const val BACK_TWICE_MS = 600L
        val ARROW_SAMPLES_MS = longArrayOf(4000L, 8000L, 12000L)

        val Button.x: Pointer.Button
            get() = when (this) {
                Button.LEFT -> Pointer.Button.BUTTON_LEFT
                Button.RIGHT -> Pointer.Button.BUTTON_RIGHT
                Button.WHEEL_UP -> Pointer.Button.BUTTON_SCROLL_UP
                Button.WHEEL_DOWN -> Pointer.Button.BUTTON_SCROLL_DOWN
            }

        val Key.x: XKeycode
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
