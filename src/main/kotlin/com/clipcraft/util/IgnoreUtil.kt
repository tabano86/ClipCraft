package com.clipcraft.util

import com.clipcraft.model.ClipCraftOptions
import com.intellij.openapi.diagnostic.Logger
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

object IgnoreUtil {
    private val logger = Logger.getInstance(IgnoreUtil::class.java)
    private fun parseGitIgnoreIfNeeded(opts: ClipCraftOptions, basePath: String) {
        if (opts.useGitIgnore) loadIgnoreFile(Paths.get(basePath, ".gitignore"), opts)
    }
    fun shouldIgnore(file: File, opts: ClipCraftOptions, projectBase: String): Boolean {
        parseGitIgnoreIfNeeded(opts, projectBase)
        if (fileInIgnoreFiles(file, opts.ignoreFiles)) return true
        if (folderInIgnoreFolders(file, opts.ignoreFolders, projectBase)) return true
        val rel = toRelative(file.absolutePath, projectBase).replace('\\', '/').removePrefix("/")
        val patterns = gatherAllPatterns(opts)
        var ignored = false
        for (p in patterns) {
            if (p.isBlank()) continue
            val neg = p.startsWith("!")
            val pattern = p.removePrefix("!").removePrefix("/").let {
                if (it.endsWith("/") && !it.endsWith("/**")) "$it**" else it
            }
            if (standardGlobToRegex(pattern).matches(rel)) {
                ignored = !neg
            }
        }
        return if (opts.invertIgnorePatterns) !ignored else ignored
    }
    private fun fileInIgnoreFiles(f: File, ignoreFiles: List<String>?): Boolean =
        ignoreFiles?.any { it.equals(f.name, ignoreCase = true) } == true
    private fun folderInIgnoreFolders(f: File, ignoreFolders: List<String>?, basePath: String): Boolean {
        val rel = toRelative(f.absolutePath, basePath).replace('\\', '/').removePrefix("/")
        val lastSegment = rel.substringAfterLast('/')
        return ignoreFolders?.any { it.equals(lastSegment, ignoreCase = true) } == true
    }
    private fun gatherAllPatterns(opts: ClipCraftOptions): List<String> {
        val addl = opts.additionalIgnorePatterns
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
        return (opts.ignorePatterns + addl).filter { it.isNotBlank() }
    }
    private fun toRelative(absPath: String, basePath: String): String {
        if (basePath.isEmpty()) return absPath
        return try {
            val base = File(basePath).absolutePath
            if (!absPath.startsWith(base)) {
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
        } catch (e: Exception) {
            logger.warn("Error reading ${file.absolutePath}", e)
        }
    }
    fun standardGlobToRegex(glob: String, options: GlobOptions = GlobOptions()): Regex {
        val (extended, globstar, flags) = options
        val sb = StringBuilder()
        var inGroup = false
        var i = 0
        while (i < glob.length) {
            val c = glob[i]
            when (c) {
                in listOf('/', '$', '^', '+', '.', '(', ')', '=', '!', '|') -> sb.append('\\').append(c)
                '?' -> sb.append("[^/]")
                '[', ']' ->
                    if (extended) sb.append(c) else sb.append('\\').append(c)
                '{' ->
                    if (extended) {
                        inGroup = true; sb.append('(')
                    } else {
                        sb.append("\\{")
                    }
                '}' ->
                    if (extended) {
                        inGroup = false; sb.append(')')
                    } else {
                        sb.append("\\}")
                    }
                ',' ->
                    if (inGroup) sb.append('|') else sb.append("\\,")
                '*' -> {
                    var starCount = 1
                    while (i + 1 < glob.length && glob[i + 1] == '*') {
                        starCount++
                        i++
                    }
                    if (!globstar) {
                        sb.append(".*")
                    } else {
                        sb.append("([^/]*)")
                    }
                }
                else -> sb.append(c)
            }
            i++
        }
        val pattern = if (!flags.contains('g')) "^$sb\$" else sb.toString()
        val opts = mutableSetOf<RegexOption>().apply {
            if (flags.contains('i', ignoreCase = true)) add(RegexOption.IGNORE_CASE)
        }
        return Regex(pattern, opts)
    }
}
