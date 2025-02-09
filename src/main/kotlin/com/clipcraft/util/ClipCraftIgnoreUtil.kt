package com.clipcraft.util

import com.clipcraft.model.ClipCraftOptions
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Paths

/**
 * Custom .gitignore utility implementation.
 * This version completely removes onelenyk’s parser and rolls our own basic parser/matcher.
 */
object ClipCraftIgnoreUtil {

    private val log = LoggerFactory.getLogger(ClipCraftIgnoreUtil::class.java)

    /**
     * Reads the .gitignore file from the project's base directory and
     * merges its non-comment, non-blank lines into the ignorePatterns list.
     */
    fun mergeGitIgnoreRules(opts: ClipCraftOptions, project: Project) {
        val baseDir = project.basePath ?: return
        val gitIgnoreFile = File(baseDir, ".gitignore")
        if (!gitIgnoreFile.exists() || !gitIgnoreFile.isFile) {
            log.debug("No .gitignore found at $baseDir")
            return
        }
        log.info("Merging .gitignore from $gitIgnoreFile")
        val patterns = parseGitignoreFile(gitIgnoreFile)
        opts.ignorePatterns = opts.ignorePatterns + patterns
    }

    /**
     * Checks whether the given VirtualFile should be ignored based on the provided
     * ClipCraftOptions and the custom .gitignore rules.
     *
     * @param file the file or directory to check
     * @param opts the options containing ignore lists and patterns
     * @param project the current project (used for determining the project base directory)
     * @return true if the file should be ignored; false otherwise
     */
    fun shouldIgnore(file: VirtualFile, opts: ClipCraftOptions, project: Project): Boolean {
        // Check direct matches from user-specified ignoreFolders/ignoreFiles.
        if (file.isDirectory && opts.ignoreFolders.any { file.name.equals(it, ignoreCase = true) }) return true
        if (!file.isDirectory && opts.ignoreFiles.any { file.name.equals(it, ignoreCase = true) }) return true

        // Check user-specified glob patterns.
        val positiveGlobs = opts.ignorePatterns.filter { !it.startsWith("!") }
        val negativeGlobs = opts.ignorePatterns.filter { it.startsWith("!") }.map { it.removePrefix("!") }
        val filePathStr = file.path
        val matchesPositive = positiveGlobs.any { matchesGlob(it, filePathStr) }
        val matchesNegative = negativeGlobs.any { matchesGlob(it, filePathStr) }
        if (matchesPositive && !matchesNegative) return true

        // Check .gitignore rules.
        if (opts.useGitIgnore) {
            val baseDir = project.basePath ?: return false
            // Compute the file's relative path with respect to the project root.
            val relativePath = file.path.removePrefix(baseDir).trimStart('/', '\\')
            if (relativePath.isNotEmpty()) {
                val gitIgnoreFile = File(baseDir, ".gitignore")
                if (gitIgnoreFile.exists() && gitIgnoreFile.isFile) {
                    val patterns = parseGitignoreFile(gitIgnoreFile)
                    var ignored = false
                    // Iterate over patterns in order; later rules override earlier ones.
                    for (pattern in patterns) {
                        if (pattern.startsWith("!")) {
                            val p = pattern.removePrefix("!")
                            if (matchGitignorePattern(p, relativePath, file.isDirectory)) {
                                ignored = false
                            }
                        } else {
                            if (matchGitignorePattern(pattern, relativePath, file.isDirectory)) {
                                ignored = true
                            }
                        }
                    }
                    if (ignored) return true
                }
            }
        }
        return false
    }

    /**
     * Parses a .gitignore file into a list of patterns.
     */
    private fun parseGitignoreFile(gitIgnoreFile: File): List<String> {
        return gitIgnoreFile.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
    }

    /**
     * Converts a .gitignore glob pattern into a regex and checks if it matches the given relative path.
     *
     * This simple implementation handles:
     * - Patterns anchored at the root if they start with a "/"
     * - Negated patterns are handled in the calling code.
     * - If a pattern ends with a "/", it will only match directories.
     *
     * @param pattern the .gitignore pattern (without a leading "!" if negated)
     * @param relativePath the file's path relative to the project root
     * @param isDirectory whether the file is a directory
     * @return true if the pattern matches the relativePath, false otherwise.
     */
    private fun matchGitignorePattern(pattern: String, relativePath: String, isDirectory: Boolean): Boolean {
        // If pattern ends with '/', it should only match directories.
        if (pattern.endsWith("/")) {
            if (!isDirectory) return false
        }
        // Remove trailing slash if present.
        val trimmedPattern = if (pattern.endsWith("/")) pattern.dropLast(1) else pattern

        // If pattern starts with '/', it is anchored at the root.
        val regexPattern = if (trimmedPattern.startsWith("/")) {
            "^" + globToRegex(trimmedPattern.removePrefix("/")) + "$"
        } else {
            // Otherwise, match anywhere in the relative path.
            ".*" + globToRegex(trimmedPattern) + "$"
        }
        return Regex(regexPattern).matches(relativePath)
    }

    /**
     * Converts a glob pattern to a regex string.
     * This simplistic conversion may need enhancements for full spec compliance.
     */
    private fun globToRegex(glob: String): String {
        return glob
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".?")
    }

    /**
     * Converts a glob pattern to a regex string and checks if it matches the full path.
     * Used for user-specified ignore patterns.
     */
    private fun matchesGlob(glob: String, fullPath: String): Boolean {
        val regexPattern = "^" + globToRegex(glob) + "$"
        return Regex(regexPattern).containsMatchIn(fullPath)
    }
}
