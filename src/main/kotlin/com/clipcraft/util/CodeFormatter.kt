package com.clipcraft.util

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.CompressionMode
import com.clipcraft.model.OutputFormat
import com.clipcraft.model.Snippet
import kotlin.math.min
import org.apache.commons.lang3.StringEscapeUtils

object CodeFormatter {
    fun formatSnippets(snippets: List<Snippet>, options: ClipCraftOptions): List<String> {
        options.resolveConflicts()
        val merged = snippets.joinToString("\n\n") { formatSingleSnippet(it, options) }.trim()
        return when (options.chunkStrategy) {
            com.clipcraft.model.ChunkStrategy.NONE -> listOf(merged)
            com.clipcraft.model.ChunkStrategy.BY_SIZE -> chunkBySize(merged, options.chunkSize, true)
            com.clipcraft.model.ChunkStrategy.BY_METHODS -> chunkByMethods(merged)
        }
    }

    fun formatSingleSnippet(snippet: Snippet, o: ClipCraftOptions): String {
        val lang = if (o.autoDetectLanguage && snippet.language.isNullOrBlank()) guessLang(snippet.fileName) else snippet.language
        var content = snippet.content
        if (o.removeImports) content = removeImports(content, lang)
        if (o.removeComments) content = removeComments(content, lang)
        if (o.trimWhitespace) content = trimWhitespace(content, o.collapseBlankLines, o.removeLeadingBlankLines)
        if (o.removeEmptyLines) content = collapseConsecutiveBlankLines(content)
        if (o.singleLineOutput) content = singleLineOutput(content)
        content = applyCompression(content, o)
        if (o.includeLineNumbers) content = addLineNumbers(content)
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
        return StringBuilder().apply {
            append("**File:** ${snippet.relativePath ?: snippet.fileName} | **Size:** ${snippet.fileSizeBytes} bytes | **Modified:** ${snippet.lastModified}\n\n")
            append(content)
        }.toString()
    }

    fun guessLang(filename: String?): String {
        val ext = filename?.substringAfterLast('.', "")?.lowercase() ?: return "java"
        return when (ext) {
            "java" -> "java"
            "kt", "kts" -> "kotlin"
            "js" -> "javascript"
            "ts" -> "typescript"
            "py" -> "python"
            "cpp", "cxx", "cc" -> "cpp"
            else -> "java"
        }
    }

    fun removeImports(text: String, lang: String?): String {
        return if (lang?.lowercase() == "python") {
            text.lineSequence().filterNot {
                val t = it.trimStart()
                t.startsWith("import ") || t.startsWith("from ")
            }.joinToString("\n")
        } else {
            text.lineSequence().filterNot {
                it.trimStart().startsWith("import ")
            }.joinToString("\n")
        }
    }

    fun removeComments(text: String, lang: String?): String {
        return if (lang?.lowercase()?.contains("python") == true)
            text.lineSequence().filterNot { it.trimStart().startsWith("#") }.joinToString("\n")
        else {
            val noBlock = text.replace(Regex("(?s)/\\*.*?\\*/"), "")
            noBlock.lineSequence().map { it.replace(Regex("//.*$"), "").trimEnd() }
                .filter { it.isNotBlank() }
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
            CompressionMode.MINIMAL -> input.lineSequence().joinToString("\n") {
                it.replace("\u200B", " ").replace(Regex("\\s+"), " ")
            }
            CompressionMode.ULTRA -> input.lineSequence().map { line ->
                line.replace("\uFEFF", "").replace("\u200B", "").replace(Regex("\\p{C}+"), "").trim()
            }.filter {
                if (!o.selectiveCompression) it.isNotBlank() else it.isNotBlank() && !it.uppercase().contains("TODO") && !it.uppercase().contains("DEBUG")
            }.joinToString(" ") { it.replace(Regex("\\s+"), " ") }
        }
    }

    fun addLineNumbers(text: String): String =
        text.lines().mapIndexed { i, line -> "${i + 1}: $line" }.joinToString("\n")

    fun chunkBySize(text: String, maxChunkSize: Int, preserveWords: Boolean): List<String> {
        require(maxChunkSize > 0) { "maxChunkSize must be positive" }
        if (text.length <= maxChunkSize) return listOf(text)
        if (!preserveWords) {
            val chunks = mutableListOf<String>()
            var index = 0
            while (index < text.length) {
                val end = min(index + maxChunkSize, text.length)
                chunks.add(text.substring(index, end))
                index = end
            }
            return chunks
        }
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
        return result
    }

    fun chunkByMethods(text: String): List<String> {
        val parts = text.split(Regex("(?m)(?=^\\s*(fun |public |private |def ))"))
        return if (parts.size <= 1) listOf(text) else parts.filter { it.isNotBlank() }
    }
}
