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
        // Check explicit file/folder ignore lists (by file name).
        if (file.isDirectory && opts.ignoreFolders.any { file.name.equals(it, ignoreCase = true) }) return true
        if (!file.isDirectory && opts.ignoreFiles.any { file.name.equals(it, ignoreCase = true) }) return true

        // For ignorePatterns (non-gitignore), match against the file name.
        val fileName = file.name
        val positiveGlobs = opts.ignorePatterns.filter { !it.startsWith("!") }
        val negativeGlobs = opts.ignorePatterns.filter { it.startsWith("!") }.map { it.removePrefix("!") }
        val matchesPositive = positiveGlobs.any { matchesGlob(it, fileName) }
        val matchesNegative = negativeGlobs.any { matchesGlob(it, fileName) }
        if (matchesPositive && !matchesNegative) return true

        // If useGitIgnore is enabled, match against the file's relative path.
        if (opts.useGitIgnore) {
            val baseDir = project.basePath ?: return false
            val relativePath = try {
                File(baseDir).toPath().relativize(File(file.path).toPath())
                    .toString().replace(File.separatorChar, '/')
            } catch (e: Exception) {
                file.path
            }
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

    /**
     * If the pattern ends with '/', treat it as a directory pattern.
     * The relative path is a match if it equals the pattern (without the trailing slash)
     * or starts with that pattern followed by '/'.
     * Otherwise, convert the glob to a regex.
     */
    private fun matchGitignorePattern(pattern: String, relativePath: String, isDirectory: Boolean): Boolean {
        if (pattern.endsWith("/")) {
            val trimmedPattern = pattern.dropLast(1)
            return relativePath == trimmedPattern || relativePath.startsWith("$trimmedPattern/")
        }
        val regexPattern = if (pattern.startsWith("/")) {
            "^" + globToRegex(pattern.removePrefix("/")) + "$"
        } else {
            ".*" + globToRegex(pattern) + "$"
        }
        return Regex(regexPattern).matches(relativePath)
    }

    private fun globToRegex(glob: String): String {
        // Replace only '*' and '?' without escaping dots.
        return glob.replace("*", ".*").replace("?", ".?")
    }

    private fun matchesGlob(glob: String, target: String): Boolean {
        val regexPattern = "^" + globToRegex(glob) + "$"
        return Regex(regexPattern).containsMatchIn(target)
    }
}
