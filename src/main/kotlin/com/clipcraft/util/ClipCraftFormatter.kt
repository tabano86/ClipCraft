package com.clipcraft.util

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.CompressionMode
import com.clipcraft.model.OutputFormat
import com.clipcraft.services.ClipCraftMacroManager
import org.slf4j.LoggerFactory
import java.io.File

// Dependency: implementation("com.google.googlejavaformat:google-java-format:<version>")
import com.google.googlejavaformat.java.Formatter as GoogleJavaFormatter

object ClipCraftFormatter {

    private val log = LoggerFactory.getLogger(ClipCraftFormatter::class.java)

    private val globRegexCache = mutableMapOf<String, Regex>()

    private val regexBlockComment = Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL)
    private val regexHorizontalWhitespace = Regex("[ \\t\u200B]{2,}")

    fun processContent(originalText: String, opts: ClipCraftOptions, language: String): String {
        val pipeline = listOf<(String) -> String>(
            ::removeLeadingBlankLines,
            { s -> if (opts.trimLineWhitespace) trimLineWhitespaceAdvanced(s) else s },
            { s -> if (opts.removeComments) removeComments(s, language) else s },
            { s -> if (opts.removeImports) removeImports(s, language) else s },
            {
                when (language.lowercase()) {
                    "java" -> externalFormatJavaIfAvailable(it)
                    "javascript", "typescript" -> externalFormatJsIfAvailable(it, language)
                    else -> it
                }
            },
            { s -> applyCompression(s, opts) },
            { s -> if (opts.collapseBlankLines) collapseConsecutiveBlankLines(s) else s },
            { s -> if (opts.singleLineOutput) s.replace("\n", " ") else s },
            { s -> ClipCraftMacroManager.applyMacros(s, opts.macros) },
            { s -> if (opts.includeLineNumbers) addLineNumbers(s) else s }
        )
        return pipeline.fold(originalText) { acc, step -> step(acc) }.trimEnd()
    }

    private fun trimLineWhitespaceAdvanced(text: String): String =
        text.lineSequence().map { it.trim().trim('\u200B') }.joinToString("\n")

    fun minimizeWhitespace(input: String): String =
        input.lineSequence()
            .map { it.trim() }
            .fold(mutableListOf<String>()) { acc, line ->
                if (line.isEmpty() && acc.lastOrNull()?.isEmpty() == true) acc else acc.apply { add(line) }
            }
            .joinToString("\n")
            .trim()

    private fun externalFormatJavaIfAvailable(text: String): String =
        try {
            GoogleJavaFormatter().formatSource(text)
        } catch (e: Exception) {
            log.warn("Google Java Format failed; returning original text. Reason: ${e.message}")
            text
        }

    private fun externalFormatJsIfAvailable(text: String, language: String): String {
        log.debug("Pretending to run Prettier for $language; returning original text.")
        return text
    }

    private fun addLineNumbers(text: String): String =
        text.lines().mapIndexed { idx, line -> "%4d: %s".format(idx + 1, line) }.joinToString("\n")

    fun collapseConsecutiveBlankLines(text: String): String {
        val sb = StringBuilder()
        var lastBlank = false
        for (line in text.lineSequence()) {
            if (line.isBlank()) {
                if (!lastBlank) {
                    sb.append("\n")
                    lastBlank = true
                }
            } else {
                sb.append(line).append("\n")
                lastBlank = false
            }
        }
        return sb.toString().trimEnd()
    }

    fun removeLeadingBlankLines(input: String): String =
        input.lineSequence().dropWhile { it.isBlank() }.joinToString("\n")

    fun removeComments(input: String, language: String): String {
        return when (language.lowercase()) {
            "java", "kotlin", "javascript", "typescript", "csharp", "cpp" -> {
                val noBlock = input.replace(regexBlockComment, "")
                noBlock.lineSequence()
                    .filter { !it.trim().startsWith("//") }
                    .joinToString("\n")
            }
            "python", "ruby", "sh", "bash" -> {
                input.lineSequence().filter { !it.trim().startsWith("#") }
                    .joinToString("\n")
            }
            else -> input
        }
    }

    fun removeImports(input: String, language: String): String {
        return when (language.lowercase()) {
            "java", "kotlin", "javascript", "typescript", "csharp", "cpp" ->
                input.lineSequence().filter { !it.trim().startsWith("import ") }
                    .joinToString("\n")
            "python" ->
                input.lineSequence().filter {
                    val trimmed = it.trim()
                    !(trimmed.startsWith("import ") || trimmed.startsWith("from "))
                }.joinToString("\n")
            else -> input
        }
    }

    fun applyCompression(input: String, opts: ClipCraftOptions): String {
        return when (opts.compressionMode) {
            CompressionMode.NONE -> input
            CompressionMode.MINIMAL -> input.replace(regexHorizontalWhitespace, " ")
            CompressionMode.ULTRA -> {
                var result = input.replace(Regex("\\n{3,}"), "\n\n")
                if (opts.selectiveCompression) {
                    result = result.lineSequence()
                        .filter { it.isNotBlank() && !it.contains("TODO", ignoreCase = true) && !it.contains("debug log", ignoreCase = true) }
                        .joinToString("\n")
                }
                result.lines().joinToString("\n") { it.replace(regexHorizontalWhitespace, " ") }.trim()
            }
        }
    }

    fun chunkContent(text: String, chunkSize: Int, respectLineBoundaries: Boolean): List<String> {
        val effectiveSize = if (chunkSize <= 0) 3000 else chunkSize
        return if (!respectLineBoundaries) {
            val chunks = ArrayList<String>()
            var index = 0
            val len = text.length
            while (index < len) {
                val end = minOf(index + effectiveSize, len)
                chunks.add(text.substring(index, end))
                index = end
            }
            chunks
        } else {
            // Ensure input ends with newline so that every line is complete.
            val input = if (text.endsWith("\n")) text else text + "\n"
            val lines = input.lines().filter { it.isNotEmpty() }
            val chunks = mutableListOf<String>()
            val currentChunk = mutableListOf<String>()
            var currentLength = 0
            for (line in lines) {
                val lineWithNewline = "$line\n"
                if (currentLength + lineWithNewline.length > effectiveSize && currentChunk.isNotEmpty()) {
                    chunks.add(currentChunk.joinToString(""))
                    currentChunk.clear()
                    currentLength = 0
                }
                currentChunk.add(lineWithNewline)
                currentLength += lineWithNewline.length
            }
            if (currentChunk.isNotEmpty()) {
                chunks.add(currentChunk.joinToString(""))
            }
            chunks
        }
    }

    fun formatBlock(content: String, language: String, format: OutputFormat): String {
        return when (format) {
            OutputFormat.MARKDOWN -> "```$language\n$content\n```"
            OutputFormat.PLAIN -> content
            OutputFormat.HTML -> "<pre><code class=\"$language\">$content</code></pre>"
        }
    }

    fun filterFilesByGitIgnore(filePaths: List<String>, gitIgnoreFile: File): List<String> = filePaths

    private fun globToRegex(glob: String): String {
        return globRegexCache.getOrPut(glob) {
            Regex(
                glob.replace(".", "\\.")
                    .replace("*", ".*")
                    .replace("?", ".?")
            )
        }.pattern
    }

    private fun matchesGlob(glob: String, fullPath: String): Boolean {
        val regexPattern = "^" + globToRegex(glob) + "$"
        return Regex(regexPattern).containsMatchIn(fullPath)
    }
}
