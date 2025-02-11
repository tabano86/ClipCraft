package com.clipcraft.util

import com.clipcraft.model.*
import org.apache.commons.lang.StringEscapeUtils
import kotlin.math.min

/**
 * A unified, advanced code formatter that applies user-defined cleanup,
 * chunking, compression, and metadata injection.
 */
object CodeFormatter {

    /**
     * Formats multiple snippets into a list of output strings (chunks)
     * according to the user’s [options]. This is the main entry point.
     */
    fun formatSnippets(snippets: List<Snippet>, options: ClipCraftOptions): List<String> {
        // Ensure internal consistency
        options.resolveConflicts()

        val combinedContent = buildString {
            snippets.forEach { snippet ->
                append(formatSingleSnippet(snippet, options))
                append("\n\n")
            }
        }

        return when (options.chunkStrategy) {
            ChunkStrategy.NONE -> listOf(combinedContent)
            ChunkStrategy.BY_SIZE -> chunkBySize(combinedContent, options.chunkSize)
            ChunkStrategy.BY_METHODS -> chunkByMethods(combinedContent)
        }
    }

    /**
     * Performs the full pipeline for a single snippet: remove imports, comments,
     * apply compression, and wrap with metadata if needed.
     */
    private fun formatSingleSnippet(snippet: Snippet, options: ClipCraftOptions): String {
        // If autoDetectLanguage is on and the snippet's language isn't set,
        // guess it from the file extension.
        if (options.autoDetectLanguage && (snippet.language.isNullOrEmpty())) {
            snippet.language = guessLanguageByExtension(snippet.fileName ?: "")
        }

        var content = snippet.content

        // Remove imports
        if (options.removeImports) {
            content = removeImports(content)
        }
        // Remove comments
        if (options.removeComments) {
            content = removeComments(content)
        }
        // Trim extra whitespace
        if (options.trimWhitespace) {
            content = trimWhitespace(content, options.collapseBlankLines, options.removeLeadingBlankLines)
        }
        // Possibly transform to single line
        if (options.singleLineOutput) {
            content = content.replace(Regex("\\s+"), " ")
        }
        // Apply compression
        content = applyCompression(content, options.compressionMode)

        // Add line numbers if requested
        if (options.includeLineNumbers) {
            content = addLineNumbers(content)
        }

        // If including metadata, prepend it
        val finalText = if (options.includeMetadata) {
            buildString {
                append("**File:** ${snippet.relativePath ?: snippet.fileName ?: "Unknown"}")
                append(" | **Size:** ${snippet.fileSizeBytes} bytes")
                append(" | **Modified:** ${snippet.lastModified}")
                if (options.includeGitInfo && snippet.gitCommitHash != null) {
                    append(" | **GitCommit:** ${snippet.gitCommitHash}")
                }
                append("\n\n")
                append(content)
            }
        } else {
            content
        }

        // Wrap in Markdown, HTML, or plain text
        return wrapOutputForFormat(finalText, options.outputFormat, snippet.language)
    }

    /**
     * Tries to guess language from a file extension in the simplest possible way.
     */
    private fun guessLanguageByExtension(fileName: String): String? {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "java" -> "java"
            "kt", "kts" -> "kotlin"
            "js" -> "javascript"
            "ts" -> "typescript"
            "py" -> "python"
            "cpp", "cxx", "cc" -> "cpp"
            else -> null
        }
    }

    private fun removeImports(text: String): String {
        // Basic approach for Java-like languages
        val importRegex = Regex("(?m)^(?:import\\s+.*|#include\\s+.*)$")
        return text.replace(importRegex, "")
    }

    private fun removeComments(text: String): String {
        var result = text
        // Single-line comments
        result = result.replace(Regex("(?m)//.*$"), "")
        // Multi-line comments
        result = result.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
        return result
    }

    private fun trimWhitespace(
        text: String,
        collapseBlankLines: Boolean,
        removeLeadingBlankLines: Boolean
    ): String {
        // Trim trailing spaces from each line
        var processed = text.lines().joinToString("\n") { it.trimEnd() }

        // Remove consecutive blank lines
        if (collapseBlankLines) {
            processed = processed.replace(Regex("(\\n){2,}"), "\n\n")
        }
        // Remove leading blank lines
        if (removeLeadingBlankLines) {
            processed = processed.replace(Regex("^\\s+"), "")
        }
        return processed
    }

    private fun applyCompression(text: String, mode: CompressionMode): String {
        return when (mode) {
            CompressionMode.NONE -> text
            CompressionMode.MINIMAL -> text.replace(Regex("\\n{3,}"), "\n\n")
            CompressionMode.ULTRA -> text.replace(Regex("\\s+"), " ")
        }
    }

    private fun addLineNumbers(content: String): String {
        val lines = content.lines()
        val width = lines.size.toString().length
        return lines.mapIndexed { index, line ->
            val lineNum = (index + 1).toString().padStart(width, ' ')
            "$lineNum: $line"
        }.joinToString("\n")
    }

    private fun wrapOutputForFormat(snippetText: String, format: OutputFormat, language: String?): String {
        return when (format) {
            OutputFormat.MARKDOWN -> {
                val lang = language ?: ""
                "```$lang\n$snippetText\n```"
            }
            OutputFormat.HTML -> {
                "<pre><code>${StringEscapeUtils.escapeHtml(snippetText)}</code></pre>"
            }
            else -> snippetText // PLAIN
        }
    }

    /**
     * Splits large text into chunks based on the [maxChunkSize].
     * This is the legacy approach if chunkStrategy == BY_SIZE.
     */
    private fun chunkBySize(text: String, maxChunkSize: Int): List<String> {
        if (maxChunkSize <= 0 || text.length <= maxChunkSize) return listOf(text)
        val chunks = mutableListOf<String>()
        var start = 0
        while (start < text.length) {
            val end = min(start + maxChunkSize, text.length)
            chunks.add(text.substring(start, end))
            start = end
        }
        return chunks
    }

    /**
     * Splits text by a naive detection of methods or functions.
     * This is an experimental approach if chunkStrategy == BY_METHODS.
     */
    private fun chunkByMethods(text: String): List<String> {
        // Very naive example: split on "fun " or lines that start with e.g. "public " "private " "def "
        // A real approach would parse AST or use a smarter regex.
        val pattern = Regex("(?m)(?=^\\s*(fun |public |private |def ))")
        val parts = text.split(pattern)
        return if (parts.size <= 1) listOf(text) else parts.filter { it.isNotBlank() }
    }
}
