package com.clipcraft.util

import com.clipcraft.model.CompressionMode
import com.clipcraft.model.OutputFormat
import matchesGlob
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.util.stream.Stream

class ClipCraftFormatterTest {

    @Test
    fun `chunkContent returns single chunk when content is shorter than maxChunkSize`() {
        val content = "Short content"
        val chunks = CodeFormatter.chunkBySize(content, 50)
        assertEquals(1, chunks.size)
        assertEquals(content, chunks[0])
    }

    @Test
    fun `chunkContent reassembles original content with preserveWords true`() {
        val content = "This is a sample text that should be split into multiple chunks without breaking words."
        val maxChunkSize = 20
        val chunks = CodeFormatter.chunkBySize(content, maxChunkSize)
        val reassembled = chunks.joinToString(separator = "") { it }
        assertEquals(content, reassembled)
    }

    @Test
    fun `each chunk is within the specified maxChunkSize`() {
        val content = "Line one. Line two is a bit longer. Line three."
        val maxChunkSize = 25
        val chunks = CodeFormatter.chunkBySize(content, maxChunkSize)
        chunks.forEach { chunk ->
            assertTrue(chunk.length <= maxChunkSize) { "Chunk exceeds max size: \"$chunk\"" }
        }
        val reassembled = chunks.joinToString(separator = "") { it }
        assertEquals(content, reassembled)
    }

    @Test
    fun `trimLineWhitespaceAdvanced removes leading + trailing whitespace and zero-width spaces`() {
        val input = "\u200B   Hello, World!   \u200B"
        val expected = "Hello, World!"
        val result = CodeFormatter.trimWhitespace(
            input,
            collapseBlankLines = false,
            removeLeadingBlankLines = false
        )
        assertEquals(expected, result)
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

    @Test
    fun `removeLeadingBlankLines removes all blank lines at the beginning`() {
        val input = "\n\n   \nline1\nline2"
        val expected = "line1\nline2"
        val result = CodeFormatter.removeLeadingBlankLines(input)
        assertEquals(expected, result)
    }

    @Test
    fun `removeComments for Java removes block and line comments`() {
        val input = "int a = 5;\n// This is a comment\nint b = 6; /* block comment */\nint c = 7;"
        val expected = "int a = 5;\nint b = 6;\nint c = 7;"
        val result = CodeFormatter.removeComments(input, "java")
        assertEquals(expected, result)
    }

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
        val opts = CodeFormatterTestHelper.createOptions(CompressionMode.NONE)
        val result = CodeFormatter.applyCompression(input, opts)
        assertEquals(input, result)
    }

    @Test
    fun `applyCompression minimizes whitespace for MINIMAL mode`() {
        val input = "a  b\t\tc\u200B\u200Bd"
        val opts = CodeFormatterTestHelper.createOptions(CompressionMode.MINIMAL)
        val result = CodeFormatter.applyCompression(input, opts)
        val expected = "a b c d"
        assertEquals(expected, result)
    }

    @Test
    fun `applyCompression ultra mode without selective compression compresses newlines and spaces`() {
        val input = "Line    with  spaces\n\n\nAnother    line"
        val opts = CodeFormatterTestHelper.createOptions(CompressionMode.ULTRA, selectiveCompression = false)
        val result = CodeFormatter.applyCompression(input, opts)
        val expected = "Line with spaces Another line"
        assertEquals(expected, result)
    }

    @Test
    fun `applyCompression ultra mode with selective compression filters out lines containing TODO`() {
        val input = "Line    with  spaces\nTODO:   something\n\n\nAnother    line"
        val opts = CodeFormatterTestHelper.createOptions(CompressionMode.ULTRA, selectiveCompression = true)

        println("Test sees selectiveCompression = ${opts.selectiveCompression}")
        // Possibly call opts.resolveConflicts() here if your code does that, to see if it changes

        val result = CodeFormatter.applyCompression(input, opts)
        val expected = "Line with spaces Another line"
        assertEquals(expected, result)
    }


    @Test
    fun `chunkContent throws IllegalArgumentException for non-positive maxChunkSize`() {
        val exception = assertThrows<IllegalArgumentException> {
            CodeFormatter.chunkBySize("any content", 0)
        }
        assertTrue(exception.message!!.contains("maxChunkSize must be positive"))
    }

    @Test
    fun `chunkContent returns single chunk when content length equals maxChunkSize`() {
        val content = "ExactSize"
        val chunks = CodeFormatter.chunkBySize(content, content.length)
        assertEquals(1, chunks.size)
        assertEquals(content, chunks[0])
    }

    @Test
    fun `chunkContent handles content with no whitespace correctly when preserving words`() {
        val content = "abcdefghij"
        val chunks = CodeFormatter.chunkBySize(content, 3)
        val expectedChunks = listOf("abc", "def", "ghi", "j")
        assertEquals(expectedChunks, chunks)
    }

    @ParameterizedTest(name = "Glob \"{0}\" matching path \"{1}\" should be {2}")
    @MethodSource("globTestProvider")
    fun `matchesGlob works correctly`(glob: String, fullPath: String, expected: Boolean) {
        val result = matchesGlob(glob, fullPath)
        assertEquals(expected, result, "Glob: \"$glob\" vs Path: \"$fullPath\"")
    }

    @ParameterizedTest(name = "FormatBlock returns expected output for format {0}")
    @MethodSource("formatBlockProvider")
    fun `formatBlock formats content correctly`(format: OutputFormat, expected: String) {
        val result = CodeFormatter.formatBlock("code snippet", "java", format)
        assertEquals(expected, result)
    }

    @Test
    fun `filterFilesByGitIgnore returns input file list unchanged when file does not exist`() {
        val fileList = listOf("file1.txt", "file2.txt")
        val dummyGitIgnore = File("dummy.gitignore")
        val result = CodeFormatter.filterFilesByGitIgnore(fileList, dummyGitIgnore)
        assertEquals(fileList, result)
    }

    companion object {
        @JvmStatic
        fun globTestProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("**/*.kt", "src/main/kotlin/Example.kt", true),
            Arguments.of("**/*.java", "src/main/kotlin/Example.kt", false),
            Arguments.of("src/**/Example.kt", "src/main/kotlin/Example.kt", true),
            Arguments.of("src/*/*.kt", "src/main/kotlin/Example.kt", true),
            Arguments.of("src/*/*.kt", "src/Example.kt", false),
            Arguments.of("src/main/kotlin/Example.kt", "src/main/kotlin/Example.kt", true),
            Arguments.of("src/main/kotlin/Ex?mple.kt", "src/main/kotlin/Example.kt", true),
            Arguments.of("*Example.kt", "src/main/kotlin/Example.kt", true),
            Arguments.of("**", "any/path/should/match", true),
            Arguments.of("no/match", "different/path", false)
        )

        @JvmStatic
        fun formatBlockProvider(): Stream<Arguments> = Stream.of(
            Arguments.of(OutputFormat.MARKDOWN, "```java\ncode snippet\n```"),
            Arguments.of(OutputFormat.PLAIN, "code snippet"),
            Arguments.of(OutputFormat.HTML, "<pre><code class=\"java\">code snippet</code></pre>")
        )
    }
}