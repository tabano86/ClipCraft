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
import com.intellij.openapi.vfs.VirtualFile
import java.awt.datatransfer.StringSelection
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Paths
import javax.swing.JOptionPane

class ClipCraftAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val selected = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        if (selected.isNullOrEmpty()) {
            ephemeralNotification("No files or directories selected.", project)
            return
        }

        val settings = ClipCraftSettings.getInstance()
        val curOpts = settings.state

        // If autoProcess = false, show a dialog
        val finalOpts = if (!curOpts.autoProcess) {
            val dialog = ClipCraftOptionsDialog(curOpts)
            if (!dialog.showAndGet()) return else dialog.getOptions()
        } else {
            curOpts
        }

        // Save any changes from the dialog
        settings.loadState(finalOpts)

        val basePath = project?.basePath ?: ""
        val blocks = mutableListOf<String>()

        // For each selected file or directory, process it
        selected.forEach { vf ->
            val block = processVirtualFile(vf, basePath, finalOpts, project)
            if (block.isNotEmpty()) blocks += block
        }

        // Combine them based on user prefs
        val combined = if (finalOpts.singleCodeBlock) {
            val merged = blocks.joinToString("\n\n")
            if (finalOpts.minimizeWhitespace) minimizeWhitespace(merged) else merged
        } else {
            val joined = blocks.joinToString("\n\n")
            if (finalOpts.minimizeWhitespace) minimizeWhitespace(joined) else joined
        }

        if (finalOpts.exportToFile) {
            val outPath = if (finalOpts.exportFilePath.isNotEmpty()) finalOpts.exportFilePath
            else "$basePath/clipcraft_output.txt"
            try {
                File(outPath).writeText(combined, Charsets.UTF_8)
                ephemeralNotification("Output exported to $outPath", project)
            } catch (ex: Exception) {
                ephemeralNotification("Export failed: ${ex.message}", project)
            }
        } else {
            if (finalOpts.showPreview) {
                JOptionPane.showMessageDialog(null, combined, "ClipCraft Preview", JOptionPane.INFORMATION_MESSAGE)
            } else {
                CopyPasteManager.getInstance().setContents(StringSelection(combined))
                ephemeralNotification("Code copied to clipboard.", project)
            }
        }
    }

    private fun processVirtualFile(
        file: VirtualFile,
        basePath: String,
        opts: ClipCraftOptions,
        project: Project?
    ): String {
        if (file.isDirectory) {
            // Recursively process subdirectories
            return file.children.joinToString("\n\n") {
                processVirtualFile(it, basePath, opts, project)
            }
        } else {
            // Check if textual
            if (!isTextFile(file)) return ""
            val content = if (file.length > opts.largeFileThreshold) {
                loadFileWithProgress(file, project)
            } else {
                loadFileContent(file)
            }
            // Build the final snippet
            val relPath = if (basePath.isNotEmpty())
                Paths.get(basePath).relativize(Paths.get(file.path)).toString() else file.path

            val header = buildString {
                append("File: $relPath")
                if (opts.includeMetadata) {
                    append(" [Size: ${file.length} bytes, Last Modified: ${file.timeStamp}]")
                }
            }
            return """
                $header
                ```${detectLanguage(file)}
                ${processContent(content, opts)}
                ```
            """.trimIndent()
        }
    }

    private fun isTextFile(file: VirtualFile): Boolean {
        val sample = file.contentsToByteArray().take(8000)
        return sample.none { it.toInt() == 0 }
    }

    private fun loadFileContent(file: VirtualFile): String {
        return ReadAction.compute<String, Exception> {
            String(file.contentsToByteArray(), Charset.forName("UTF-8"))
        }
    }

    private fun loadFileWithProgress(file: VirtualFile, project: Project?): String {
        var text = ""
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Loading large file: ${file.name}", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Reading ${file.name}..."
                text = loadFileContent(file)
            }
        })
        return text
    }

    private fun detectLanguage(file: VirtualFile): String {
        return when (file.extension?.lowercase()) {
            "java" -> "java"
            "kt"   -> "kotlin"
            "py"   -> "python"
            "js"   -> "javascript"
            "ts"   -> "typescript"
            "html" -> "html"
            "css"  -> "css"
            "lua"  -> "lua"
            "xml"  -> "xml"
            "json" -> "json"
            "md"   -> "markdown"
            else   -> "txt"
        }
    }

    fun processContent(text: String, opts: ClipCraftOptions): String {
        val sb = StringBuilder()
        var lineNum = 1
        for (line in text.lines()) {
            val finalLine = if (opts.includeLineNumbers) {
                "%4d: %s".format(lineNum, line)
            } else line
            sb.append(finalLine).append("\n")
            lineNum++
        }
        return sb.toString().trimEnd()
    }

    private fun minimizeWhitespace(input: String): String {
        // Remove consecutive blank lines
        val lines = input.lines()
        val result = mutableListOf<String>()
        for (line in lines) {
            if (line.isBlank() && result.lastOrNull()?.isBlank() == true) {
                continue
            }
            result += line
        }
        return result.joinToString("\n")
    }

    private fun ephemeralNotification(msg: String, project: Project?) {
        val n = Notification("ClipCraft", "ClipCraft", msg, NotificationType.INFORMATION)
        Notifications.Bus.notify(n, project)
    }
}
