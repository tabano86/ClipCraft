package com.clipcraft.util

import com.clipcraft.model.ClipCraftOptions
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import com.github.javadev.gitignore.GitIgnore

object ClipCraftIgnoreUtil {

    /**
     * Merge .gitignore patterns (if present) into the provided options.
     */
    fun mergeGitIgnoreRules(opts: ClipCraftOptions, project: Project) {
        val baseDir = project.basePath ?: return
        val gitIgnoreFile = File(baseDir, ".gitignore")
        if (!gitIgnoreFile.exists() || !gitIgnoreFile.isFile) return

        val parser = GitIgnore.fromFile(gitIgnoreFile)

        // We still keep explicit ignore lists from ClipCraftOptions,
        // but also rely on the parser for robust matching in shouldIgnore() below.
        // If we wanted to expand the user's ignoreFolders/ignoreFiles, we could parse them.
        // For simplicity, do nothing else here. We'll just rely on 'parser' in shouldIgnore.
        // If you prefer to add them to 'opts.ignorePatterns' etc., do it here.
    }

    /**
     * Check if a file/directory should be ignored based on ClipCraftOptions
     * and any .gitignore rules loaded by [mergeGitIgnoreRules].
     *
     * If the user has .gitignore usage enabled, we do an additional check
     * with [GitIgnore.match] via [gitIgnoreParser].
     */
    fun shouldIgnore(file: VirtualFile, opts: ClipCraftOptions): Boolean {
        // Direct matches from ignoreFolders/ignoreFiles
        if (file.isDirectory && opts.ignoreFolders.any { file.name.equals(it, ignoreCase = true) }) return true
        if (!file.isDirectory && opts.ignoreFiles.any { file.name.equals(it, ignoreCase = true) }) return true

        // Basic patterns
        val positiveGlobs = opts.ignorePatterns.filter { !it.startsWith("!") }
        val negativeGlobs = opts.ignorePatterns.filter { it.startsWith("!") }.map { it.substring(1) }
        val path = file.path
        val matchesPositive = positiveGlobs.any { matchesGlob(it, path) }
        val matchesNegative = negativeGlobs.any { matchesGlob(it, path) }
        if (matchesPositive && !matchesNegative) {
            return true
        }

        // Additional check with .gitignore if enabled
        // We'll do a best-effort by re-loading .gitignore each time or caching it (see note below).
        if (opts.useGitIgnore) {
            val baseDir = file.project?.basePath
            if (!baseDir.isNullOrEmpty()) {
                val ignoreFile = File(baseDir, ".gitignore")
                if (ignoreFile.exists()) {
                    val parser = GitIgnore.fromFile(ignoreFile)
                    // Convert to path relative to base
                    val relative = file.path.removePrefix(baseDir).trimStart('/', '\\')
                    if (relative.isNotEmpty() && parser.match("/$relative", file.isDirectory)) {
                        return true
                    }
                }
            }
        }

        return false
    }

    /**
     * A basic naive "glob" match function used for user-specified ignore patterns.
     * If you prefer full-blown regex usage, we already do [Regex] in other places,
     * but we keep this for backward-compatibility with prior code.
     */
    private fun matchesGlob(glob: String, fullPath: String): Boolean {
        // A quick hack to transform simple * globs to a regex
        // It's not a perfect solution. For robust usage, use an official library or Regex directly.
        val pattern = glob
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".?")
        return Regex("^$pattern\$").containsMatchIn(fullPath)
    }
}
