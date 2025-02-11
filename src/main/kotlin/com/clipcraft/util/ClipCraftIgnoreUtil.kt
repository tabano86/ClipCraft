package com.clipcraft.util

import com.clipcraft.model.ClipCraftOptions
import java.io.File
import java.nio.file.Paths

object ClipCraftIgnoreUtil {

    /**
     * If the useGitIgnore flag is set in [options], reads the .gitignore file from the given
     * [projectBasePath] and adds any non-empty, non-comment pattern to the options.ignorePatterns list.
     */
    fun parseGitIgnoreIfEnabled(options: ClipCraftOptions, projectBasePath: String) {
        if (!options.useGitIgnore) return

        val gitIgnoreFile = Paths.get(projectBasePath, ".gitignore").toFile()
        if (gitIgnoreFile.exists() && gitIgnoreFile.isFile) {
            gitIgnoreFile.readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .forEach { pattern ->
                    if (!options.ignorePatterns.contains(pattern)) {
                        options.ignorePatterns.add(pattern)
                    }
                }
        }
    }

    /**
     * Checks whether the given [file] should be ignored based on the .gitignore and custom ignore patterns in [options].
     * The file's path is converted to a relative path (if possible) with respect to [projectBasePath].
     */
    fun shouldIgnore(file: File, options: ClipCraftOptions, projectBasePath: String): Boolean {
        val basePath = File(projectBasePath).absolutePath
        val filePath = file.absolutePath
        val relativePath = if (filePath.startsWith(basePath)) filePath.substring(basePath.length) else filePath

        return options.ignorePatterns.any { pattern ->
            matchPattern(relativePath, pattern)
        }
    }

    /**
     * A very basic pattern matcher that supports "*" as a wildcard.
     * For robust matching (including .gitignore semantics), consider using a dedicated library.
     */
    private fun matchPattern(path: String, pattern: String): Boolean {
        // Convert "*" to ".*" and anchor the regex
        val regexPattern = "^" + pattern.replace("*", ".*") + "$"
        return Regex(regexPattern, RegexOption.IGNORE_CASE).matches(path)
    }
}
