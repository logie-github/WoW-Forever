package app.gamenative.rules

import app.gamenative.data.TouchGestureConfig
import com.winlator.inputcontrols.Binding
import com.winlator.inputcontrols.BindingCombo
import com.winlator.inputcontrols.ControlElement
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** Hard rule (Blizzard's terms of use): one button press is one function. No sequences, no multi-key presses. */
class OnePressOneFunctionTest {
    @Test
    fun `a sequence is never kept`() {
        val combo = BindingCombo.fromBindings(listOf(Binding.KEY_E, Binding.MOUSE_LEFT_BUTTON), BindingCombo.Mode.SEQUENCE, 220)
        assertFalse(combo.isSequence)
        assertEquals(listOf(Binding.MOUSE_LEFT_BUTTON), combo.bindings)
    }

    @Test
    fun `a saved sequence loads as one press`() {
        val json = JSONObject().put("bindings", JSONArray(listOf("KEY_1", "KEY_2", "KEY_3"))).put("mode", "sequence")
        val combo = BindingCombo.fromJsonValue(json)
        assertFalse(combo.isSequence)
        assertEquals(listOf(Binding.KEY_3), combo.bindings)
    }

    @Test
    fun `a combo is its modifiers and one key`() {
        assertEquals(
            listOf(Binding.KEY_SHIFT_L, Binding.KEY_CTRL_L, Binding.KEY_1),
            BindingCombo.fromBindings(listOf(Binding.KEY_1, Binding.KEY_SHIFT_L, Binding.KEY_CTRL_L)).bindings,
        )
        assertEquals(1, BindingCombo.fromBindings(listOf(Binding.KEY_1, Binding.KEY_2, Binding.KEY_3)).bindings.size)
    }

    @Test
    fun `touch gesture actions are one press`() {
        assertEquals(listOf("key_2"), TouchGestureConfig.actionParts("seq:150:key_1|key_2"))
        assertFalse(TouchGestureConfig.isActionSequence("seq:150:key_1|key_2"))
        assertEquals(listOf("key_CTRL_L", "key_2"), TouchGestureConfig.actionParts("combo:key_1|key_2|key_CTRL_L"))
        assertEquals("combo:key_SHIFT_L|key_E", TouchGestureConfig.actionComboOf(listOf("key_E", "key_SHIFT_L"), sequence = true))
    }

    @Test
    fun `a stick holds one direction at a time`() {
        val all = booleanArrayOf(true, true, false, false)
        assertArrayEquals(booleanArrayOf(true, false, false, false), ControlElement.oneDirection(all, 0.5f, -0.9f))
        assertArrayEquals(booleanArrayOf(false, true, false, false), ControlElement.oneDirection(all, 0.9f, -0.5f))
    }
}
