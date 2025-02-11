package com.clipcraft.util

import com.clipcraft.model.*
import org.apache.commons.lang3.StringEscapeUtils
import kotlin.math.min

object CodeFormatter {
    fun formatSnippets(snippets: List<Snippet>, options: ClipCraftOptions): List<String> {
        options.resolveConflicts()
        val merged = snippets.joinToString("\n\n") { formatSingleSnippet(it, options) }.trim()
        return when (options.chunkStrategy) {
            ChunkStrategy.NONE -> listOf(merged)
            ChunkStrategy.BY_SIZE -> chunkBySize(merged, options.chunkSize, preserveWords = true)
            ChunkStrategy.BY_METHODS -> chunkByMethods(merged)
        }
    }

    /**
     * Updated to keep the trailing space if the chunk ends on whitespace,
     * so that chunk boundaries do not remove spaces between words.
     */
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
                if (lastSpace >= idx) {
                    // Include that space in the chunk
                    end = lastSpace + 1
                }
            }
            result.add(text.substring(idx, end))
            idx = end
        }
        return result
    }

    fun chunkByMethods(text: String): List<String> {
        val pattern = Regex("(?m)(?=^\\s*(fun |public |private |def ))")
        val parts = text.split(pattern)
        return if (parts.size <= 1) listOf(text) else parts.filter { it.isNotBlank() }
    }

    fun formatSingleSnippet(snippet: Snippet, o: ClipCraftOptions): String {
        val lang = if (o.autoDetectLanguage && snippet.language.isNullOrBlank()) guessLang(snippet.fileName) else snippet.language
        var content = snippet.content
        if (o.removeImports) content = removeImports(content, lang)
        if (o.removeComments) content = removeComments(content, lang)
        if (o.trimWhitespace) {
            content = trimWhitespace(
                content,
                collapse = o.collapseBlankLines,
                removeLeading = o.removeLeadingBlankLines
            )
        }
        if (o.removeEmptyLines) content = collapseConsecutiveBlankLines(content)
        if (o.singleLineOutput) content = singleLineOutput(content)
        content = applyCompression(content, o)
        if (o.includeLineNumbers) content = addLineNumbers(content)
        val final = formatMetadata(snippet, content, o)
        return wrap(final, o.outputFormat, lang)
    }

    fun guessLang(filename: String?): String {
        if (filename == null) return "java"
        val ext = filename.substringAfterLast('.', "").lowercase()
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
        if (lang == null) return text
        val l = lang.lowercase()
        return if (l.contains("python")) {
            text.lineSequence().filterNot {
                val trimmed = it.trimStart()
                trimmed.startsWith("import ") || trimmed.startsWith("from ")
            }.joinToString("\n")
        } else {
            text.lineSequence().filterNot {
                val trimmed = it.trimStart()
                trimmed.startsWith("import ") || trimmed.startsWith("#include ")
            }.joinToString("\n")
        }
    }

    fun removeComments(text: String, lang: String?): String {
        if (lang == null) return text
        val l = lang.lowercase()
        if (l.contains("python")) {
            return text.lineSequence().filterNot { it.trimStart().startsWith("#") }.joinToString("\n")
        }
        val noBlock = text.replace(Regex("(?s)/\\*.*?\\*/"), "")
        return noBlock.lineSequence()
            .map { it.replace(Regex("//.*$"), "").trimEnd() }
            .filterNot { it.isBlank() }
            .joinToString("\n")
    }

    fun trimWhitespace(text: String, collapse: Boolean, removeLeading: Boolean): String {
        val lines = text.lines().map { it.replace("\u200B", "").trim() }
        val final = if (removeLeading) lines.dropWhile { it.isEmpty() } else lines
        return if (collapse) collapseConsecutiveBlankLines(final.joinToString("\n")) else final.joinToString("\n")
    }

    fun collapseConsecutiveBlankLines(text: String): String {
        return text.replace(Regex("(\\n\\s*){2,}"), "\n\n").trim()
    }

    fun singleLineOutput(text: String): String {
        return text.replace(Regex("\\s+"), " ").trim()
    }

    fun applyCompression(input: String, o: ClipCraftOptions): String {
        return when (o.compressionMode) {
            CompressionMode.NONE -> input
            CompressionMode.MINIMAL -> input.lineSequence().joinToString("\n") {
                it.replace("\u200B", " ").replace(Regex("\\s+"), " ")
            }
            CompressionMode.ULTRA -> input.lineSequence().map { line ->
                line.replace("\uFEFF", "")
                    .replace("\u200B", "")
                    .replace(Regex("\\p{C}+"), "")
                    .trim()
            }.filter { line ->
                if (!o.selectiveCompression) line.isNotBlank()
                else {
                    val up = line.uppercase()
                    line.isNotBlank() && !up.contains("TODO") && !up.contains("DEBUG")
                }
            }.joinToString(" ") {
                it.replace(Regex("\\s+"), " ")
            }
        }
    }

    fun addLineNumbers(text: String): String {
        return text.lines().mapIndexed { i, line -> "${i + 1}: $line" }.joinToString("\n")
    }

    fun wrap(content: String, format: OutputFormat, lang: String?): String {
        val l = lang ?: "none"
        return when (format) {
            OutputFormat.MARKDOWN -> "```$l\n$content\n```"
            OutputFormat.HTML -> "<pre><code class=\"$l\">${StringEscapeUtils.escapeHtml4(content)}</code></pre>"
            OutputFormat.PLAIN -> content
        }
    }

    fun formatMetadata(snippet: Snippet, content: String, o: ClipCraftOptions): String {
        if (!o.includeMetadata) return content
        val b = StringBuilder()
        b.append("**File:** ").append(snippet.relativePath ?: snippet.fileName)
            .append(" | **Size:** ").append(snippet.fileSizeBytes).append(" bytes")
            .append(" | **Modified:** ").append(snippet.lastModified)
        if (o.includeGitInfo && !snippet.gitCommitHash.isNullOrEmpty()) {
            b.append(" | **GitCommit:** ").append(snippet.gitCommitHash)
        }
        b.append("\n\n").append(content)
        return b.toString()
    }
}
