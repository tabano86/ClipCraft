package com.clipcraft

import com.clipcraft.services.ClipCraftMacroManager
import org.junit.Assert.assertEquals
import org.junit.Test

class ClipCraftMacroManagerTest {

    @Test
    fun testApplyMacros() {
        val text = "Hello \${FIRST}, meet \${SECOND}"
        val macros = mapOf("FIRST" to "Alice", "SECOND" to "Bob")
        val result = ClipCraftMacroManager.applyMacros(text, macros)
        assertEquals("Hello Alice, meet Bob", result)
    }

    @Test
    fun testNoMacros() {
        val text = "Nothing to replace"
        val result = ClipCraftMacroManager.applyMacros(text, emptyMap())
        assertEquals(text, result)
    }
}
