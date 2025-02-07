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
  val result = action.processContent(content, opts)
  val expected = "   1: foo\n   2: bar\n   3: baz"
  assertEquals(expected, result)
 }

 @Test
 fun testProcessContentWithoutLineNumbers() {
  val action = ClipCraftAction()
  val content = "alpha\nbeta\ngamma"
  val opts = ClipCraftOptions(includeLineNumbers = false)
  val result = action.processContent(content, opts)
  val expected = "alpha\nbeta\ngamma"
  assertEquals(expected, result)
 }
}
