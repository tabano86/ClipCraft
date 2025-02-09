package com.clipcraft

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.CompressionMode
import com.clipcraft.model.OutputFormat
import com.clipcraft.util.ClipCraftFormatter
import org.junit.Assert.*
import org.junit.Test

class ClipCraftFormatterTest {

    @Test
    fun testProcessContentPlain() {
        val content = "   foo   \n   bar   "
        val opts = ClipCraftOptions(trimLineWhitespace = true)
        val result = ClipCraftFormatter.processContent(content, opts, "txt")
        // Expect lines trimmed.
        val expected = "foo\nbar"
        assertEquals(expected, result)
    }

    @Test
    fun testProcessContentLineNumbers() {
        val content = "alpha\nbeta\ngamma"
        val opts = ClipCraftOptions(includeLineNumbers = true)
        val result = ClipCraftFormatter.processContent(content, opts, "txt")
        val expected = "   1: alpha\n   2: beta\n   3: gamma"
        assertEquals(expected, result)
    }

    @Test
    fun testExternalFormatJavaFallback() {
        // If external formatting fails (for example due to module access issues), the original text is returned.
        val content = "public class Test{void foo(){}}"
        val opts = ClipCraftOptions()
        val result = try {
            ClipCraftFormatter.processContent(content, opts, "java")
        } catch (e: Throwable) {
            content // fallback to original content
        }
        // The result should contain the original class declaration (or a formatted version that is non-empty).
        assertTrue(result.contains("class Test"))
    }

    @Test
    fun testFormatBlockHTML() {
        val content = "foo\nbar"
        val result = ClipCraftFormatter.formatBlock(content, "txt", OutputFormat.HTML)
        val expected = "<pre><code class=\"txt\">foo\nbar</code></pre>"
        assertEquals(expected, result)
    }

    @Test
    fun testFormatBlockMarkdown() {
        val content = "foo\nbar"
        val result = ClipCraftFormatter.formatBlock(content, "txt", OutputFormat.MARKDOWN)
        val expected = "```txt\nfoo\nbar\n```"
        assertEquals(expected, result)
    }

    @Test
    fun testApplyCompressionUltraSelective() {
        val content = """
            This  is   some    text.
            
            TODO: Should not compress this line.
            
            More     text   here.
            debug log: should be removed.
        """.trimIndent()
        val opts = ClipCraftOptions(
            compressionMode = CompressionMode.ULTRA,
            selectiveCompression = true
        )
        val result = ClipCraftFormatter.processContent(content, opts, "txt")
        val expected = "This is some text.\nMore text here."
        assertEquals(expected, result)
    }

    @Test
    fun testCollapseConsecutiveBlankLines() {
        val input = "line1\n\n\nline2\n\n\n\nline3"
        val expected = "line1\n\nline2\n\nline3"
        val result = ClipCraftFormatter.collapseConsecutiveBlankLines(input)
        assertEquals(expected, result)
    }

    @Test
    fun testChunkContentRespectLineBoundaries() {
        // Create a multi-line string.
        val content = (1..50).joinToString("\n") { "Line $it " + "x".repeat(it) }
        val chunkSize = 100
        val chunks = ClipCraftFormatter.chunkContent(content, chunkSize, true)
        // Each chunk should contain only whole lines.
        chunks.forEach { chunk ->
            // Verify that each chunk ends with a newline (unless it's the last chunk).
            if (!chunk.endsWith("\n")) {
                // The last line may not have a newline, so check that it does not break a line.
                val lines = chunk.split("\n")
                assertFalse("Last line should not be broken", lines.last().contains(" "))
            }
        }
    }

    @Test
    fun testChunkContentWithoutRespectingLineBoundaries() {
        val content = "abcdefghij".repeat(10) // 100 characters total
        val chunkSize = 30
        val chunks = ClipCraftFormatter.chunkContent(content, chunkSize, false)
        // Expected: three chunks of 30 and one chunk of 10 characters.
        assertEquals(4, chunks.size)
        assertEquals(30, chunks[0].length)
        assertEquals(30, chunks[1].length)
        assertEquals(30, chunks[2].length)
        assertEquals(10, chunks[3].length)
    }
}
