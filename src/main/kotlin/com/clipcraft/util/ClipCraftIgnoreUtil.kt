package com.clipcraft.util

import com.clipcraft.model.ClipCraftOptions
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.slf4j.LoggerFactory
import java.io.File

object ClipCraftIgnoreUtil {

    private val log = LoggerFactory.getLogger(ClipCraftIgnoreUtil::class.java)

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

    fun shouldIgnore(file: VirtualFile, opts: ClipCraftOptions, project: Project): Boolean {
        if (file.isDirectory && opts.ignoreFolders.any { file.name.equals(it, ignoreCase = true) }) return true
        if (!file.isDirectory && opts.ignoreFiles.any { file.name.equals(it, ignoreCase = true) }) return true

        val positiveGlobs = opts.ignorePatterns.filter { !it.startsWith("!") }
        val negativeGlobs = opts.ignorePatterns.filter { it.startsWith("!") }.map { it.removePrefix("!") }
        val filePathStr = file.path
        val matchesPositive = positiveGlobs.any { matchesGlob(it, filePathStr) }
        val matchesNegative = negativeGlobs.any { matchesGlob(it, filePathStr) }
        if (matchesPositive && !matchesNegative) return true

        if (opts.useGitIgnore) {
            val baseDir = project.basePath ?: return false
            val relativePath = file.path.removePrefix(baseDir).trimStart('/', '\\')
            if (relativePath.isNotEmpty()) {
                val gitIgnoreFile = File(baseDir, ".gitignore")
                if (gitIgnoreFile.exists() && gitIgnoreFile.isFile) {
                    val patterns = parseGitignoreFile(gitIgnoreFile)
                    var ignored = false
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

    private fun parseGitignoreFile(gitIgnoreFile: File): List<String> {
        return gitIgnoreFile.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
    }

    private fun matchGitignorePattern(pattern: String, relativePath: String, isDirectory: Boolean): Boolean {
        if (pattern.endsWith("/")) {
            if (!isDirectory) return false
        }
        val trimmedPattern = if (pattern.endsWith("/")) pattern.dropLast(1) else pattern
        val regexPattern = if (trimmedPattern.startsWith("/")) {
            "^" + globToRegex(trimmedPattern.removePrefix("/")) + "$"
        } else {
            ".*" + globToRegex(trimmedPattern) + "$"
        }
        return Regex(regexPattern).matches(relativePath)
    }

    private fun globToRegex(glob: String): String {
        return glob
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".?")
    }

    private fun matchesGlob(glob: String, fullPath: String): Boolean {
        val regexPattern = "^" + globToRegex(glob) + "$"
        return Regex(regexPattern).containsMatchIn(fullPath)
    }
}
