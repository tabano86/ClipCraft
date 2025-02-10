package com.clipcraft.util

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.CompressionMode
import com.clipcraft.model.OutputFormat
import com.clipcraft.services.ClipCraftMacroManager
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Paths
import com.google.googlejavaformat.java.Formatter as GoogleJavaFormatter

object ClipCraftFormatter {
    private const val ZERO_WIDTH_SPACE = '\u200B'
    private val log = LoggerFactory.getLogger(ClipCraftFormatter::class.java)

    // Precompiled regex for block comments (for C-style languages)
    private val regexBlockComment = Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL)
    // Precompiled regex for horizontal whitespace (2+ occurrences)
    private val regexHorizontalWhitespace = Regex("[ \\t\u200B]{2,}")

    fun processContent(originalText: String, opts: ClipCraftOptions, language: String): String {
        val pipeline: List<(String) -> String> = listOf(
            ::removeLeadingBlankLines,
            { s -> if (opts.trimLineWhitespace) trimLineWhitespaceAdvanced(s) else s },
            { s -> if (opts.removeComments) removeComments(s, language) else s },
            { s -> if (opts.removeImports) removeImports(s, language) else s },
            { s ->
                when (language.lowercase()) {
                    "java" -> externalFormatJavaIfAvailable(s)
                    "javascript", "typescript" -> externalFormatJsIfAvailable(s, language)
                    else -> s
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

    fun trimLineWhitespaceAdvanced(text: String?): String =
        text?.lineSequence()
            ?.map { line ->
                // Remove both leading and trailing whitespace and also the zero-width space.
                line.trim { ch -> ch.isWhitespace() || ch == ZERO_WIDTH_SPACE }
            }
            ?.joinToString("\n") ?: ""

    fun minimizeWhitespace(input: String): String =
        input.lines()
            .map(String::trim)
            .joinToString("\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()

    private fun externalFormatJavaIfAvailable(text: String): String =
        runCatching { GoogleJavaFormatter().formatSource(text).trim() }
            .getOrElse {
                log.warn("Google Java Format failed; returning original text. Reason: ${it.message}")
                text.trim()
            }

    private fun externalFormatJsIfAvailable(text: String, language: String): String {
        log.debug("Pretending to run Prettier for $language; returning original text.")
        return text
    }

    fun addLineNumbers(text: String): String =
        text.lines().mapIndexed { idx, line -> "%4d: %s".format(idx + 1, line) }.joinToString("\n")

    fun collapseConsecutiveBlankLines(text: String): String =
        text.lines().fold(mutableListOf<String>()) { acc, line ->
            if (line.isBlank() && acc.lastOrNull()?.isBlank() == true) acc
            else acc.apply { add(line) }
        }.joinToString("\n").trimEnd()

    fun removeLeadingBlankLines(input: String): String =
        input.lineSequence().dropWhile { it.isBlank() }.joinToString("\n")

    fun removeComments(input: String, language: String): String = when (language.lowercase()) {
        "java", "kotlin", "javascript", "typescript", "csharp", "cpp" -> {
            // Remove block comments, then filter out lines starting with line comment markers.
            input.replace(regexBlockComment, "")
                .lineSequence()
                .filterNot { it.trim().startsWith("//") }
                .joinToString("\n")
        }
        "python", "ruby", "sh", "bash" ->
            input.lineSequence()
                .filterNot { it.trim().startsWith("#") }
                .joinToString("\n")
        else -> input
    }

    fun removeImports(input: String, language: String): String = when (language.lowercase()) {
        "java", "kotlin", "javascript", "typescript", "csharp", "cpp" ->
            input.replace(Regex("(?m)^import\\s+.*"), "")
        "python" ->
            input.replace(Regex("(?m)^(import\\s+.*|from\\s+.*)"), "")
        else -> input
    }

    fun applyCompression(input: String, opts: ClipCraftOptions): String =
        when (opts.compressionMode) {
            CompressionMode.NONE -> input
            CompressionMode.MINIMAL ->
                input.replace(regexHorizontalWhitespace, " ")
            CompressionMode.ULTRA -> {
                var result = input.replace(Regex("\n{3,}"), "\n\n")
                if (opts.selectiveCompression) {
                    result = result.lineSequence()
                        .filter { line ->
                            line.isNotBlank() &&
                                    !line.contains("TODO", ignoreCase = true) &&
                                    !line.contains("debug log", ignoreCase = true)
                        }
                        .joinToString("\n")
                }
                result.lineSequence()
                    .map { it.replace(regexHorizontalWhitespace, " ") }
                    .filter { it.isNotBlank() }
                    .joinToString("\n")
                    .trim()
            }
        }

    /**
     * Splits the given content into chunks of at most [maxChunkSize] characters.
     * When [preserveWords] is true the algorithm will try to break at a newline or space.
     *
     * @param content the content to split.
     * @param maxChunkSize maximum size (in characters) of each chunk.
     * @param preserveWords if true, attempts to break at a whitespace boundary.
     * @return list of chunks that together form the original content.
     * @throws IllegalArgumentException if maxChunkSize is not positive.
     */
    fun chunkContent(content: String, maxChunkSize: Int, preserveWords: Boolean = true): List<String> {
        require(maxChunkSize > 0) { "maxChunkSize must be positive" }
        if (content.length <= maxChunkSize) return listOf(content)

        val chunks = mutableListOf<String>()
        var start = 0
        val length = content.length

        while (start < length) {
            var end = (start + maxChunkSize).coerceAtMost(length)
            if (preserveWords && end < length) {
                // Attempt to find a newline or space to break on (if any exists between start and end)
                val breakPoint = content.lastIndexOfAny(charArrayOf('\n', ' '), startIndex = end)
                if (breakPoint > start) {
                    end = breakPoint
                }
            }
            // Ensure progress even if no good breakpoint was found.
            if (end == start) {
                end = (start + maxChunkSize).coerceAtMost(length)
            }
            chunks.add(content.substring(start, end))
            start = end
        }
        return chunks
    }

    fun formatBlock(content: String, language: String, format: OutputFormat): String = when (format) {
        OutputFormat.MARKDOWN -> "```$language\n$content\n```"
        OutputFormat.PLAIN -> content
        OutputFormat.HTML -> "<pre><code class=\"$language\">$content</code></pre>"
    }

    fun filterFilesByGitIgnore(filePaths: List<String>, gitIgnoreFile: File): List<String> {
        // TODO: Integrate a proper GitIgnore parser library.
        return filePaths
    }

    /**
     * Returns true if [fullPath] matches the given glob [pattern].
     * This implementation converts the glob to a regex.
     */
    fun matchesGlob(glob: String, fullPath: String): Boolean {
        return globToRegex(glob).matches(fullPath)
    }

    private fun globToRegex(glob: String): Regex {
        val sb = StringBuilder("^")
        var i = 0
        while (i < glob.length) {
            when (val c = glob[i]) {
                '*' -> {
                    // Check if the next character is also a '*'
                    if (i + 1 < glob.length && glob[i + 1] == '*') {
                        sb.append(".*")
                        i += 2
                        continue
                    } else {
                        sb.append(".*")
                    }
                }
                '?' -> sb.append(".")
                else -> {
                    // Escape regex-special characters
                    if ("\\.[]{}()+-^$|".indexOf(c) != -1) {
                        sb.append("\\")
                    }
                    sb.append(c)
                }
            }
            i++
        }
        sb.append("$")
        return Regex(sb.toString())
    }
}
