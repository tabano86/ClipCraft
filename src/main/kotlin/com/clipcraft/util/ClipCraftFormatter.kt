package com.clipcraft.util

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.CompressionMode
import com.clipcraft.model.OutputFormat
import com.clipcraft.model.Snippet
import java.util.regex.Pattern

object ClipCraftFormatter {

    /**
     * Process and format a list of snippets according to the provided options.
     */
    fun formatSnippets(snippets: List<Snippet>, options: ClipCraftOptions): List<String> {
        // Combine formatted snippets
        val combinedContent = buildString {
            snippets.forEach { snippet ->
                append(formatSingleSnippet(snippet, options))
                append("\n\n")
            }
        }
        // If needed, break output into chunks
        return chunkIfNeeded(combinedContent, options.chunkSize)
    }

    private fun formatSingleSnippet(snippet: Snippet, options: ClipCraftOptions): String {
        var content = snippet.content

        // (a) Apply regex filters if provided
        options.customRegexFilters.forEach { regex ->
            content = regex.matcher(content).replaceAll("")
        }
        // (b) Remove import lines if enabled
        if (options.removeImports) {
            content = removeImports(content)
        }
        // (c) Remove comments if enabled
        if (options.removeComments) {
            content = removeComments(content)
        }
        // (d) Trim extra whitespace if enabled
        if (options.trimLineWhitespace) {
            content = trimWhitespace(content)
        }
        // (e) Apply compression mode
        content = applyCompression(content, options.compressionMode)
        // (f) Build final snippet text including metadata if requested
        return buildSnippetText(snippet, content, options)
    }

    private fun removeImports(content: String): String {
        // For Java-like languages: remove lines starting with "import ..."
        val importPattern = "(?m)^[ \\t]*import .*;[ \\t]*\n?"
        return content.replace(Regex(importPattern), "")
    }

    private fun removeComments(content: String): String {
        // Remove single-line comments
        val singleLine = Pattern.compile("(?m)^[ \\t]*//.*$")
        var result = singleLine.matcher(content).replaceAll("")
        // Remove multi-line comments
        val multiLine = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL)
        result = multiLine.matcher(result).replaceAll("")
        return result
    }

    private fun trimWhitespace(content: String): String {
        // Remove trailing spaces from each line and collapse multiple blank lines
        return content.lines()
            .map { it.trimEnd() }
            .joinToString("\n")
            .replace(Regex("(\\n){2,}"), "\n\n")
    }

    private fun applyCompression(content: String, mode: CompressionMode): String {
        return when (mode) {
            CompressionMode.NONE -> content
            CompressionMode.MINIMAL -> content.replace(Regex("\\n{3,}"), "\n\n")
            CompressionMode.ULTRA -> content.replace(Regex("\\s+"), " ")
        }
    }

    private fun buildSnippetText(snippet: Snippet, processed: String, options: ClipCraftOptions): String {
        val metadata = if (options.includeMetadata) {
            buildString {
                append("**File:** ${snippet.relativePath}")
                append(" | **Size:** ${snippet.fileSizeBytes} bytes")
                append(" | **Modified:** ${snippet.lastModified}")
                if (options.includeGitInfo && snippet.gitCommitHash != null) {
                    append(" | **Git Commit:** ${snippet.gitCommitHash}")
                }
                append("\n\n")
            }
        } else {
            ""
        }
        return when (options.outputFormat) {
            OutputFormat.MARKDOWN ->
                "$metadata```" + detectLanguage(snippet.filePath) + "\n$processed\n```\n"

            OutputFormat.HTML ->
                "$metadata<pre><code>${htmlEscape(processed)}</code></pre>"

            OutputFormat.PLAIN_TEXT ->
                "$metadata$processed"
        }
    }

    private fun detectLanguage(filePath: String): String {
        return when {
            filePath.endsWith(".kt", ignoreCase = true) -> "kotlin"
            filePath.endsWith(".java", ignoreCase = true) -> "java"
            filePath.endsWith(".py", ignoreCase = true) -> "python"
            filePath.endsWith(".ts", ignoreCase = true) -> "typescript"
            else -> ""
        }
    }

    private fun htmlEscape(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    private fun chunkIfNeeded(content: String, chunkSize: Int): List<String> {
        if (chunkSize <= 0 || content.length <= chunkSize) return listOf(content)
        val chunks = mutableListOf<String>()
        var start = 0
        while (start < content.length) {
            val end = (start + chunkSize).coerceAtMost(content.length)
            // For simplicity, we break exactly at chunkSize. More advanced logic can avoid breaking in the middle of a code block.
            chunks.add(content.substring(start, end))
            start = end
        }
        return chunks
    }
}
