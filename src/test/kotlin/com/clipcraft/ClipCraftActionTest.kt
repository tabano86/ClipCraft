package com.clipcraft

import com.clipcraft.model.ClipCraftOptions
import org.junit.Assert.assertEquals
import org.junit.Test

class ClipCraftActionTest {

 @Test
 fun testProcessContentWithLineNumbers() {
  val action = ClipCraftAction()
  val content = "alpha\nbeta\ngamma"
  val opts = ClipCraftOptions(includeLineNumbers = true)
  val result = action.processContent(content, opts)
  val expected = "   1: alpha\n   2: beta\n   3: gamma"
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
