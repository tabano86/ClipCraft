package com.clipcraft

import com.clipcraft.model.ClipCraftOptions
import org.junit.Assert.assertEquals
import org.junit.Test

class ClipCraftActionTest {

 @Test
 fun testProcessContentWithLineNumbers() {
  val action = ClipCraftAction()
  val content = "first line\nsecond line\nthird line"
  val options = ClipCraftOptions(includeLineNumbers = true)
  val result = action.processContent(content, options)
  val expected = "   1: first line\n   2: second line\n   3: third line"
  assertEquals(expected, result)
 }

 @Test
 fun testProcessContentWithoutLineNumbers() {
  val action = ClipCraftAction()
  val content = "alpha\nbeta\ngamma"
  val options = ClipCraftOptions(includeLineNumbers = false)
  val result = action.processContent(content, options)
  val expected = "alpha\nbeta\ngamma"
  assertEquals(expected, result)
 }
}
