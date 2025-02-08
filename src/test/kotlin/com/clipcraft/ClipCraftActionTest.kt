package com.clipcraft

import com.clipcraft.actions.ClipCraftAction
import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.OutputFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class ClipCraftActionTest {

    @Test
    fun testProcessContentWithLineNumbers() {
        val action = ClipCraftAction()
        val content = "foo\nbar\nbaz"
        val opts = ClipCraftOptions(includeLineNumbers = true)
        val result = action.processContent(content, opts, "txt")
        val expected = "   1: foo\n   2: bar\n   3: baz"
        assertEquals(expected, result)
    }

    @Test
    fun testProcessContentWithoutLineNumbers() {
        val action = ClipCraftAction()
        val content = "alpha\nbeta\ngamma"
        val opts = ClipCraftOptions(includeLineNumbers = false)
        val result = action.processContent(content, opts, "txt")
        val expected = "alpha\nbeta\ngamma"
        assertEquals(expected, result)
    }

    @Test
    fun testProcessContentWithRemoveComments() {
        val action = ClipCraftAction()
        val content = "code line\n// comment line\nmore code"
        val opts = ClipCraftOptions(removeComments = true)
        val result = action.processContent(content, opts, "java")
        val expected = "code line\nmore code"
        assertEquals(expected, result)
    }

    @Test
    fun testProcessContentWithTrimLineWhitespace() {
        val action = ClipCraftAction()
        val content = "  foo  \n  bar  "
        val opts = ClipCraftOptions(trimLineWhitespace = true)
        val result = action.processContent(content, opts, "txt")
        val expected = "  foo\n  bar"
        assertEquals(expected, result)
    }

    @Test
    fun testProcessContentWithRemoveImports() {
        val action = ClipCraftAction()
        val content = "import java.util.*\npublic class Test {}"
        val opts = ClipCraftOptions(removeImports = true)
        val result = action.processContent(content, opts, "java")
        assertEquals("public class Test {}", result.trim())
    }

    @Test
    fun testMacroReplacement() {
        val action = ClipCraftAction()
        val content = "Hello, \${NAME}!"
        val opts = ClipCraftOptions(macros = mapOf("NAME" to "World"))
        val result = action.processContent(content, opts, "txt")
        assertEquals("Hello, World!", result.trim())
    }

    @Test
    fun testOutputFormatHTML() {
        val action = ClipCraftAction()
        val content = "foo\nbar"
        val opts = ClipCraftOptions(outputFormat = OutputFormat.HTML)
        val result = action.processContent(content, opts, "txt")
        // For HTML, we do not insert <html> tags, but we do preserve line breaks.
        val expected = "foo\nbar"
        assertEquals(expected, result)
    }
}
