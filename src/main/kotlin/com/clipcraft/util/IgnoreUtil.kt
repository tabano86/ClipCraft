package com.clipcraft.util

import com.clipcraft.model.ClipCraftOptions
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths

object IgnoreUtil {
    private val logger = Logger.getInstance(IgnoreUtil::class.java)
    private fun parseGitIgnoreIfNeeded(opts: ClipCraftOptions, basePath: String) {
        if (opts.useGitIgnore) loadIgnoreFile(Paths.get(basePath, ".gitignore"), opts)
    }
    fun mergeGitIgnoreRules(opts: ClipCraftOptions, project: Project) {
        project.basePath?.let { loadIgnoreFile(Paths.get(it, ".gitignore"), opts) }
    }
    fun parseCustomIgnoreFiles(opts: ClipCraftOptions, projectBase: String, files: List<String>) {
        files.forEach { loadIgnoreFile(Paths.get(projectBase, it), opts) }
    }
    fun shouldIgnore(file: File, opts: ClipCraftOptions, projectBase: String): Boolean {
        parseGitIgnoreIfNeeded(opts, projectBase)
        if (file.name.startsWith(".")) return true
        if (fileInIgnoreFiles(file, opts.ignoreFiles)) return true
        if (folderInIgnoreFolders(file, opts.ignoreFolders, projectBase)) return true
        var rel = toRelative(file.absolutePath, projectBase).replace('\\', '/').removePrefix("/")
        val patterns = gatherAllPatterns(opts)
        var ignored = false
        for (p in patterns) {
            if (p.isBlank()) continue
            val neg = p.startsWith("!")
            val pattern = p.removePrefix("!").removePrefix("/").let {
                if (it.endsWith("/") && !it.endsWith("/**")) "$it**" else it
            }
            if (globToRegex(pattern).matches(rel)) ignored = !neg
        }
        return if (opts.invertIgnorePatterns) !ignored else ignored
    }
    private fun fileInIgnoreFiles(f: File, ignoreFiles: List<String>?): Boolean =
        ignoreFiles?.any { it.equals(f.name, ignoreCase = true) } ?: false
    private fun folderInIgnoreFolders(f: File, ignoreFolders: List<String>?, basePath: String): Boolean {
        val rel = toRelative(f.absolutePath, basePath).replace('\\', '/').removePrefix("/")
        val lastSegment = rel.substringAfterLast('/')
        return ignoreFolders?.any { it.equals(lastSegment, ignoreCase = true) } ?: false
    }
    private fun gatherAllPatterns(opts: ClipCraftOptions): List<String> {
        val addl = opts.additionalIgnorePatterns?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
        return (opts.ignorePatterns + addl).filter { it.isNotBlank() }
    }
    private fun toRelative(absPath: String, basePath: String): String {
        if (basePath.isEmpty()) return absPath
        return try {
            val base = File(basePath).absolutePath
            if (absPath.length < base.length || !absPath.startsWith(base)) {
                absPath
            } else {
                absPath.substring(base.length).trimStart(File.separatorChar)
            }
        } catch (e: Exception) {
            absPath
        }
    }
    private fun loadIgnoreFile(p: Path, opts: ClipCraftOptions) {
        val file = p.toFile()
        if (!file.exists() || !file.isFile) return
        try {
            file.readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .forEach {
                    if (it !in opts.ignorePatterns) opts.ignorePatterns.add(it)
                }
        } catch (e: IOException) {
            logger.warn("Error reading ${file.absolutePath}", e)
        }
    }
}
