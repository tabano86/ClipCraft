package com.clipcraft.util

import com.clipcraft.model.*
import matchesGlob
import org.apache.commons.lang3.StringEscapeUtils
import java.io.File
import kotlin.math.min

/**
 * A unified, advanced code formatter that applies configurable cleanup,
 * chunking, compression, and metadata injection.
 */
object CodeFormatter {

    fun formatSnippets(snippets: List<Snippet>, options: ClipCraftOptions): List<String> {
        options.resolveConflicts()

        val combined = snippets.joinToString("\n\n") { snippet ->
            formatSingleSnippet(snippet, options)
        }

        val processed = combined.trim()

        return when (options.chunkStrategy) {
            ChunkStrategy.NONE       -> listOf(processed)
            ChunkStrategy.BY_SIZE    -> chunkBySize(processed, options.chunkSize, preserveWords = true)
            ChunkStrategy.BY_METHODS -> chunkByMethods(processed)
        }
    }

    private fun formatSingleSnippet(snippet: Snippet, options: ClipCraftOptions): String {
        val language = if (options.autoDetectLanguage && snippet.language.isNullOrBlank()) {
            guessLanguageByExtension(snippet.fileName) ?: "java"
        } else {
            snippet.language ?: "java"
        }

        var content = snippet.content

        if (options.removeImports) content = removeImports(content, language)
        if (options.removeComments) content = removeComments(content, language)
        if (options.trimWhitespace) {
            content = trimWhitespace(content, options.collapseBlankLines, options.removeLeadingBlankLines)
        }
        if (options.removeEmptyLines) content = collapseConsecutiveBlankLines(content)
        if (options.singleLineOutput) content = singleLineOutput(content)

        content = applyCompression(content, options)
        if (options.includeLineNumbers) content = addLineNumbers(content)

        val finalText = if (options.includeMetadata) {
            val metadata = buildString {
                append("**File:** ${snippet.relativePath ?: snippet.fileName}")
                append(" | **Size:** ${snippet.fileSizeBytes} bytes")
                append(" | **Modified:** ${snippet.lastModified}")
                if (options.includeGitInfo && !snippet.gitCommitHash.isNullOrBlank()) {
                    append(" | **GitCommit:** ${snippet.gitCommitHash}")
                }
            }
            "$metadata\n\n$content"
        } else {
            content
        }

        return wrapOutputForFormat(finalText, options.outputFormat, language)
    }

    private fun guessLanguageByExtension(fileName: String): String? {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "java"            -> "java"
            "kt", "kts"       -> "kotlin"
            "js"              -> "javascript"
            "ts"              -> "typescript"
            "py"              -> "python"
            "cpp", "cxx", "cc"-> "cpp"
            else              -> null
        }
    }

    fun removeImports(text: String, language: String): String {
        return when (language.lowercase()) {
            "python" -> text.lineSequence()
                .filterNot { it.trimStart().startsWith("import ") || it.trimStart().startsWith("from ") }
                .joinToString("\n")
            else -> text.lineSequence()
                .filterNot { it.trimStart().startsWith("import ") || it.trimStart().startsWith("#include ") }
                .joinToString("\n")
        }
    }

    fun removeComments(text: String, language: String): String {
        return when (language.lowercase()) {
            "python" -> text.lineSequence()
                .filterNot { it.trimStart().startsWith("#") }
                .joinToString("\n")

            else -> {
                // Remove block comments first
                val withoutBlock = text.replace(Regex("(?s)/\\*.*?\\*/"), "")
                // Remove line comments; skip any now-empty lines
                val lines = withoutBlock.lineSequence()
                    .map { line ->
                        // remove //... comment text
                        val noLineComment = line.replace(Regex("//.*$"), "")
                        // trim trailing spaces
                        noLineComment.trimEnd()
                    }
                    // discard lines if they’re now blank
                    .filterNot { it.isBlank() }
                    .toList()

                lines.joinToString("\n")
            }
        }
    }


    fun trimWhitespace(
        text: String,
        collapseBlankLines: Boolean,
        removeLeadingBlankLines: Boolean
    ): String {
        val trimmedLines = text.lines().map { it.replace("\u200B", "").trim() }
        val processedLines = if (removeLeadingBlankLines) {
            trimmedLines.dropWhile { it.isEmpty() }
        } else {
            trimmedLines
        }
        return if (collapseBlankLines)
            collapseConsecutiveBlankLines(processedLines.joinToString("\n"))
        else
            processedLines.joinToString("\n")
    }

    fun collapseConsecutiveBlankLines(text: String): String {
        return text.replace(Regex("(\\n\\s*){2,}"), "\n\n").trim()
    }

    fun singleLineOutput(text: String): String {
        return text.replace(Regex("\\s+"), " ").trim()
    }

    fun applyCompression(input: String, options: ClipCraftOptions): String {
        return when (options.compressionMode) {
            CompressionMode.NONE -> input

            CompressionMode.MINIMAL -> input.lineSequence().joinToString("\n") { line ->
                line.replace("\u200B", " ")
                    .replace(Regex("\\s+"), " ")
            }

            CompressionMode.ULTRA -> {
                println("applyCompression(ULTRA) – selective=${options.selectiveCompression}")
                input.lineSequence()
                    .map { line ->
                        // Also print raw line
                        println("Raw line => '$line'")
                        val sanitized = line.replace("\uFEFF", "")
                            .replace("\u200B", "")
                            .replace(Regex("\\p{C}+"), "")
                            .trim()
                        println("Sanitized => '$sanitized' (uppercase='${sanitized.uppercase()}')")
                        sanitized
                    }
                    .filter { cleanLine ->
                        val upper = cleanLine.uppercase()
                        val toKeep = if (options.selectiveCompression) {
                            !upper.contains("TODO") && !upper.contains("DEBUG") && cleanLine.isNotBlank()
                        } else {
                            cleanLine.isNotBlank()
                        }
                        println("Filter => '$cleanLine' => toKeep=$toKeep")
                        toKeep
                    }
                    .joinToString(" ") { cleanLine ->
                        cleanLine.replace(Regex("\\s+"), " ")
                    }
            }


        }
    }


    fun addLineNumbers(text: String): String {
        return text.lines().mapIndexed { index, line -> "${index + 1}: $line" }
            .joinToString("\n")
    }

    fun wrapOutputForFormat(snippetText: String, format: OutputFormat, language: String?): String {
        return when (format) {
            OutputFormat.MARKDOWN -> {
                val lang = language ?: ""
                "```$lang\n$snippetText\n```"
            }
            OutputFormat.HTML -> {
                val langClass = language?.let { " class=\"$it\"" } ?: ""
                "<pre><code$langClass>${StringEscapeUtils.escapeHtml4(snippetText)}</code></pre>"
            }
            OutputFormat.PLAIN -> snippetText
        }
    }

    fun chunkBySize(text: String, maxChunkSize: Int, preserveWords: Boolean = false): List<String> {
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
        val chunks = mutableListOf<String>()
        var index = 0
        while (index < text.length) {
            var end = min(index + maxChunkSize, text.length)
            if (end < text.length) {
                val lastDelimiter = text.lastIndexOfAny(charArrayOf(' ', '\n', '\t'), end)
                if (lastDelimiter > index) {
                    end = lastDelimiter
                }
            }
            chunks.add(text.substring(index, end))
            index = end
            while (index < text.length && text[index].isWhitespace()) index++
        }
        return chunks
    }

    fun chunkByMethods(text: String): List<String> {
        val pattern = Regex("(?m)(?=^\\s*(fun |public |private |def ))")
        val parts = text.split(pattern)
        return if (parts.size <= 1) listOf(text) else parts.filter { it.isNotBlank() }
    }

    fun filterFilesByGitIgnore(fileList: List<String>, gitIgnoreFile: File): List<String> {
        if (!gitIgnoreFile.exists() || !gitIgnoreFile.isFile) return fileList

        val patterns = gitIgnoreFile.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }

        return fileList.filter { file ->
            val normalized = file.replace('\\', '/')
            patterns.none { pattern -> matchesGlob(pattern, normalized) }
        }
    }

    fun processContent(input: String, options: ClipCraftOptions, language: String): String {
        var result = input
        if (options.removeImports) result = removeImports(result, language)
        if (options.removeComments) result = removeComments(result, language)
        if (options.trimWhitespace) {
            result = trimWhitespace(
                result,
                options.collapseBlankLines,
                options.removeLeadingBlankLines
            )
        }
        return result.trim()
    }

    fun trimLineWhitespaceAdvanced(input: String): String {
        return input.lines().joinToString("\n") { line ->
            line.replace("\u200B", "").trimStart()
        }
    }

    fun minimizeWhitespace(input: String): String {
        return input.lines().joinToString("\n") { line ->
            line.replace("\u200B", "").trim()
        }.trimEnd()
    }

    fun formatBlock(code: String, language: String, format: OutputFormat): String {
        return when (format) {
            OutputFormat.MARKDOWN -> "```$language\n$code\n```"
            OutputFormat.HTML -> "<pre><code class=\"$language\">${StringEscapeUtils.escapeHtml4(code)}</code></pre>"
            OutputFormat.PLAIN -> code
        }
    }

    fun removeLeadingBlankLines(input: String): String {
        return input.lines().dropWhile { it.isBlank() }.joinToString("\n")
    }
}
