package com.clipcraft.util

import com.clipcraft.model.ChunkStrategy
import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.CompressionMode
import com.clipcraft.model.OutputFormat
import com.clipcraft.model.Snippet
import org.apache.commons.lang3.StringEscapeUtils
import kotlin.math.min

object CodeFormatter {

    fun formatSnippets(snippets: List<Snippet>, options: ClipCraftOptions): List<String> {
        options.resolveConflicts()
        val merged = snippets.joinToString("\n\n") { formatSingleSnippet(it, options) }.trim()

        return when (options.chunkStrategy) {
            ChunkStrategy.NONE -> listOf(merged)
            ChunkStrategy.BY_SIZE -> chunkBySize(merged, options.chunkSize, true)
            ChunkStrategy.BY_METHODS -> chunkByMethods(merged)
        }
    }

    private fun formatSingleSnippet(snippet: Snippet, o: ClipCraftOptions): String {
        // If snippet.language is null or blank, fallback to naive guess
        val lang = if (!snippet.language.isNullOrBlank()) {
            snippet.language
        } else {
            guessLang(snippet.fileName)
        }

        var content = snippet.content
        if (o.removeImports) content = removeImports(content, lang)
        if (o.removeComments) content = removeComments(content, lang)
        if (o.trimWhitespace) content = trimWhitespace(content, o.collapseBlankLines, o.removeLeadingBlankLines)
        if (o.removeEmptyLines) content = collapseConsecutiveBlankLines(content)
        if (o.singleLineOutput) content = singleLineOutput(content)
        content = applyCompression(content, o)

        if (o.includeLineNumbers) {
            content = addLineNumbers(content)
        }

        val meta = if (o.includeMetadata) formatMetadata(snippet, content, o) else content
        return wrap(meta, o.outputFormat, lang)
    }

    private fun wrap(content: String, format: OutputFormat, lang: String?): String {
        val l = lang ?: "none"
        return when (format) {
            OutputFormat.MARKDOWN -> "```$l\n$content\n```"
            OutputFormat.HTML -> "<pre><code class=\"$l\">${StringEscapeUtils.escapeHtml4(content)}</code></pre>"
            OutputFormat.PLAIN -> content
        }
    }

    private fun formatMetadata(snippet: Snippet, content: String, o: ClipCraftOptions): String {
        return buildString {
            append("**File:** ${snippet.relativePath ?: snippet.fileName} | **Size:** ${snippet.fileSizeBytes} bytes | **Modified:** ${snippet.lastModified}\n\n")
            append(content)
        }
    }

    // Simple extension-based fallback if PSI detection was null:
    fun guessLang(filename: String?): String {
        val ext = filename?.substringAfterLast('.', "")?.lowercase() ?: return "none"
        return when (ext) {
            "java" -> "java"
            "kt", "kts" -> "kotlin"
            "js" -> "javascript"
            "ts" -> "typescript"
            "py" -> "python"
            "cpp", "cxx", "cc" -> "cpp"
            "cs" -> "csharp"
            "html" -> "html"
            "css" -> "css"
            else -> "none"
        }
    }

    fun removeImports(text: String, lang: String?): String {
        return when (lang?.lowercase()) {
            "python" -> text.lines().filterNot {
                val trimmed = it.trim()
                trimmed.startsWith("import ") || trimmed.startsWith("from ")
            }.joinToString("\n")

            else -> text.lines().filterNot {
                it.trim().lowercase().startsWith("import ")
            }.joinToString("\n")
        }
    }

    fun removeComments(text: String, lang: String?): String {
        return if (lang?.contains("python", ignoreCase = true) == true) {
            text.lines().filterNot { it.trim().startsWith("#") }.joinToString("\n")
        } else {
            val noBlock = text.replace(Regex("(?s)/\\*.*?\\*/"), "")
            noBlock.lines().map { it.replace(Regex("//.*$"), "").trimEnd() }
                .joinToString("\n")
        }
    }

    fun trimWhitespace(text: String, collapse: Boolean, removeLeading: Boolean): String {
        val lines = text.lines().map { it.replace("\u200B", "").trim() }
        val trimmed = if (removeLeading) lines.dropWhile { it.isEmpty() } else lines
        return if (collapse) collapseConsecutiveBlankLines(trimmed.joinToString("\n")) else trimmed.joinToString("\n")
    }

    fun collapseConsecutiveBlankLines(text: String): String =
        text.replace(Regex("(\\n\\s*){2,}"), "\n\n").trim()

    fun singleLineOutput(text: String): String = text.replace(Regex("\\s+"), " ").trim()

    fun applyCompression(input: String, o: ClipCraftOptions): String {
        return when (o.compressionMode) {
            CompressionMode.NONE -> input
            CompressionMode.MINIMAL -> input.lines().joinToString("\n") {
                it.replace("\u200B", " ").replace(Regex("\\s+"), " ")
            }

            CompressionMode.ULTRA -> input.lines().map { line ->
                line.replace("\uFEFF", "")
                    .replace("\u200B", "")
                    .replace(Regex("\\p{C}+"), "")
                    .trim()
            }.filter {
                if (!o.selectiveCompression) {
                    it.isNotBlank()
                } else {
                    it.isNotBlank() && !it.uppercase().contains("TODO") && !it.uppercase().contains("DEBUG")
                }
            }.joinToString(" ") { it.replace(Regex("\\s+"), " ") }
        }
    }

    fun addLineNumbers(text: String): String =
        text.lines().mapIndexed { i, line -> "${i + 1}: $line" }.joinToString("\n")

    fun chunkBySize(text: String, maxChunkSize: Int, preserveWords: Boolean): List<String> {
        if (maxChunkSize <= 0) {
            throw IllegalArgumentException("maxChunkSize must be positive")
        }
        if (text.length <= maxChunkSize) return listOf(text)
        return if (!preserveWords) {
            val chunks = mutableListOf<String>()
            var index = 0
            while (index < text.length) {
                val end = min(index + maxChunkSize, text.length)
                chunks.add(text.substring(index, end))
                index = end
            }
            chunks
        } else {
            val result = mutableListOf<String>()
            var idx = 0
            while (idx < text.length) {
                var end = min(idx + maxChunkSize, text.length)
                if (end < text.length) {
                    val lastSpace = text.lastIndexOf(' ', end - 1)
                    if (lastSpace >= idx) end = lastSpace + 1
                }
                result.add(text.substring(idx, end))
                idx = end
            }
            result
        }
    }

    fun chunkByMethods(text: String): List<String> {
        val parts = text.split(Regex("(?m)(?=^\\s*(fun |public |private |def |class ))"))
        return if (parts.size <= 1) listOf(text) else parts.filter { it.isNotBlank() }
    }
}
