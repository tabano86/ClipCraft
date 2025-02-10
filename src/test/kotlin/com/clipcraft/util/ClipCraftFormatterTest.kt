package com.clipcraft.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

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
  // When reassembled, the chunks must equal the original content.
  val reassembled = chunks.joinToString(separator = "") { it }
  assertEquals(content, reassembled)
 }

 @Test
 fun `chunkContent reassembles original content with preserveWords false`() {
  val content = "This is a sample text that should be split into multiple chunks."
  val maxChunkSize = 10
  val chunks = ClipCraftFormatter.chunkContent(content, maxChunkSize, preserveWords = false)
  // When reassembled, the chunks must equal the original content.
  val reassembled = chunks.joinToString(separator = "") { it }
  assertEquals(content, reassembled)
 }

 @Test
 fun `each chunk is within the specified maxChunkSize when preserving words`() {
  val content = "Line one.\nLine two is a bit longer.\nLine three."
  val maxChunkSize = 25
  val chunks = ClipCraftFormatter.chunkContent(content, maxChunkSize, preserveWords = true)
  chunks.forEach { chunk ->
   // The chunk may be shorter than maxChunkSize due to word preservation.
   assertTrue(chunk.length <= maxChunkSize) { "Chunk exceeds max size: \"$chunk\"" }
  }
  // Verify reassembly
  val reassembled = chunks.joinToString(separator = "") { it }
  assertEquals(content, reassembled)
 }
}
