package com.clipcraft

import com.clipcraft.actions.ClipCraftAction
import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.OutputFormat
import com.clipcraft.util.ClipCraftFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class ClipCraftActionTest {

    @Test
    fun testProcessContentWithLineNumbers() {
        val content = "foo\nbar\nbaz"
        val opts = ClipCraftOptions(includeLineNumbers = true)
        val result = ClipCraftFormatter.processContent(content, opts, "txt")
        val expected = "   1: foo\n   2: bar\n   3: baz"
        assertEquals(expected, result)
    }

    @Test
    fun testProcessContentCollapseBlankLines() {
        val content = "alpha\n\n\nbeta\n\n\ngamma"
        val opts = ClipCraftOptions(collapseBlankLines = true)
        val result = ClipCraftFormatter.processContent(content, opts, "txt")
        // Only a single blank line should remain
        val expected = "alpha\n\nbeta\n\ngamma"
        assertEquals(expected, result)
    }

    @Test
    fun testProcessContentSingleLine() {
        val content = "alpha\nbeta\ngamma"
        val opts = ClipCraftOptions(singleLineOutput = true)
        val result = ClipCraftFormatter.processContent(content, opts, "txt")
        val expected = "alpha beta gamma"
        assertEquals(expected, result)
    }

    @Test
    fun testProcessContentWithRemoveComments() {
        val content = "code line\n// comment line\nmore code"
        val opts = ClipCraftOptions(removeComments = true)
        val result = ClipCraftFormatter.processContent(content, opts, "java")
        val expected = "code line\nmore code"
        assertEquals(expected, result)
    }

    @Test
    fun testMacroReplacement() {
        val content = "Hello, \${NAME}!"
        val opts = ClipCraftOptions(macros = mapOf("NAME" to "World"))
        val result = ClipCraftFormatter.processContent(content, opts, "txt")
        assertEquals("Hello, World!", result.trim())
    }

    @Test
    fun testChunking() {
        val bigString = "1234567890".repeat(100) // 1000 chars
        val chunks = ClipCraftFormatter.chunkContent(bigString, 300, true)
        // Expect 4 chunks: 300,300,300,100
        assertEquals(4, chunks.size)
        assertEquals(300, chunks[0].length)
        assertEquals(300, chunks[1].length)
        assertEquals(300, chunks[2].length)
        assertEquals(100, chunks[3].length)
    }

    @Test
    fun testOutputFormatHTML() {
        val content = "foo\nbar"
        val opts = ClipCraftOptions(outputFormat = OutputFormat.HTML)
        val result = ClipCraftFormatter.processContent(content, opts, "txt")
        val expected = "foo\nbar"
        assertEquals(expected, result)
    }
}
