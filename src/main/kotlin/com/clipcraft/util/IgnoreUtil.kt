package com.clipcraft.util

import com.clipcraft.model.ClipCraftOptions
import com.intellij.openapi.project.Project
import matchesGlob
import java.io.File
import java.io.IOException
import java.nio.file.Paths

object IgnoreUtil {

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

    fun mergeGitIgnoreRules(options: ClipCraftOptions, project: Project) {
        parseGitIgnoreIfEnabled(options, project.basePath ?: "")
    }

    fun shouldIgnore(file: File, options: ClipCraftOptions, projectBasePath: String): Boolean {
        // Skip dotfiles/folders
        if (file.name.startsWith(".")) return true

        // If file name is in ignoreFiles
        options.ignoreFiles?.let { files ->
            if (!file.isDirectory && files.any { it.equals(file.name, ignoreCase = true) }) {
                return true
            }
        }

        // If folder name is in ignoreFolders
        options.ignoreFolders?.let { folders ->
            if (file.isDirectory && folders.any { it.equals(file.name, ignoreCase = true) }) {
                return true
            }
        }

        val relPath = toRelativePath(file.absolutePath, projectBasePath).replace('\\', '/')

        // Combine base ignorePatterns with additionalIgnorePatterns
        val additional = options.additionalIgnorePatterns
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
        val allPatterns = (options.ignorePatterns + additional)

        var isIgnored = false
        for (pattern in allPatterns) {
            if (pattern.isBlank()) continue
            val isNegation = pattern.startsWith("!")
            val rawPattern = if (isNegation) pattern.substring(1) else pattern
            if (matchesGlob(adjustGitIgnoreDirPattern(rawPattern), relPath)) {
                isIgnored = !isNegation
            }
        }

        val finalIgnored = if (options.invertIgnorePatterns) !isIgnored else isIgnored
        return finalIgnored
    }

    private fun toRelativePath(filePath: String, projectBasePath: String): String {
        val basePath = File(projectBasePath).absolutePath
        return if (filePath.startsWith(basePath)) {
            filePath.substring(basePath.length).trimStart(File.separatorChar)
        } else filePath
    }

    private fun adjustGitIgnoreDirPattern(glob: String): String {
        return if (glob.endsWith("/") && !glob.endsWith("/**")) {
            "$glob**"
        } else glob
    }
}