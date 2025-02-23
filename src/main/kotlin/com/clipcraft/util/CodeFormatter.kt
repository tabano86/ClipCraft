package com.clipcraft.util

import com.clipcraft.model.ChunkStrategy
import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.CompressionMode
import com.clipcraft.model.OutputFormat
import com.clipcraft.model.Snippet
import kotlin.math.min
import org.apache.commons.text.StringEscapeUtils

object CodeFormatter {

    fun formatSnippets(snippets: List<Snippet>, options: ClipCraftOptions): List<String> {
        options.resolveConflicts()
        val merged = snippets.joinToString("\n\n") { process(it, options) }.trim()
        return when (options.chunkStrategy) {
            ChunkStrategy.NONE -> listOf(merged)
            ChunkStrategy.BY_SIZE -> chunkBySize(merged, options.chunkSize, true)
            ChunkStrategy.BY_METHODS -> chunkByMethods(merged)
        }
    }

    fun process(snippet: Snippet, o: ClipCraftOptions): String {
        val lang = snippet.language?.ifBlank { null } ?: detectLanguage(snippet.fileName)
        var text = snippet.content
        if (o.removeImports) text = removeImports(text, lang)
        if (o.removeComments) text = removeComments(text, lang)
        if (o.trimWhitespace) text = trimWhitespace(text, o.collapseBlankLines, o.removeLeadingBlankLines)
        if (o.removeEmptyLines) text = collapseEmpty(text)
        if (o.singleLineOutput) text = singleLine(text)
        text = compress(text, o)
        if (o.includeLineNumbers) text = lineNumbers(text)
        val meta = if (o.includeMetadata) metadata(snippet, text) else text
        return wrap(meta, o.outputFormat, lang)
    }

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

    fun removeImports(text: String, lang: String?): String {
        val lines = text.lines()
        return when {
            lang?.contains("python", true) == true ->
                lines.filterNot {
                    val t = it.trim().lowercase()
                    t.startsWith("import ") || t.startsWith("from ")
                }
            lang?.contains("ruby", true) == true ->
                lines.filterNot {
                    val t = it.trim().lowercase()
                    t.startsWith("require ") || t.startsWith("load ")
                }
            lang?.contains("php", true) == true ->
                lines.filterNot {
                    val t = it.trim().lowercase()
                    t.startsWith("use ") || t.startsWith("require ")
                }
            else ->
                lines.filterNot {
                    val t = it.trim().lowercase()
                    t.startsWith("import ") || t.startsWith("#include ") || t.startsWith("using ")
                }
        }.joinToString("\n")
    }

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

            else -> // For Java and other C-like languages
                text.replace(Regex("(?s)/\\*.*?\\*/"), "")
                    .lines()
                    .map { it.replace(Regex("//.*$"), "").trimEnd() }
                    .filter { it.isNotBlank() } // Remove lines fully empty after stripping comments
                    .joinToString("\n")
        }
    }

    fun trimWhitespace(value: String, collapse: Boolean, removeLeading: Boolean): String {
        val lines = value.lines().map { it.replace("\u200B", "").trim() }
        val trimmed = if (removeLeading) lines.dropWhile { it.isEmpty() } else lines
        return if (collapse) collapseEmpty(trimmed.joinToString("\n")) else trimmed.joinToString("\n")
    }

    fun collapseEmpty(text: String): String =
        text.replace(Regex("(\\n\\s*){2,}"), "\n\n").trim()

    fun singleLine(text: String): String =
        text.replace(Regex("\\s+"), " ").trim()

    fun compress(input: String, o: ClipCraftOptions): String {
        return when (o.compressionMode) {
            CompressionMode.NONE -> input
            CompressionMode.MINIMAL -> input.lines().joinToString("\n") {
                it.replace("\u200B", " ").replace(Regex("\\s+"), " ")
            }
            CompressionMode.ULTRA -> input.lines().map {
                it.replace("\uFEFF", "").replace("\u200B", "").replace(Regex("\\p{C}+"), "").trim()
            }.filter {
                if (!o.selectiveCompression) {
                    it.isNotBlank()
                } else {
                    it.isNotBlank() && !it.uppercase().contains("TODO") && !it.uppercase().contains("DEBUG")
                }
            }.joinToString(" ") { it.replace(Regex("\\s+"), " ") }
        }
    }

    fun lineNumbers(text: String): String =
        text.lines().mapIndexed { i, line -> "${i + 1}: $line" }.joinToString("\n")

    fun metadata(snippet: Snippet, content: String): String =
        "**File:** ${snippet.relativePath ?: snippet.fileName} | **Size:** ${snippet.fileSizeBytes} bytes | **Modified:** ${snippet.lastModified}\n\n$content"

    fun wrap(content: String, format: OutputFormat, lang: String?): String {
        val l = lang ?: "none"
        return when (format) {
            OutputFormat.MARKDOWN -> "```$l\n$content\n```"
            OutputFormat.HTML -> "<pre><code class=\"$l\">${StringEscapeUtils.escapeHtml4(content)}</code></pre>"
            OutputFormat.PLAIN -> content
        }
    }

    fun chunkBySize(text: String, maxChunkSize: Int, preserveWords: Boolean): List<String> {
        require(maxChunkSize > 0) { "maxChunkSize must be positive" }
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

    fun chunkByMethods(text: String): List<String> {
        val pattern = Regex("(?m)(?=^\\s*(fun\\s|public\\s|private\\s|def\\s|class\\s|function\\s|override\\s))")
        val parts = text.split(pattern)
        if (parts.size < 2) return listOf(text)
        return parts.filter { it.isNotBlank() }
    }
}
