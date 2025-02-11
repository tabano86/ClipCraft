package com.clipcraft.util

import com.clipcraft.model.ClipCraftOptions
import com.intellij.openapi.project.Project
import java.io.File
import java.io.IOException
import java.nio.file.Paths

/**
 * Provides utilities for determining whether a file should be ignored,
 * including reading .gitignore and handling additional glob patterns.
 */
object IgnoreUtil {

    /**
     * Reads the .gitignore file from [projectBasePath] and adds its patterns to options.ignorePatterns.
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
     * Merges .gitignore rules into options.
     */
    fun mergeGitIgnoreRules(options: ClipCraftOptions, project: Project) {
        parseGitIgnoreIfEnabled(options, project.basePath ?: "")
    }

    /**
     * Returns true if [file] should be ignored based on [options].
     * Supports .gitignore-like leading "!" to negate a prior ignore.
     */
    fun shouldIgnore(file: File, options: ClipCraftOptions, projectBasePath: String): Boolean {
        // Always ignore hidden files/folders (starting with '.').
        if (file.name.startsWith(".")) return true

        // If file name is in ignoreFiles, ignore it.
        options.ignoreFiles?.let { files ->
            if (!file.isDirectory && files.any { it.equals(file.name, ignoreCase = true) }) {
                return true
            }
        }

        // If folder name is in ignoreFolders, ignore it.
        options.ignoreFolders?.let { folders ->
            if (file.isDirectory && folders.any { it.equals(file.name, ignoreCase = true) }) {
                return true
            }
        }

        // If directory pattern matching is enabled and file is a directory, check for patterns ending in "/"
        if (options.enableDirectoryPatternMatching && file.isDirectory) {
            val dirPatterns = options.ignorePatterns.filter { it.endsWith("/") }
            val relPathDir = toRelativePath(file.absolutePath, projectBasePath)
            // If any directory pattern matches, ignore it; but note we still need the "!" negation logic
            // so we’ll handle it with the same path-based approach below.
        }

        // Combine base ignore patterns with additional ones (comma-separated).
        val additional = options.additionalIgnorePatterns
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
        val allPatterns = (options.ignorePatterns + additional)

        // Convert the file’s absolute path to a project-relative path (with forward slashes).
        val relPath = toRelativePath(file.absolutePath, projectBasePath).replace('\\', '/')

        // Evaluate all patterns in order, as .gitignore does.
        // The last pattern that matches determines if the file is ignored or not.
        // A pattern starting with "!" => un-ignore (negate).
        var isIgnored = false
        for (pattern in allPatterns) {
            if (pattern.isBlank()) continue

            val isNegation = pattern.startsWith("!")
            val rawPattern = if (isNegation) pattern.substring(1) else pattern

            if (matchesGlob(rawPattern, relPath)) {
                // If it's a normal pattern => mark ignored
                // If it's a "!" pattern => un-ignore
                isIgnored = !isNegation
            }
        }

        // If invertIgnorePatterns == true, flip the final decision
        val finalIgnored = if (options.invertIgnorePatterns) !isIgnored else isIgnored
        return finalIgnored
    }

    /**
     * Converts an absolute file path to a project-relative path.
     */
    private fun toRelativePath(filePath: String, projectBasePath: String): String {
        val basePath = File(projectBasePath).absolutePath
        return if (filePath.startsWith(basePath)) {
            filePath.substring(basePath.length).trimStart(File.separatorChar)
        } else filePath
    }

    /**
     * Converts [glob] to a regex, then checks if [path] matches it.
     * In .gitignore, a trailing slash means "match any file within this directory."
     */
    fun matchesGlob(glob: String, path: String): Boolean {
        val normalizedPath = path.replace('\\', '/')

        // If it ends with '/', interpret as "folder + any subpath" => 'secret/' => 'secret/**'
        val effectiveGlob = if (glob.endsWith("/") && !glob.endsWith("/**")) {
            glob + "**"
        } else {
            glob
        }

        val patternBuilder = StringBuilder("^")
        var i = 0
        while (i < effectiveGlob.length) {
            when (val c = effectiveGlob[i]) {
                '.', '(', ')', '+', '|', '^', '$', '@', '%', '=', '!' ->
                    patternBuilder.append('\\').append(c)

                '?' ->
                    patternBuilder.append("[^/]")

                '*' -> {
                    // Check if "**"
                    val nextStar = (i + 1 < effectiveGlob.length && effectiveGlob[i + 1] == '*')
                    if (nextStar) {
                        patternBuilder.append(".*")  // matches zero or more dirs/files
                        i++
                    } else {
                        patternBuilder.append("[^/]*") // single '*'
                    }
                }

                '\\' ->
                    patternBuilder.append("\\\\")

                else ->
                    patternBuilder.append(c)
            }
            i++
        }
        patternBuilder.append('$')

        // Case-insensitive
        val regex = Regex(patternBuilder.toString(), RegexOption.IGNORE_CASE)
        return regex.matches(normalizedPath)
    }

}
