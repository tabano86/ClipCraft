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
    fun testProcessContentCollapseBlankLines() {
        val action = ClipCraftAction()
        val content = "alpha\n\n\nbeta\n\n\ngamma"
        val opts = ClipCraftOptions(collapseBlankLines = true)
        val result = action.processContent(content, opts, "txt")
        // Only a single blank line should remain between alpha/beta/gamma
        val expected = "alpha\n\nbeta\n\ngamma"
        assertEquals(expected, result)
    }

    @Test
    fun testProcessContentSingleLine() {
        val action = ClipCraftAction()
        val content = "alpha\nbeta\ngamma"
        val opts = ClipCraftOptions(singleLineOutput = true)
        val result = action.processContent(content, opts, "txt")
        val expected = "alpha beta gamma"
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
    fun testMacroReplacement() {
        val action = ClipCraftAction()
        val content = "Hello, \${NAME}!"
        val opts = ClipCraftOptions(macros = mapOf("NAME" to "World"))
        val result = action.processContent(content, opts, "txt")
        assertEquals("Hello, World!", result.trim())
    }

    @Test
    fun testChunking() {
        val action = ClipCraftAction()
        val bigString = "1234567890".repeat(100) // 1000 chars
        val chunked = action.chunkContent(bigString, 300)
        // Expect 4 chunks: 300, 300, 300, 100
        assertEquals(4, chunked.size)
        assertEquals(300, chunked[0].length)
        assertEquals(300, chunked[1].length)
        assertEquals(300, chunked[2].length)
        assertEquals(100, chunked[3].length)
    }

    @Test
    fun testOutputFormatHTML() {
        val action = ClipCraftAction()
        val content = "foo\nbar"
        val opts = ClipCraftOptions(outputFormat = OutputFormat.HTML)
        val result = action.processContent(content, opts, "txt")
        val expected = "foo\nbar"
        assertEquals(expected, result)
    }
}
