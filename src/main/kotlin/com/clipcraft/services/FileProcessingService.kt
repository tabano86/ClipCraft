package com.clipcraft.services

import com.clipcraft.model.SettingsState
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.util.ArrayDeque

class FileProcessingResult(
    val markdownContent: String,
    val filesProcessed: Int,
    val filesSkipped: Int
)

private class FileProcessException(val reason: String) : Exception()

object FileProcessingService {

    fun processVirtualFiles(
        initialSelection: List<VirtualFile?>,
        settings: SettingsState,
        projectBasePath: Path,
        indicator: ProgressIndicator
    ): FileProcessingResult {
        val includeMatchers = settings.includeGlobs.lines().filter(String::isNotBlank).map(::createMatcher)
        val excludeMatchers = settings.excludeGlobs.lines().filter(String::isNotBlank).map(::createMatcher)
        val markdownBuilder = StringBuilder()
        var filesProcessed = 0
        var filesSkipped = 0

        indicator.text = "Collecting files..."
        val distinctFiles = collectDistinctFiles(initialSelection)

        distinctFiles.forEachIndexed { index, file ->
            indicator.checkCanceled()
            indicator.fraction = (index + 1).toDouble() / distinctFiles.size
            indicator.text2 = projectBasePath.relativize(file.toNioPath()).toString()

            val nioPath = file.toNioPath()
            val relativePath = projectBasePath.relativize(nioPath)

            if (!isMatch(relativePath, includeMatchers, excludeMatchers)) {
                return@forEachIndexed
            }

            markdownBuilder.append("--- \n")
                .append("### `").append(relativePath).append("`\n")

            try {
                val content = readFileContent(file, settings.maxFileSizeKb)
                val lang = file.fileType.name.lowercase().replace(" ", "")
                markdownBuilder.append("```").append(lang).append("\n")
                    .append(content)
                    .append("\n```\n\n")
                filesProcessed++
            } catch (e: FileProcessException) {
                markdownBuilder.append(e.reason).append("\n\n")
                filesSkipped++
            }
        }

        return FileProcessingResult(markdownBuilder.toString(), filesProcessed, filesSkipped)
    }

    private fun collectDistinctFiles(initialSelection: List<VirtualFile?>): List<VirtualFile> {
        val collectedFiles = mutableSetOf<VirtualFile>()
        val filesToProcess = ArrayDeque(initialSelection.filterNotNull())
        while (filesToProcess.isNotEmpty()) {
            val file = filesToProcess.pop()
            if (file.isDirectory) {
                file.children?.let { filesToProcess.addAll(it) }
            } else {
                collectedFiles.add(file)
            }
        }
        return collectedFiles.toList()
    }

    private fun readFileContent(file: VirtualFile, maxSizeKb: Int): String {
        if (file.fileType.isBinary) throw FileProcessException("- Skipped: File is binary.")

        val fileSizeKb = file.length / 1024
        if (fileSizeKb > maxSizeKb) throw FileProcessException("- Skipped: File size (${fileSizeKb}KB) exceeds limit (${maxSizeKb}KB).")

        return try {
            file.contentsToByteArray().toString(file.charset)
        } catch (e: IOException) {
            throw FileProcessException("- Skipped: Could not read file (${e.message}).")
        }
    }

    private fun createMatcher(glob: String): PathMatcher = FileSystems.getDefault().getPathMatcher("glob:$glob")

    private fun isMatch(path: Path, includes: List<PathMatcher>, excludes: List<PathMatcher>): Boolean {
        if (excludes.any { it.matches(path) }) return false
        return includes.isEmpty() || includes.any { it.matches(path) }
    }
}