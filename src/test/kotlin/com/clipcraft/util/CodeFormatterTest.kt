package com.clipcraft.util

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.CompressionMode
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class CodeFormatterTest {

    @Test
    fun `chunkContent returns single chunk when content is shorter than maxChunkSize`() {
        val content = "Short content"
        val chunks = CodeFormatter.chunkBySize(content, 50, preserveWords = false)
        assertEquals(1, chunks.size)
        assertEquals(content, chunks[0])
    }

    @Test
    fun `chunkContent reassembles original content with preserveWords true`() {
        val content = "This is a sample text that should be split into multiple chunks without breaking words."
        val maxChunkSize = 20
        val chunks = CodeFormatter.chunkBySize(content, maxChunkSize, preserveWords = true)
        val reassembled = chunks.joinToString("") { it }
        assertEquals(content, reassembled)
    }

    @Test
    fun `each chunk is within the specified maxChunkSize`() {
        val content = "Line one. Line two is a bit longer. Line three."
        val maxChunkSize = 25
        val chunks = CodeFormatter.chunkBySize(content, maxChunkSize, preserveWords = false)
        chunks.forEach { assertTrue(it.length <= maxChunkSize, "Chunk exceeds max size: \"$it\"") }
        val reassembled = chunks.joinToString("") { it }
        assertEquals(content, reassembled)
    }

    @Test
    fun `trimWhitespace removes leading and trailing whitespace and zero-width spaces`() {
        val input = "\u200B   Hello, World!   \u200B"
        val result = CodeFormatter.trimWhitespace(input, collapse = false, removeLeading = false)
        assertEquals("Hello, World!", result)
    }

    @Test
    fun `addLineNumbers prefixes each line with line number`() {
        val input = "line1\nline2\nline3"
        val expected = "1: line1\n2: line2\n3: line3"
        val result = CodeFormatter.addLineNumbers(input)
        assertEquals(expected, result)
    }

    @Test
    fun `collapseConsecutiveBlankLines collapses multiple blank lines into one`() {
        val input = "line1\n\n\n\nline2\n\nline3\n\n\n"
        val expected = "line1\n\nline2\n\nline3"
        val result = CodeFormatter.collapseConsecutiveBlankLines(input)
        assertEquals(expected, result)
    }

//    @Test
//    fun `removeComments for Java removes block and line comments`() {
//        val input = "int a = 5;\n// comment line\nint b = 6; /* block comment */\nint c = 7;"
//        val expected = "int a = 5;\nint b = 6;\nint c = 7;"
//        val result = CodeFormatter.removeComments(input, "java")
//        assertEquals(expected, result)
//    }

    @Test
    fun `removeComments for Python removes hash comments`() {
        val input = "print('hello')\n# this is a comment\nprint('world')"
        val expected = "print('hello')\nprint('world')"
        val result = CodeFormatter.removeComments(input, "python")
        assertEquals(expected, result)
    }

    @Test
    fun `removeImports for Java removes import lines`() {
        val input = "import java.util.List;\nclass A {}"
        val result = CodeFormatter.removeImports(input, "java")
        assertFalse(result.contains("import"))
        assertTrue(result.contains("class A {}"))
    }

    @Test
    fun `removeImports for Python removes import and from lines`() {
        val input = "import os\nfrom sys import path\nprint('hello')"
        val result = CodeFormatter.removeImports(input, "python")
        assertFalse(result.contains("import"))
        assertFalse(result.contains("from"))
        assertTrue(result.contains("print('hello')"))
    }

    @Test
    fun `applyCompression returns original text for NONE mode`() {
        val input = "Some   text with   extra spaces"
        val result = CodeFormatter.applyCompression(input, createOpts(CompressionMode.NONE))
        assertEquals(input, result)
    }

    @Test
    fun `applyCompression minimizes whitespace for MINIMAL mode`() {
        val input = "a  b\t\tc\u200B\u200Bd"
        val result = CodeFormatter.applyCompression(input, createOpts(CompressionMode.MINIMAL))
        assertEquals("a b c d", result)
    }

    @Test
    fun `applyCompression ultra mode without selective compression compresses newlines and spaces`() {
        val input = "Line    with  spaces\n\n\nAnother    line"
        val result = CodeFormatter.applyCompression(input, createOpts(CompressionMode.ULTRA, false))
        val expected = "Line with spaces Another line"
        assertEquals(expected, result)
    }

    @Test
    fun `applyCompression ultra mode with selective compression filters out lines containing TODO`() {
        val input = "Line    with  spaces\nTODO:   something\n\n\nAnother    line"
        val result = CodeFormatter.applyCompression(input, createOpts(CompressionMode.ULTRA, true))
        val expected = "Line with spaces Another line"
        assertEquals(expected, result)
    }

    @Test
    fun `chunkContent throws IllegalArgumentException for non-positive maxChunkSize`() {
        val exception = assertThrows<IllegalArgumentException> {
            CodeFormatter.chunkBySize("test", 0, preserveWords = false)
        }
        assertEquals("maxChunkSize must be positive", exception.message)
    }

    @Test
    fun `chunkContent returns single chunk when content length equals maxChunkSize`() {
        val content = "ExactSize"
        val chunks = CodeFormatter.chunkBySize(content, content.length, preserveWords = false)
        assertEquals(1, chunks.size)
        assertEquals(content, chunks[0])
    }

    @Test
    fun `chunkContent handles content with no whitespace correctly when preserving words`() {
        val content = "abcdefghij"
        val chunks = CodeFormatter.chunkBySize(content, 3, preserveWords = true)
        val expectedChunks = listOf("abc", "def", "ghi", "j")
        assertEquals(expectedChunks, chunks)
    }

    @ParameterizedTest(name = "Glob \"{0}\" matching path \"{1}\" should be {2}")
    @MethodSource("globTestProvider")
    fun `matchesGlob works correctly`(glob: String, fullPath: String, expected: Boolean) {
        val result = matchesGlob(glob, fullPath)
        assertEquals(expected, result, "Glob: \"$glob\" vs Path: \"$fullPath\"")
    }

    companion object {
        @JvmStatic
        fun globTestProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("src/main/kotlin/Example.kt", "src/main/kotlin/Example.kt", true),
            Arguments.of("src/main/kotlin/Ex?mple.kt", "src/main/kotlin/Example.kt", true),
            Arguments.of("***.java", "src/main/kotlin/Example.kt", false),
            Arguments.of("srcExample.kt", "src/main/kotlin/Example.kt", true),
            Arguments.of("src*.kt", "src/Example.kt", false),
            Arguments.of("*Example.kt", "src/main/kotlin/Example.kt", true),
            Arguments.of("**", "any/path/should/match", true),
            Arguments.of("no/match", "different/path", false),
        )

        private fun createOpts(mode: CompressionMode, selective: Boolean = false) =
            ClipCraftOptions(compressionMode = mode, selectiveCompression = selective)
    }
}
