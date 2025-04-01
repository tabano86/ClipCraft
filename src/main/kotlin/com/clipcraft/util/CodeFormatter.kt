package com.clipcraft.util

import com.clipcraft.model.ChunkStrategy
import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.CompressionMode
import com.clipcraft.model.OutputFormat
import com.clipcraft.model.Snippet
import org.apache.commons.text.StringEscapeUtils
import kotlin.math.min

object CodeFormatter {

    /**
     * Formats a list of snippets into a single string.
     * Each snippet is processed individually and combined with a blank line separator.
     * For Markdown (or HTML) output, each snippet is wrapped in appropriate code fences.
     */
    fun formatSnippets(snippets: List<Snippet>, options: ClipCraftOptions): String {
        return snippets.joinToString(separator = "\n\n") { snippet ->
            processSnippet(snippet, options)
        }
    }

    /**
     * Processes a single snippet.
     *
     * The output always includes:
     *  - A metadata block (always inserted using MetadataFormatter).
     *  - A snippet header: if the user hasn’t provided one, a default is computed using the snippet’s fileName and filePath.
     *  - The transformed snippet content (split by methods or size if requested, with line numbers if enabled), wrapped in a code block.
     *  - A snippet footer if provided.
     */
    private fun processSnippet(snippet: Snippet, options: ClipCraftOptions): String {
        val sb = StringBuilder()

        // Always include metadata.
        val metaTemplate = if (options.metadataTemplate.isNullOrBlank()) {
            "File: {fileName} | Path: {filePath} | Size: {size} bytes"
        } else {
            options.metadataTemplate!!
        }
        val meta = MetadataFormatter.formatMetadata(metaTemplate, snippet)
        sb.appendLine(meta)
        sb.appendLine() // Separate metadata from code.

        // Determine header: use user-defined header if present; otherwise compute a default header.
        val header = if (options.snippetHeaderText.isNullOrBlank()) {
            "File: ${snippet.fileName} | Path: ${snippet.filePath}"
        } else {
            options.snippetHeaderText!!
        }
        sb.appendLine(header)

        // Transform snippet content.
        var content = snippet.content
        content = transformSnippetContent(content, options, snippet)

        // Detect language and wrap content.
        val language = snippet.language?.takeIf { it.isNotBlank() } ?: detectLanguage(snippet.fileName)
        val wrappedContent = wrapCodeBlock(content, options.outputFormat, language)
        sb.appendLine(wrappedContent)

        // Append footer if provided.
        if (!options.snippetFooterText.isNullOrBlank()) {
            sb.appendLine(options.snippetFooterText)
        }
        return sb.toString().trimEnd()
    }

    /**
     * Transforms snippet content based on options.
     * It removes imports/comments, trims whitespace, and if a chunk strategy is defined,
     * splits the content accordingly.
     * Then, if line numbering is enabled, it adds line numbers.
     */
    private fun transformSnippetContent(text: String, options: ClipCraftOptions, snippet: Snippet): String {
        var result = text
        val lang = snippet.language?.takeIf { it.isNotBlank() } ?: detectLanguage(snippet.fileName)

        if (options.removeImports) result = removeImports(result, lang)
        if (options.removeComments) result = removeComments(result, lang)
        if (options.trimWhitespace) {
            result =
                trimWhitespace(result, options.collapseBlankLines, options.removeLeadingBlankLines)
        }
        if (options.removeEmptyLines) result = collapseEmpty(result)
        if (options.singleLineOutput) result = singleLine(result)
        result = compress(result, options)

        // Apply chunking strategy if set.
        result = when (options.chunkStrategy) {
            ChunkStrategy.BY_METHODS -> {
                val chunks = chunkByMethods(result)
                chunks.joinToString(separator = "\n\n---\n\n")
            }

            ChunkStrategy.BY_SIZE -> {
                val chunks = chunkBySize(result, options.chunkSize, preserveWords = true)
                chunks.joinToString(separator = "\n\n---\n\n")
            }

            else -> result
        }

        if (options.includeLineNumbers) result = lineNumbers(result)
        return result
    }

    /**
     * Detects the programming language from the file extension.
     */
    fun detectLanguage(fileName: String?): String {
        val ext = fileName?.substringAfterLast('.', "")?.lowercase().orEmpty()
        return when (ext) {
            "java" -> "java"
            "kt", "kts" -> "kotlin"
            "js" -> "javascript"
            "ts" -> "typescript"
            "py" -> "python"
            "cpp", "cxx", "cc" -> "cpp"
            "cs" -> "csharp"
            "rb" -> "ruby"
            "php" -> "php"
            "html" -> "html"
            "css" -> "css"
            "swift" -> "swift"
            "go" -> "go"
            "rs" -> "rust"
            else -> "none"
        }
    }

    /**
     * Removes import statements based on language.
     */
    fun removeImports(text: String, lang: String?): String {
        val lines = text.lines()
        val filtered = when {
            lang?.contains("python", true) == true ->
                lines.filterNot {
                    it.trim().lowercase().startsWith("import ") || it.trim().lowercase().startsWith("from ")
                }

            lang?.contains("ruby", true) == true ->
                lines.filterNot {
                    it.trim().lowercase().startsWith("require ") || it.trim().lowercase().startsWith("load ")
                }

            lang?.contains("php", true) == true ->
                lines.filterNot {
                    it.trim().lowercase().startsWith("use ") || it.trim().lowercase().startsWith("require ")
                }

            else ->
                lines.filterNot {
                    val t = it.trim().lowercase()
                    t.startsWith("import ") || t.startsWith("#include ") || t.startsWith("using ")
                }
        }
        return filtered.joinToString("\n")
    }

    /**
     * Removes comments based on language.
     */
    fun removeComments(text: String, lang: String?): String {
        return when {
            lang?.contains("python", true) == true || lang.equals("ruby", true) ->
                text.lines().filterNot { it.trim().startsWith("#") }.joinToString("\n")

            lang.equals("php", true) ->
                text.replace(Regex("(?s)/\\*.*?\\*/"), "")
                    .lines()
                    .map { it.replace(Regex("//.*$"), "").replace(Regex("#.*$"), "").trimEnd() }
                    .filter { it.isNotBlank() }
                    .joinToString("\n")

            else ->
                text.replace(Regex("(?s)/\\*.*?\\*/"), "")
                    .lines()
                    .map { it.replace(Regex("//.*$"), "").trimEnd() }
                    .filter { it.isNotBlank() }
                    .joinToString("\n")
        }
    }

    /**
     * Trims whitespace from each line.
     */
    fun trimWhitespace(value: String, collapse: Boolean, removeLeading: Boolean): String {
        val lines = value.lines().map { it.replace("\u200B", "").trim() }
        val trimmed = if (removeLeading) lines.dropWhile { it.isEmpty() } else lines
        return if (collapse) collapseEmpty(trimmed.joinToString("\n")) else trimmed.joinToString("\n")
    }

    /**
     * Collapses consecutive empty lines.
     */
    fun collapseEmpty(text: String): String {
        return text.replace(Regex("(\\n\\s*){2,}"), "\n\n").trim()
    }

    /**
     * Converts multiline text into a single line.
     */
    fun singleLine(text: String): String {
        return text.replace(Regex("\\s+"), " ").trim()
    }

    /**
     * Compresses text based on the chosen CompressionMode.
     */
    fun compress(input: String, o: ClipCraftOptions): String {
        return when (o.compressionMode) {
            CompressionMode.NONE -> input
            CompressionMode.MINIMAL ->
                input.lines().joinToString("\n") {
                    it.replace("\u200B", " ").replace(Regex("\\s+"), " ")
                }

            CompressionMode.ULTRA ->
                input.lines()
                    .map {
                        it.replace("\uFEFF", "")
                            .replace("\u200B", "")
                            .replace(Regex("\\p{C}+"), "")
                            .trim()
                    }
                    .filter {
                        if (!o.selectiveCompression) {
                            it.isNotBlank()
                        } else {
                            it.isNotBlank() && !it.uppercase().contains("TODO") && !it.uppercase().contains("DEBUG")
                        }
                    }
                    .joinToString(" ") { ln -> ln.replace(Regex("\\s+"), " ") }
        }
    }

    /**
     * Adds line numbers to each line.
     */
    fun lineNumbers(text: String): String {
        return text.lines().mapIndexed { i, line -> "${i + 1}: $line" }.joinToString("\n")
    }

    /**
     * Wraps content in code fences for Markdown, <pre>/<code> for HTML, or leaves it plain.
     */
    private fun wrapCodeBlock(content: String, format: OutputFormat, lang: String?): String {
        val language = lang ?: "none"
        return when (format) {
            OutputFormat.MARKDOWN -> "```$language\n$content\n```"
            OutputFormat.HTML -> "<pre><code class=\"$language\">${StringEscapeUtils.escapeHtml4(content)}</code></pre>"
            OutputFormat.PLAIN -> content
        }
    }

    /**
     * Splits text into chunks of at most maxChunkSize characters.
     */
    fun chunkBySize(text: String, maxChunkSize: Int, preserveWords: Boolean): List<String> {
        require(maxChunkSize > 0)
        if (text.length <= maxChunkSize) return listOf(text)
        if (!preserveWords) {
            val chunks = mutableListOf<String>()
            var idx = 0
            while (idx < text.length) {
                val end = min(idx + maxChunkSize, text.length)
                chunks += text.substring(idx, end)
                idx = end
            }
            return chunks
        }
        val results = mutableListOf<String>()
        var i = 0
        while (i < text.length) {
            var end = min(i + maxChunkSize, text.length)
            if (end < text.length) {
                val space = text.lastIndexOf(' ', end - 1)
                if (space >= i) end = space + 1
            }
            results += text.substring(i, end)
            i = end
        }
        return results
    }

    /**
     * Splits text by top-level method or function definitions.
     */
    fun chunkByMethods(text: String): List<String> {
        val pattern = Regex("(?m)(?=^\\s*(fun\\s|public\\s|private\\s|def\\s|class\\s|function\\s|override\\s))")
        val parts = text.split(pattern)
        return if (parts.size < 2) listOf(text) else parts.filter { it.isNotBlank() }
    }

    // Default snippet header when user leaves header blank.
    private const val DEFAULT_SNIPPET_HEADER = "File: {fileName} | Path: {filePath}"
}
