package com.clipcraft

import com.clipcraft.model.ClipCraftOptions
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
  val opts = ClipCraftOptions(includeLineNumbers = false, removeComments = true)
  val result = action.processContent(content, opts, "java")
  val expected = "code line\nmore code"
  assertEquals(expected, result)
 }

 @Test
 fun testProcessContentWithTrimLineWhitespace() {
  val action = ClipCraftAction()
  val content = "  foo  \n  bar  "
  val opts = ClipCraftOptions(includeLineNumbers = false, trimLineWhitespace = true)
  val result = action.processContent(content, opts, "txt")
  val expected = "foo\nbar"
  assertEquals(expected, result)
 }
}
