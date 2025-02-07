package com.clipcraft

import com.clipcraft.model.ClipCraftOptions
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import java.awt.datatransfer.StringSelection
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Paths

class ClipCraftAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val selected = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        if (selected.isNullOrEmpty()) {
            ephemeralNotification("No files or directories selected.")
            return
        }

        // Load saved settings
        val settings = ClipCraftSettings.getInstance()
        val currentOpts = settings.state

        // Show dialog if autoProcess = false
        val finalOpts = if (!currentOpts.autoProcess) {
            val dialog = ClipCraftOptionsDialog(currentOpts)
            if (!dialog.showAndGet()) return else dialog.getOptions()
        } else currentOpts

        // Save any changes
        settings.loadState(finalOpts)

        val basePath = project?.basePath ?: ""
        val allCodeBlocks = mutableListOf<String>()

        // Collect code from all selected files/folders
        selected.forEach { file ->
            val codeBlock = processVirtualFile(file, basePath, finalOpts, project)
            if (codeBlock.isNotEmpty()) {
                allCodeBlocks.add(codeBlock)
            }
        }

        // Combine code blocks
        val finalCode = if (finalOpts.singleCodeBlock) {
            // Merge all file outputs into one big code block
            val merged = allCodeBlocks.joinToString(separator = "\n\n") { it }
            if (finalOpts.minimizeWhitespace) {
                minimizeWhitespace(merged)
            } else merged
        } else {
            // Keep them separate
            val joined = allCodeBlocks.joinToString(separator = "\n\n")
            if (finalOpts.minimizeWhitespace) {
                minimizeWhitespace(joined)
            } else joined
        }

        // Export or copy or show preview
        if (finalOpts.exportToFile) {
            val path = if (finalOpts.exportFilePath.isNotEmpty()) finalOpts.exportFilePath
            else "$basePath/clipcraft_output.txt"
            try {
                File(path).writeText(finalCode, Charsets.UTF_8)
                ephemeralNotification("ClipCraft output saved to $path")
            } catch (ex: Exception) {
                ephemeralNotification("ClipCraft failed to export: ${ex.message}")
            }
        } else {
            // either show preview or copy
            if (finalOpts.showPreview) {
                // Use a non-modal dialog so we don't block
                Messages.showIdeaMessageDialog(
                    project,
                    finalCode,
                    "ClipCraft Preview",
                    arrayOf("OK"),
                    0,
                    null,
                    null
                )
            } else {
                // Copy to clipboard, ephemeral notify
                CopyPasteManager.getInstance().setContents(StringSelection(finalCode))
                ephemeralNotification("ClipCraft code copied to clipboard")
            }
        }
    }

    /**
     * Return a string containing code blocks from [file], or an empty string if none.
     */
    private fun processVirtualFile(
        file: VirtualFile,
        basePath: String,
        opts: ClipCraftOptions,
        project: Project?
    ): String {
        val sb = StringBuilder()
        if (file.isDirectory) {
            // Recursively process subdirs
            file.children.forEach { child ->
                val childBlock = processVirtualFile(child, basePath, opts, project)
                if (childBlock.isNotEmpty()) {
                    sb.append(childBlock).append("\n\n")
                }
            }
            return sb.toString().trim()
        } else {
            if (!isTextFile(file)) return ""
            val content = if (file.length > opts.largeFileThreshold) {
                loadFileWithProgress(file, project)
            } else {
                loadFileContent(file)
            }

            val relPath = if (basePath.isNotEmpty()) {
                Paths.get(basePath).relativize(Paths.get(file.path)).toString()
            } else file.path

            val header = buildString {
                append("File: ").append(relPath)
                if (opts.includeMetadata) {
                    append(" [Size: ").append(file.length)
                    append(" bytes, Last Modified: ").append(file.timeStamp).append("]")
                }
            }

            sb.append(header).append("\n```").append(detectLanguage(file)).append("\n")
            sb.append(processContent(content, opts)).append("\n```\n")
            return sb.toString().trim()
        }
    }

    /**
     * Basic text file check
     */
    private fun isTextFile(file: VirtualFile): Boolean {
        // If first 8k bytes contain a null char => binary
        val sample = file.contentsToByteArray().take(8000)
        return sample.none { it.toInt() == 0 }
    }

    /**
     * Read entire file in a read action
     */
    private fun loadFileContent(file: VirtualFile): String {
        return ReadAction.compute<String, Exception> {
            String(file.contentsToByteArray(), Charset.forName("UTF-8"))
        }
    }

    /**
     * Use a background task to read large files
     */
    private fun loadFileWithProgress(file: VirtualFile, project: Project?): String {
        var text = ""
        ProgressManager.getInstance().run(object : Task.Backgroundable(
            project,
            "Loading large file: ${file.name}",
            /* canBeCancelled = */ false
        ) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Loading large file..."
                text = loadFileContent(file)
            }
        })
        return text
    }

    /**
     * Attempt a naive detection for code fencing
     */
    private fun detectLanguage(file: VirtualFile): String {
        return when (file.extension?.lowercase()) {
            "java" -> "java"
            "kt" -> "kotlin"
            "py" -> "python"
            "js" -> "javascript"
            "ts" -> "typescript"
            "html" -> "html"
            "css" -> "css"
            "lua" -> "lua"
            "xml" -> "xml"
            "json" -> "json"
            "md" -> "markdown"
            else -> "txt"
        }
    }

    /**
     * Process file content: optional line numbers
     */
    fun processContent(content: String, opts: ClipCraftOptions): String {
        val lines = content.lines()
        val sb = StringBuilder()
        var lineNo = 1
        for (line in lines) {
            val finalLine = if (opts.includeLineNumbers) {
                String.format("%4d: %s", lineNo, line)
            } else line
            sb.append(finalLine).append("\n")
            lineNo++
        }
        return sb.toString().trimEnd()
    }

    /**
     * Remove extra blank lines or multiple consecutive blank lines
     */
    private fun minimizeWhitespace(text: String): String {
        // Basic approach: convert multiple blank lines into one blank line
        // More advanced approaches could trim spaces, etc.
        val lines = text.lines()
        val sb = StringBuilder()
        var blankCount = 0
        for (line in lines) {
            if (line.isBlank()) {
                blankCount++
                if (blankCount <= 1) {
                    sb.append("\n")
                }
            } else {
                blankCount = 0
                sb.append(line).append("\n")
            }
        }
        return sb.toString().trimEnd()
    }

    /**
     * Show ephemeral balloon notification
     */
    private fun ephemeralNotification(content: String) {
        val notification = Notification(
            "ClipCraft Notifications",
            "ClipCraft",
            content,
            NotificationType.INFORMATION
        )
        Notifications.Bus.notify(notification)
    }
}
