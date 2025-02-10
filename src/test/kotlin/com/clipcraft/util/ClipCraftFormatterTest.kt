package com.clipcraft.util

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.CompressionMode
import com.clipcraft.model.OutputFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
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
  val chunks = ClipCraftFormatter.chunkContent(content, 50)
  assertEquals(1, chunks.size)
  assertEquals(content, chunks[0])
 }

 @Test
 fun `chunkContent reassembles original content with preserveWords true`() {
  val content = "This is a sample text that should be split into multiple chunks without breaking words."
  val maxChunkSize = 20
  val chunks = ClipCraftFormatter.chunkContent(content, maxChunkSize, preserveWords = true)
  val reassembled = chunks.joinToString(separator = "") { it }
  assertEquals(content, reassembled)
 }

 @Test
 fun `chunkContent reassembles original content with preserveWords false`() {
  val content = "This is a sample text that should be split into multiple chunks."
  val maxChunkSize = 10
  val chunks = ClipCraftFormatter.chunkContent(content, maxChunkSize, preserveWords = false)
  val reassembled = chunks.joinToString(separator = "") { it }
  assertEquals(content, reassembled)
 }

 @Test
 fun `each chunk is within the specified maxChunkSize when preserving words`() {
  val content = "Line one.\nLine two is a bit longer.\nLine three."
  val maxChunkSize = 25
  val chunks = ClipCraftFormatter.chunkContent(content, maxChunkSize, preserveWords = true)
  chunks.forEach { chunk ->
   assertTrue(chunk.length <= maxChunkSize) { "Chunk exceeds max size: \"$chunk\"" }
  }
  val reassembled = chunks.joinToString(separator = "") { it }
  assertEquals(content, reassembled)
 }

 // --- Additional tests for whitespace and formatting ---

 @Test
 fun `trimLineWhitespaceAdvanced removes leading and trailing whitespace and zero-width spaces`() {
  val input = "\u200B   Hello, World!   \u200B"
  val expected = "Hello, World!"
  val result = ClipCraftFormatter.trimLineWhitespaceAdvanced(input)
  assertEquals(expected, result)
 }

 @Test
 fun `minimizeWhitespace collapses extra spaces and trims lines`() {
  val input = "   hello   \n\n\n   world  \n\n"
  val expected = "hello\n\nworld"
  val result = ClipCraftFormatter.minimizeWhitespace(input)
  assertEquals(expected, result)
 }

 @Test
 fun `addLineNumbers prefixes each line with line number`() {
  val input = "line1\nline2\nline3"
  val expected = "   1: line1\n   2: line2\n   3: line3"
  val result = ClipCraftFormatter.addLineNumbers(input)
  assertEquals(expected, result)
 }

 @Test
 fun `collapseConsecutiveBlankLines collapses multiple blank lines into one`() {
  val input = "line1\n\n\n\nline2\n\nline3\n\n\n"
  val expected = "line1\n\nline2\n\nline3"
  val result = ClipCraftFormatter.collapseConsecutiveBlankLines(input)
  assertEquals(expected, result)
 }

 @Test
 fun `removeLeadingBlankLines removes all blank lines at the beginning`() {
  val input = "\n\n   \nline1\nline2"
  val expected = "line1\nline2"
  val result = ClipCraftFormatter.removeLeadingBlankLines(input)
  assertEquals(expected, result)
 }

 @Test
 fun `removeComments for C-style languages removes block and line comments`() {
  val input = "int a = 5;\n// This is a comment\nint b = 6; /* block comment */\nint c = 7;"
  val expected = "int a = 5;\nint b = 6; \nint c = 7;"
  val result = ClipCraftFormatter.removeComments(input, "java")
  assertEquals(expected, result)
 }

 @Test
 fun `removeComments for Python removes hash comments`() {
  val input = "print('hello')\n# this is a comment\nprint('world')"
  val expected = "print('hello')\nprint('world')"
  val result = ClipCraftFormatter.removeComments(input, "python")
  assertEquals(expected, result)
 }

 @Test
 fun `removeImports for Java removes import lines`() {
  val input = "import java.util.List;\nclass A {}"
  val result = ClipCraftFormatter.removeImports(input, "java")
  assertFalse(result.contains("import"))
  assertTrue(result.contains("class A {}"))
 }

 @Test
 fun `removeImports for Python removes import and from lines`() {
  val input = "import os\nfrom sys import path\nprint('hello')"
  val result = ClipCraftFormatter.removeImports(input, "python")
  assertFalse(result.contains("import"))
  assertFalse(result.contains("from"))
  assertTrue(result.contains("print('hello')"))
 }

 // --- Compression and chunk tests ---

 private fun createOptions(
  compressionMode: CompressionMode,
  selectiveCompression: Boolean = false
 ): ClipCraftOptions {
  return ClipCraftOptions(
   trimLineWhitespace = false,
   removeComments = false,
   removeImports = false,
   collapseBlankLines = false,
   singleLineOutput = false,
   compressionMode = compressionMode,
   selectiveCompression = selectiveCompression,
   macros = emptyMap(),
   includeLineNumbers = false
  )
 }

 @Test
 fun `applyCompression returns original text for NONE mode`() {
  val input = "Some   text with   extra spaces"
  val opts = createOptions(CompressionMode.NONE)
  val result = ClipCraftFormatter.applyCompression(input, opts)
  assertEquals(input, result)
 }

 @Test
 fun `applyCompression minimizes whitespace for MINIMAL mode`() {
  val input = "a  b\t\tc\u200B\u200Bd"
  val opts = createOptions(CompressionMode.MINIMAL)
  val result = ClipCraftFormatter.applyCompression(input, opts)
  val expected = "a b c d"
  assertEquals(expected, result)
 }

 @Test
 fun `applyCompression ultra mode without selective compression compresses newlines and spaces`() {
  val input = "Line    with  spaces\n\n\nAnother    line"
  val opts = createOptions(CompressionMode.ULTRA, selectiveCompression = false)
  val result = ClipCraftFormatter.applyCompression(input, opts)
  val expected = "Line with spaces\nAnother line"
  assertEquals(expected, result)
 }

 @Test
 fun `applyCompression ultra mode with selective compression filters out lines containing TODO or debug log`() {
  val input = "Line    with  spaces\nTODO:   something\n\n\nAnother    line"
  val opts = createOptions(CompressionMode.ULTRA, selectiveCompression = true)
  val result = ClipCraftFormatter.applyCompression(input, opts)
  val expected = "Line with spaces\nAnother line"
  assertEquals(expected, result)
 }

 @Test
 fun `chunkContent throws IllegalArgumentException for non-positive maxChunkSize`() {
  val exception = assertThrows<IllegalArgumentException> {
   ClipCraftFormatter.chunkContent("any content", 0)
  }
  assertTrue(exception.message!!.contains("maxChunkSize must be positive"))
 }

 @Test
 fun `chunkContent returns single chunk when content length equals maxChunkSize`() {
  val content = "ExactSize"
  val chunks = ClipCraftFormatter.chunkContent(content, content.length)
  assertEquals(1, chunks.size)
  assertEquals(content, chunks[0])
 }

 @Test
 fun `chunkContent handles content with no whitespace correctly when preserving words`() {
  val content = "abcdefghij" // 10 characters, no whitespace
  val chunks = ClipCraftFormatter.chunkContent(content, 3, preserveWords = true)
  val expectedChunks = listOf("abc", "def", "ghi", "j")
  assertEquals(expectedChunks, chunks)
 }

 // --- Additional tests for matchesGlob ---

 companion object {
  @JvmStatic
  fun globTestProvider(): Stream<Arguments> = Stream.of(
   // Provided test cases:
   Arguments.of("**/*.kt", "src/main/kotlin/Example.kt", true),
   Arguments.of("**/*.java", "src/main/kotlin/Example.kt", false),
   Arguments.of("src/**/Example.kt", "src/main/kotlin/Example.kt", true),
   Arguments.of("src/*/*.kt", "src/main/kotlin/Example.kt", true),
   Arguments.of("src/*/*.kt", "src/Example.kt", false),
   // Additional test cases:
   Arguments.of("src/main/kotlin/Example.kt", "src/main/kotlin/Example.kt", true), // exact match
   Arguments.of("src/main/kotlin/Ex?mple.kt", "src/main/kotlin/Example.kt", true),  // single char wildcard
   Arguments.of("*Example.kt", "src/main/kotlin/Example.kt", true),                // leading wildcard
   Arguments.of("**", "any/path/should/match", true),                              // match everything
   Arguments.of("no/match", "different/path", false)                              // no match
  )

  @JvmStatic
  fun formatBlockProvider(): Stream<Arguments> = Stream.of(
   Arguments.of(OutputFormat.MARKDOWN, "```java\ncode snippet\n```"),
   Arguments.of(OutputFormat.PLAIN, "code snippet"),
   Arguments.of(OutputFormat.HTML, "<pre><code class=\"java\">code snippet</code></pre>")
  )
 }

 @ParameterizedTest
 @MethodSource("globTestProvider")
 fun `matchesGlob works correctly`(glob: String, fullPath: String, expected: Boolean) {
  val result = ClipCraftFormatter.matchesGlob(glob, fullPath)
  assertEquals(expected, result, "Glob: \"$glob\" vs Path: \"$fullPath\"")
 }

 @ParameterizedTest
 @MethodSource("formatBlockProvider")
 fun `formatBlock formats content correctly`(format: OutputFormat, expected: String) {
  val result = ClipCraftFormatter.formatBlock("code snippet", "java", format)
  assertEquals(expected, result)
 }

 @Test
 fun `filterFilesByGitIgnore returns input file list unchanged`() {
  val fileList = listOf("file1.txt", "file2.txt")
  val dummyGitIgnore = File("dummy.gitignore")
  val result = ClipCraftFormatter.filterFilesByGitIgnore(fileList, dummyGitIgnore)
  assertEquals(fileList, result)
 }

 @Test
 fun `processContent applies pipeline steps correctly`() {
  // Input contains extra whitespace, an import, and a comment.
  val input = "   // Comment\n   import java.util.List;\npublic class A { }   "
  val opts = createOptions(CompressionMode.NONE).copy(
   trimLineWhitespace = true,
   removeComments = true,
   removeImports = true,
   collapseBlankLines = true,
   singleLineOutput = false,
   includeLineNumbers = false
  )
  val result = ClipCraftFormatter.processContent(input, opts, "java")
  // Expected: comments and imports are removed, extra whitespace trimmed.
  val expected = "public class A { }"
  assertEquals(expected, result)
 }
}
