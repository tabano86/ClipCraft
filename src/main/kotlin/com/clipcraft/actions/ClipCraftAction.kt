package com.clipcraft.actions

import com.clipcraft.services.ClipCraftNotificationCenter
import com.clipcraft.services.ClipCraftSettingsState
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.JOptionPane

/**
 * The main ClipCraft copy action: merges and copies content of selected files.
 */
class ClipCraftAction : AnAction() {

    private val logger = Logger.getInstance(ClipCraftAction::class.java)

    override fun update(e: AnActionEvent) {
        val vFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        e.presentation.isVisible = vFiles != null && vFiles.isNotEmpty()
        e.presentation.isEnabled = vFiles != null && vFiles.isNotEmpty()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val vFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        if (vFiles.isEmpty()) {
            ClipCraftNotificationCenter.warn("No files selected to copy.")
            return
        }

        // We'll run everything in a background task so we don't block UI
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "ClipCraft Copy", true) {
            override fun run(indicator: com.intellij.openapi.progress.ProgressIndicator) {
                runBlocking {
                    copyContents(project, vFiles.toList())
                }
            }
        })
    }

    /**
     * Uses coroutines to read content from files, merges, checks limits, and copies to clipboard.
     */
    private suspend fun copyContents(project: Project, files: List<VirtualFile>) = coroutineScope {
        val settings = ClipCraftSettingsState.getInstance()
        val maxCopy = settings.maxCopyCharacters

        // Expand directories and read contents concurrently
        val allFiles = expandFiles(files)
        logger.info("Preparing to copy ${allFiles.size} file(s) total.")

        val readResults = allFiles.map { file ->
            async(Dispatchers.IO) {
                readFileContent(file)
            }
        }.awaitAll()

        // Combine results
        val combinedText = StringBuilder()
        val failedFiles = mutableListOf<String>()

        for ((i, readResult) in readResults.withIndex()) {
            if (readResult == null) {
                failedFiles.add(allFiles[i].name)
                continue
            }
            if (combinedText.isNotEmpty()) {
                combinedText.append("\n\n")
            }
            combinedText.append(readResult.trimEnd())
        }

        // If there were any unreadable files, notify
        if (failedFiles.isNotEmpty()) {
            ClipCraftNotificationCenter.warn("Some files couldn't be read: ${failedFiles.joinToString(", ")}")
        }

        val textToCopy = combinedText.toString()
        // If text is bigger than limit, confirm
        if (maxCopy > 0 && textToCopy.length > maxCopy) {
            val response = JOptionPane.showConfirmDialog(
                null,
                "The combined text is ${textToCopy.length} chars, exceeding your limit ($maxCopy). Copy anyway?",
                "Large Copy Confirmation",
                JOptionPane.YES_NO_OPTION,
            )
            if (response != JOptionPane.YES_OPTION) {
                logger.info("User cancelled large copy.")
                return@coroutineScope
            }
        }

        // Finally copy to system clipboard
        val success = copyToClipboard(textToCopy)
        if (success) {
            ClipCraftNotificationCenter.info("Copied ${allFiles.size} file(s). Length: ${textToCopy.length} chars.")
        } else {
            ClipCraftNotificationCenter.error("Failed to copy to clipboard.")
        }
    }

    /**
     * Collects all files from the given list, recursively expanding directories.
     */
    private fun expandFiles(files: List<VirtualFile>): List<VirtualFile> {
        val result = mutableListOf<VirtualFile>()
        fun recurse(file: VirtualFile) {
            if (file.isDirectory) {
                file.children?.forEach { recurse(it) }
            } else {
                result.add(file)
            }
        }
        files.forEach { recurse(it) }
        return result
    }

    /**
     * Reads the content of a virtual file. Returns null if read fails.
     */
    private fun readFileContent(file: VirtualFile): String? {
        return try {
            val bytes = file.contentsToByteArray()
            val text = String(bytes, file.charset)
            text
        } catch (ex: Exception) {
            logger.warn("Error reading file ${file.path}: ${ex.message}", ex)
            null
        }
    }

    /**
     * Attempts to place text on the system clipboard, including WSL handling if necessary.
     */
    private fun copyToClipboard(text: String): Boolean {
        return try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(StringSelection(text), null)
            true
        } catch (ex: Exception) {
            logger.error("Clipboard copy failed", ex)
            false
        }
    }
}
