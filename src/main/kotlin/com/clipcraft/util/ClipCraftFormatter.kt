package com.clipcraft.util

import com.clipcraft.model.*
import com.clipcraft.services.ClipCraftMacroManager
import com.github.onelenyk.gitignore.parser.GitIgnoreParser
import java.io.File

object ClipCraftFormatter {

    /**
     * Main content processing pipeline.
     */
    fun processContent(text: String, opts: ClipCraftOptions, language: String): String {
        var processed = removeLeadingBlankLines(text)
        if (opts.trimLineWhitespace) {
            processed = processed.lines().joinToString("\n") { it.trimEnd() }
        }
        if (opts.removeComments) {
            processed = removeComments(processed, language)
        }
        if (opts.removeImports) {
            processed = removeImports(processed, language)
        }
        processed = applyCompression(processed, opts)

        if (opts.collapseBlankLines) {
            processed = collapseConsecutiveBlankLines(processed)
        }
        if (opts.singleLineOutput) {
            processed = processed.replace("\n", " ")
        }
        processed = ClipCraftMacroManager.applyMacros(processed, opts.macros)

        if (opts.includeLineNumbers) {
            processed = processed.lines()
                .mapIndexed { idx, line -> "%4d: %s".format(idx + 1, line) }
                .joinToString("\n")
        }
        return processed.trimEnd()
    }

    /**
     * Format a processed snippet into the requested [OutputFormat].
     */
    fun formatBlock(content: String, language: String, format: OutputFormat): String {
        return when (format) {
            OutputFormat.MARKDOWN -> "```$language\n$content\n```"
            OutputFormat.PLAIN -> content
            OutputFormat.HTML -> "<pre><code class=\"$language\">$content</code></pre>"
        }
    }

    /**
     * Splits text into multiple chunks, optionally respecting line boundaries.
     * If [respectLineBoundaries] is true, tries not to split in the middle of a line.
     */
    fun chunkContent(text: String, chunkSize: Int, respectLineBoundaries: Boolean): List<String> {
        val effectiveSize = if (chunkSize <= 0) 3000 else chunkSize
        if (!respectLineBoundaries) {
            val chunks = mutableListOf<String>()
            var index = 0
            while (index < text.length) {
                val end = minOf(index + effectiveSize, text.length)
                chunks.add(text.substring(index, end))
                index = end
            }
            return chunks
        }

        val lines = text.lines()
        val chunks = mutableListOf<String>()
        var currentChunk = StringBuilder()
        for (line in lines) {
            if (currentChunk.length + line.length + 1 > effectiveSize && currentChunk.isNotEmpty()) {
                chunks.add(currentChunk.toString())
                currentChunk = StringBuilder()
            }
            currentChunk.append(line).append("\n")
        }
        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString().trimEnd())
        }
        return chunks
    }

    /**
     * Collapses consecutive blank lines into one.
     */
    fun collapseConsecutiveBlankLines(text: String): String {
        val lines = text.lines()
        val sb = StringBuilder()
        var lastLineBlank = false
        for (line in lines) {
            if (line.isBlank()) {
                if (!lastLineBlank) {
                    sb.append("\n")
                    lastLineBlank = true
                }
            } else {
                sb.append(line).append("\n")
                lastLineBlank = false
            }
        }
        return sb.toString().trimEnd()
    }

    /**
     * Minimal approach: remove leading blank lines entirely.
     */
    fun removeLeadingBlankLines(input: String): String {
        return if (input.isBlank()) {
            input.trim()
        } else {
            val lines = input.lines()
            val trimmed = lines.dropWhile { it.isBlank() }
            trimmed.joinToString("\n")
        }
    }

    /**
     * Helper to remove multiline comments and line comments in typical C/Java style,
     * plus # comments for python, etc.
     */
    fun removeComments(input: String, language: String): String {
        return when (language) {
            "java", "kotlin", "javascript", "typescript", "csharp", "cpp" -> {
                val noBlock = input.replace(Regex("/\\*.*?\\*/", setOf(RegexOption.DOT_MATCHES_ALL)), "")
                noBlock.lines().filter { !it.trim().startsWith("//") }.joinToString("\n")
            }
            "python", "ruby", "sh", "bash" ->
                input.lines().filter { !it.trim().startsWith("#") }.joinToString("\n")
            else -> input
        }
    }

    /**
     * Removes import statements for Java/Kotlin/JS/TS/C# etc., or Python import lines.
     */
    fun removeImports(input: String, language: String): String {
        return when (language) {
            "java", "kotlin", "javascript", "typescript", "csharp", "cpp" ->
                input.lines().filter { !it.trim().startsWith("import ") }.joinToString("\n")
            "python" ->
                input.lines().filter {
                    val trimmed = it.trim()
                    !trimmed.startsWith("import ") && !trimmed.startsWith("from ")
                }.joinToString("\n")
            else -> input
        }
    }

    /**
     * Applies advanced compression as specified in [ClipCraftOptions].
     */
    fun applyCompression(input: String, opts: ClipCraftOptions): String {
        return when (opts.compressionMode) {
            CompressionMode.NONE -> input
            CompressionMode.MINIMAL -> {
                input.replace(Regex("\\s{2,}"), " ")
            }
            CompressionMode.ULTRA -> {
                var result = input.replace(Regex("\\n{3,}"), "\n\n")
                if (opts.selectiveCompression) {
                    result = result.lines()
                        .filterNot { line ->
                            line.contains("TODO", ignoreCase = true) ||
                                    line.contains("debug log", ignoreCase = true)
                        }
                        .joinToString("\n")
                }
                result.replace(Regex("\\s{2,}"), " ").trim()
            }
        }
    }

    /**
     * Minimizes repeated blank lines in the entire text.
     */
    fun minimizeWhitespace(input: String): String {
        val lines = input.lines()
        val result = mutableListOf<String>()
        for (line in lines) {
            if (line.isBlank() && result.lastOrNull()?.isBlank() == true) {
                // skip repeated blank lines
            } else {
                result += line
            }
        }
        return result.joinToString("\n")
    }

    /**
     * Filters a list of file paths by applying .gitignore rules from the specified [gitIgnoreFile].
     * This uses the onelenyk/gitignore-parser library.
     */
    fun filterFilesByGitIgnore(filePaths: List<String>, gitIgnoreFile: File): List<String> {
        val gitIgnore = GitIgnoreParser.parse(gitIgnoreFile)
        return filePaths.filter { !gitIgnore.isIgnored(it) }
    }
}
