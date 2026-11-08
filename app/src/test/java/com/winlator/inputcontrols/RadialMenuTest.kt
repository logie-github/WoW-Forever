package com.winlator.inputcontrols

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class RadialMenuTest {
    @Test
    fun `legacy single binding still loads`() {
        val slot = RadialMenu.Slot.fromJSONObject(
            JSONObject()
                .put("label", "Inventory")
                .put("binding", "KEY_I"),
        )

        assertEquals("Inventory", slot.label)
        assertEquals(BindingCombo.of(Binding.KEY_I), slot.bindingCombo)
    }

    @Test
    fun `simultaneous radial binding round trips`() {
        val expected = BindingCombo.fromBindings(listOf(Binding.KEY_CTRL_L, Binding.KEY_1))
        val slot = RadialMenu.Slot("Action", expected)

        val json = slot.toJSONObject()
        val restored = RadialMenu.Slot.fromJSONObject(json)

        assertEquals(Binding.KEY_1.name, json.getString("binding"))
        assertEquals(expected, restored.bindingCombo)
    }

}
