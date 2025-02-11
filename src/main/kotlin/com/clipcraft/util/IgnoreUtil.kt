package com.clipcraft.util

import com.clipcraft.model.ClipCraftOptions
import java.io.File
import java.io.IOException
import java.nio.file.Paths

/**
 * Provides utilities for determining whether a file should be ignored,
 * including reading .gitignore or custom patterns from ClipCraftOptions.
 */
object IgnoreUtil {

    /**
     * If the useGitIgnore flag is set in [options], reads the .gitignore file from
     * the given [projectBasePath] and adds any non-empty, non-comment pattern to
     * options.ignorePatterns if not already present.
     */
    fun parseGitIgnoreIfEnabled(options: ClipCraftOptions, projectBasePath: String) {
        if (!options.useGitIgnore) return

        val gitIgnoreFile = Paths.get(projectBasePath, ".gitignore").toFile()
        if (!gitIgnoreFile.exists() || !gitIgnoreFile.isFile) return

        try {
            gitIgnoreFile.readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .forEach { pattern ->
                    if (pattern !in options.ignorePatterns) {
                        options.ignorePatterns.add(pattern)
                    }
                }
        } catch (e: IOException) {
            println("Error reading .gitignore: ${e.message}")
        }
    }

    /**
     * Returns true if the given [file] should be ignored based on patterns in [options].
     * The file's path is converted to a relative path with respect to [projectBasePath].
     */
    fun shouldIgnore(file: File, options: ClipCraftOptions, projectBasePath: String): Boolean {
        // Hidden files/folders
        if (file.name.startsWith(".")) {
            return true
        }

        val basePath = File(projectBasePath).absolutePath
        val filePath = file.absolutePath

        val relativePath = if (filePath.startsWith(basePath)) {
            filePath.substring(basePath.length).trimStart(File.separatorChar)
        } else filePath

        return options.ignorePatterns.any { pattern ->
            matchPattern(relativePath, pattern)
        }
    }

    /**
     * Converts a pattern containing '*' into a case-insensitive regex and checks if [path] matches.
     */
    private fun matchPattern(path: String, pattern: String): Boolean {
        val regexPattern = "^" + pattern.replace("*", ".*") + "$"
        return Regex(regexPattern, RegexOption.IGNORE_CASE).matches(path)
    }
}
