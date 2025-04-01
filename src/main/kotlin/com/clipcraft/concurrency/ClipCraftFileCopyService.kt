package com.clipcraft.concurrency

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.Snippet
import com.clipcraft.util.ClipCraftNotificationCenter
import com.clipcraft.util.CodeFormatter
import com.clipcraft.util.IgnoreUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.awt.datatransfer.StringSelection
import java.util.concurrent.Semaphore

class ClipCraftFileCopyService {
    private val logger = Logger.getInstance(ClipCraftFileCopyService::class.java)

    suspend fun copyFiles(
        project: Project,
        files: List<VirtualFile>,
        options: ClipCraftOptions,
        indicator: ProgressIndicator,
    ) = coroutineScope {
        // Pre-parse .gitignore once to avoid repeated I/O
        IgnoreUtil.parseGitIgnoreIfNeeded(options, project.basePath ?: "")

        val expanded = expandFiles(files)
        val snippetList = when (options.concurrencyMode) {
            com.clipcraft.model.ConcurrencyMode.DISABLED ->
                readSequential(expanded, project, options, indicator)

            else ->
                readConcurrently(expanded, project, options, indicator)
        }

        // Format in chunks, etc.
        val combinedText = CodeFormatter.formatSnippets(snippetList, options)

        // Optional user prompt if too large
        val globalMaxChars = com.clipcraft.services.ClipCraftSettingsState.getInstance().maxCopyCharacters
        if (globalMaxChars > 0 && combinedText.length > globalMaxChars) {
            val userChoice = javax.swing.JOptionPane.showConfirmDialog(
                null,
                "Output is ${combinedText.length} chars (limit $globalMaxChars). Copy anyway?",
                "ClipCraft Large Copy",
                javax.swing.JOptionPane.YES_NO_OPTION,
            )
            if (userChoice != javax.swing.JOptionPane.YES_OPTION) {
                logger.info("User canceled large copy.")
                return@coroutineScope
            }
        }

        // Copy to clipboard if configured
        if (options.outputTarget == com.clipcraft.model.OutputTarget.CLIPBOARD ||
            options.outputTarget == com.clipcraft.model.OutputTarget.BOTH
        ) {
            // Must ensure on EDT for reliable system clipboard
            ApplicationManager.getApplication().invokeLater {
                try {
                    CopyPasteManager.getInstance().setContents(StringSelection(combinedText))
                    ClipCraftNotificationCenter.info(
                        "Copied ${snippetList.size} snippet(s), length: ${combinedText.length} chars.",
                    )
                } catch (ex: Exception) {
                    logger.error("Clipboard copy failed", ex)
                    ClipCraftNotificationCenter.error("Failed to copy to clipboard.")
                }
            }
        }
    }

    private fun expandFiles(files: List<VirtualFile>): List<VirtualFile> {
        val result = mutableListOf<VirtualFile>()
        fun recurse(f: VirtualFile) {
            if (f.isDirectory) {
                f.children?.forEach { recurse(it) }
            } else {
                result.add(f)
            }
        }
        files.forEach { recurse(it) }
        return result
    }

    private fun readSequential(
        files: List<VirtualFile>,
        project: Project,
        options: ClipCraftOptions,
        indicator: ProgressIndicator,
    ): List<Snippet> {
        val result = mutableListOf<Snippet>()
        for ((index, vf) in files.withIndex()) {
            indicator.checkCanceled()
            indicator.text = "Reading file (${index + 1}/${files.size}): ${vf.name}"
            if (!shouldInclude(vf, project, options)) continue
            val content = vf.readTextOrNull() ?: continue
            result.add(
                Snippet(
                    filePath = vf.path,
                    fileName = vf.name,
                    relativePath = null,
                    fileSizeBytes = content.length.toLong(),
                    lastModified = System.currentTimeMillis(),
                    content = content,
                ),
            )
        }
        return result
    }

    private suspend fun readConcurrently(
        files: List<VirtualFile>,
        project: Project,
        options: ClipCraftOptions,
        indicator: ProgressIndicator,
    ): List<Snippet> = coroutineScope {
        val semaphore = Semaphore(options.maxConcurrentTasks)
        files.mapIndexed { index, vf ->
            async {
                semaphore.acquire()
                try {
                    indicator.checkCanceled()
                    indicator.text = "Reading file (${index + 1}/${files.size}): ${vf.name}"
                    if (!shouldInclude(vf, project, options)) return@async null
                    val content = vf.readTextOrNull()
                    content?.let {
                        Snippet(
                            filePath = vf.path,
                            fileName = vf.name,
                            relativePath = null,
                            fileSizeBytes = it.length.toLong(),
                            lastModified = System.currentTimeMillis(),
                            content = it,
                        )
                    }
                } finally {
                    semaphore.release()
                }
            }
        }.awaitAll().filterNotNull()
    }

    private fun shouldInclude(vf: VirtualFile, project: Project, options: ClipCraftOptions): Boolean {
        if (!options.includeImageFiles && vf.isImageFile()) return false
        if (options.useGitIgnore && com.clipcraft.util.IgnoreUtil.shouldIgnore(
                java.io.File(vf.path),
                options,
                project.basePath ?: "",
            )
        ) {
            return false
        }
        return true
    }

    private fun VirtualFile.readTextOrNull(): String? = try {
        String(contentsToByteArray(), charset)
    } catch (ex: Exception) {
        logger.warn("Error reading file $path: ${ex.message}", ex)
        null
    }

    private fun VirtualFile.isImageFile(): Boolean {
        val ext = extension?.lowercase()
        return ext in listOf("png", "jpg", "jpeg", "gif", "bmp", "webp", "tiff", "ico", "svg")
    }
}
