package com.clipcraft.util

import com.clipcraft.model.ClipCraftOptions
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import matchesGlob
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths

object IgnoreUtil {
    private val logger = Logger.getInstance(IgnoreUtil::class.java)

    /**
     * Called automatically inside [shouldIgnore] if [ClipCraftOptions.useGitIgnore] is true.
     */
    private fun parseGitIgnoreIfNeeded(opts: ClipCraftOptions, basePath: String) {
        if (!opts.useGitIgnore) return
        loadIgnoreFile(Paths.get(basePath, ".gitignore"), opts)
    }

    fun mergeGitIgnoreRules(opts: ClipCraftOptions, project: Project) {
        val base = project.basePath
        if (!base.isNullOrEmpty()) loadIgnoreFile(Paths.get(base, ".gitignore"), opts)
    }

    fun parseCustomIgnoreFiles(opts: ClipCraftOptions, projectBase: String, files: List<String>) {
        files.forEach { loadIgnoreFile(Paths.get(projectBase, it), opts) }
    }

    fun shouldIgnore(file: File, opts: ClipCraftOptions, projectBase: String): Boolean {
        parseGitIgnoreIfNeeded(opts, projectBase)

        if (file.name.startsWith(".")) return true
        if (fileInIgnoreFiles(file, opts.ignoreFiles)) return true
        if (folderInIgnoreFolders(file, opts.ignoreFolders, projectBase)) return true

        var rel = toRelative(file.absolutePath, projectBase).replace('\\', '/')
        if (rel.startsWith("/")) rel = rel.removePrefix("/")

        val patterns = gatherAllPatterns(opts)
        var ignored = false
        for (pattern in patterns) {
            if (pattern.isBlank()) continue
            val neg = pattern.startsWith("!")
            val raw = pattern.removePrefix("!")
            var adj = raw
            if (adj.startsWith("/")) adj = adj.removePrefix("/")
            if (adj.endsWith("/") && !adj.endsWith("/**")) adj += "**"
            if (matchesGlob(adj, rel)) ignored = !neg
        }

        return if (opts.invertIgnorePatterns) !ignored else ignored
    }

    private fun fileInIgnoreFiles(f: File, ignoreFiles: List<String>?): Boolean {
        if (ignoreFiles.isNullOrEmpty()) return false
        return ignoreFiles.any { it.equals(f.name, ignoreCase = true) }
    }

    private fun folderInIgnoreFolders(f: File, ignoreFolders: List<String>?, basePath: String): Boolean {
        if (ignoreFolders.isNullOrEmpty()) return false
        val rel = toRelative(f.absolutePath, basePath).replace('\\', '/').removePrefix("/")
        val lastSegment = rel.substringAfterLast('/')
        return ignoreFolders.any { it.equals(lastSegment, ignoreCase = true) }
    }

    private fun gatherAllPatterns(opts: ClipCraftOptions): List<String> {
        val main = opts.ignorePatterns
        val addl = opts.additionalIgnorePatterns
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() } ?: emptyList()
        return (main + addl).filter { it.isNotBlank() }
    }

    private fun toRelative(absPath: String, basePath: String): String {
        val absBase = File(basePath).absolutePath
        if (!absPath.startsWith(absBase)) return absPath
        return absPath.substring(absBase.length).trimStart(File.separatorChar)
    }

    private fun loadIgnoreFile(p: Path, opts: ClipCraftOptions) {
        val f = p.toFile()
        if (!f.exists() || !f.isFile) return
        try {
            f.readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .forEach { line ->
                    if (line !in opts.ignorePatterns) {
                        opts.ignorePatterns.add(line)
                    }
                }
        } catch (e: IOException) {
            logger.warn("Error reading ignore file: ${f.absolutePath}", e)
        }
    }
}
