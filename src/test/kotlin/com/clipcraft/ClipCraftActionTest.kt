package com.clipcraft

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.OutputFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class ClipCraftActionTest {
 @Test
 fun testProcessContentWithLineNumbers() {
  val action = ClipCraftAction()
  val content = "foo\nbar\nbaz"
  val opts = ClipCraftOptions(includeLineNumbers = true)
  val result = action.processContent(content, opts, "txt")
  val expected = "   1: foo" + System.lineSeparator() +
          "   2: bar" + System.lineSeparator() +
          "   3: baz"
  assertEquals(expected, result)
 }

 @Test
 fun testProcessContentWithoutLineNumbers() {
  val action = ClipCraftAction()
  val content = "alpha\nbeta\ngamma"
  val opts = ClipCraftOptions(includeLineNumbers = false)
  val result = action.processContent(content, opts, "txt")
  val expected = "alpha" + System.lineSeparator() +
          "beta" + System.lineSeparator() +
          "gamma"
  assertEquals(expected, result)
 }

 @Test
 fun testProcessContentWithRemoveComments() {
  val action = ClipCraftAction()
  val content = "code line\n// comment line\nmore code"
  val opts = ClipCraftOptions(includeLineNumbers = false, removeComments = true)
  val result = action.processContent(content, opts, "java")
  val expected = "code line" + System.lineSeparator() +
          "more code"
  assertEquals(expected, result)
 }

 @Test
 fun testProcessContentWithTrimLineWhitespace() {
  val action = ClipCraftAction()
  val content = "  foo  \n  bar  "
  val opts = ClipCraftOptions(includeLineNumbers = false, trimLineWhitespace = true)
  val result = action.processContent(content, opts, "txt")
  val expected = "  foo" + System.lineSeparator() +
          "  bar"
  assertEquals(expected, result)
 }

 @Test
 fun testProcessContentWithRemoveImports() {
  val action = ClipCraftAction()
  val content = "import java.util.*\npublic class Test {}"
  val opts = ClipCraftOptions(includeLineNumbers = false, removeImports = true)
  val result = action.processContent(content, opts, "java")
  val expected = "public class Test {}"
  assertEquals(expected, result.trim())
 }

 @Test
 fun testOutputFormatHTML() {
  val action = ClipCraftAction()
  val content = "foo\nbar"
  val opts = ClipCraftOptions(includeLineNumbers = false, outputFormat = OutputFormat.HTML)
  val result = action.processContent(content, opts, "txt")
  val expected = "foo" + System.lineSeparator() +
          "bar"
  assertEquals(expected, result)
 }
}
