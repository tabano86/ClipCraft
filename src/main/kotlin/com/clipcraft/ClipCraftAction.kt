package com.clipcraft

import com.clipcraft.model.ClipCraftOptions
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
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
            Messages.showErrorDialog("No files or directories selected.", "ClipCraft")
            return
        }

        // Load saved settings.
        val settings = ClipCraftSettings.getInstance()
        val currentOpts = settings.state

        // If autoProcess is disabled, show the dialog.
        val finalOpts = if (!currentOpts.autoProcess) {
            val dialog = ClipCraftOptionsDialog(currentOpts)
            if (!dialog.showAndGet()) return else dialog.getOptions()
        } else currentOpts

        // Save any changes from the dialog.
        settings.loadState(finalOpts)

        val basePath = project?.basePath ?: ""
        val sb = StringBuilder()

        // Process each selected file/folder.
        for (f in selected) {
            processVirtualFile(f, basePath, sb, finalOpts, project)
        }

        val outputText = sb.toString()

        if (finalOpts.exportToFile) {
            val outPath = if (finalOpts.exportFilePath.isNotEmpty())
                finalOpts.exportFilePath
            else "$basePath/clipcraft_output.txt"

            try {
                File(outPath).writeText(outputText, Charsets.UTF_8)
                Messages.showInfoMessage("Output exported to $outPath", "ClipCraft")
            } catch (ex: Exception) {
                Messages.showErrorDialog("Failed to export: ${ex.message}", "ClipCraft")
            }
        } else {
            // either show preview or copy to clipboard
            if (finalOpts.showPreview) {
                Messages.showInfoMessage(outputText, "ClipCraft Preview")
            } else {
                CopyPasteManager.getInstance().setContents(StringSelection(outputText))
                Messages.showInfoMessage("Code copied to clipboard.", "ClipCraft")
            }
        }
    }

    private fun processVirtualFile(
        file: VirtualFile,
        basePath: String,
        sb: StringBuilder,
        opts: ClipCraftOptions,
        project: Project?
    ) {
        if (file.isDirectory) {
            // Recursively process subdirectories
            for (child in file.children) {
                processVirtualFile(child, basePath, sb, opts, project)
            }
        } else {
            if (!isTextFile(file)) return
            val content = if (file.length > opts.largeFileThreshold) {
                loadFileWithProgress(file, project)
            } else {
                loadFileContent(file)
            }
            val relPath = if (basePath.isNotEmpty()) {
                Paths.get(basePath).relativize(Paths.get(file.path)).toString()
            } else file.path

            sb.append("File: ").append(relPath)
            if (opts.includeMetadata) {
                sb.append(" [Size: ").append(file.length)
                    .append(" bytes, Last Modified: ").append(file.timeStamp).append("]")
            }
            sb.append("\n```").append(detectLanguage(file)).append("\n")
            sb.append(processContent(content, opts))
            sb.append("\n```\n\n")
        }
    }

    private fun isTextFile(file: VirtualFile): Boolean {
        // Quick check: if first 8k bytes contain a null, assume binary.
        val sample = file.contentsToByteArray().take(8000)
        return sample.none { it.toInt() == 0 }
    }

    private fun loadFileContent(file: VirtualFile): String {
        // Safely read file content in a read action
        return ReadAction.compute<String, Exception> {
            String(file.contentsToByteArray(), Charset.forName("UTF-8"))
        }
    }

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

    fun processContent(content: String, opts: ClipCraftOptions): String {
        val sb = StringBuilder()
        var lineNumber = 1
        for (line in content.lines()) {
            val finalLine = if (opts.includeLineNumbers) {
                String.format("%4d: %s", lineNumber, line)
            } else line
            sb.append(finalLine).append("\n")
            lineNumber++
        }
        return sb.toString().trimEnd()
    }
}
