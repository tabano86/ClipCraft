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
import java.awt.event.MouseEvent

class ClipCraftAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val selected = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        if (selected.isNullOrEmpty()) {
            ephemeralNotification("No files or directories selected.", project)
            return
        }

        val settings = ClipCraftSettings.getInstance()
        var curOpts = settings.state

        // Quick options override: if Alt is held, show the quick options panel near the click.
        curOpts = if (e.inputEvent is MouseEvent && (e.inputEvent as MouseEvent).isAltDown) {
            val quickPanel = ClipCraftQuickOptionsPanel(curOpts)
            val result = JOptionPane.showConfirmDialog(
                null,
                quickPanel,
                "Quick ClipCraft Options",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
            )
            if (result == JOptionPane.OK_OPTION) quickPanel.getOptions() else return
        } else if (!curOpts.autoProcess) {
            // Show full options dialog if autoProcess is disabled.
            val dialog = ClipCraftOptionsDialog(curOpts)
            if (!dialog.showAndGet()) return else dialog.getOptions()
        } else {
            curOpts
        }

        // Save any changes from the dialog/quick options.
        settings.loadState(curOpts)

        val basePath = project?.basePath ?: ""
        val blocks = mutableListOf<String>()

        // Process each selected file or directory.
        selected.forEach { vf ->
            val block = processVirtualFile(vf, basePath, curOpts, project)
            if (block.isNotEmpty()) blocks += block
        }

        // Combine the blocks based on user preferences.
        val combined = if (curOpts.singleCodeBlock) {
            val merged = blocks.joinToString("\n\n")
            if (curOpts.minimizeWhitespace) minimizeWhitespace(merged) else merged
        } else {
            val joined = blocks.joinToString("\n\n")
            if (curOpts.minimizeWhitespace) minimizeWhitespace(joined) else joined
        }

        if (curOpts.exportToFile) {
            val outPath = if (curOpts.exportFilePath.isNotEmpty()) curOpts.exportFilePath
            else "$basePath/clipcraft_output.txt"
            try {
                File(outPath).writeText(combined, Charsets.UTF_8)
                ephemeralNotification("Output exported to $outPath", project)
            } catch (ex: Exception) {
                ephemeralNotification("Export failed: ${ex.message}", project)
            }
        } else {
            if (curOpts.showPreview) {
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
        if (shouldIgnoreFile(file, opts)) return ""
        if (file.isDirectory) {
            // Recursively process subdirectories.
            return file.children.joinToString("\n\n") {
                processVirtualFile(it, basePath, opts, project)
            }
        } else {
            // Process only textual files.
            if (!isTextFile(file)) return ""
            val content = if (file.length > opts.largeFileThreshold) {
                loadFileWithProgress(file, project)
            } else {
                loadFileContent(file)
            }
            // Build the snippet header.
            val relPath = if (basePath.isNotEmpty())
                Paths.get(basePath).relativize(Paths.get(file.path)).toString() else file.path

            val header = buildString {
                append("File: $relPath")
                if (opts.includeMetadata) {
                    append(" [Size: ${file.length} bytes, Last Modified: ${file.timeStamp}]")
                }
            }
            val language = detectLanguage(file)
            val processedContent = processContent(content, opts, language)
            return """
                $header
                ```$language
                $processedContent
                ```
            """.trimIndent()
        }
    }

    private fun shouldIgnoreFile(file: VirtualFile, opts: ClipCraftOptions): Boolean {
        // Ignore directories based on folder names.
        if (file.isDirectory && opts.ignoreFolders.any { file.name.equals(it, ignoreCase = true) }) return true
        // Ignore files based on exact names.
        if (!file.isDirectory && opts.ignoreFiles.any { file.name.equals(it, ignoreCase = true) }) return true
        // Ignore files based on regex patterns.
        if (!file.isDirectory && opts.ignorePatterns.any { Regex(it).containsMatchIn(file.name) }) return true
        return false
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

    fun processContent(text: String, opts: ClipCraftOptions, language: String): String {
        val sb = StringBuilder()
        var lineNum = 1
        for (line in text.lines()) {
            val finalLine = if (opts.includeLineNumbers) {
                "%4d: %s".format(lineNum, line)
            } else line
            sb.append(finalLine).append("\n")
            lineNum++
        }
        var processed = sb.toString().trimEnd()

        // Remove comments if enabled.
        if (opts.removeComments) {
            processed = removeComments(processed, language)
        }
        // Trim whitespace on each line if enabled.
        if (opts.trimLineWhitespace) {
            processed = processed.lines().joinToString("\n") { it.trim() }
        }
        return processed
    }

    private fun removeComments(text: String, language: String): String {
        return when (language) {
            "java", "kotlin", "javascript", "typescript" -> {
                // Remove block comments.
                val noBlockComments = text.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
                // Remove line comments starting with //
                noBlockComments.lines().filter { !it.trim().startsWith("//") }.joinToString("\n")
            }
            "python", "ruby", "sh", "bash" -> {
                text.lines().filter { !it.trim().startsWith("#") }.joinToString("\n")
            }
            else -> text
        }
    }

    private fun minimizeWhitespace(input: String): String {
        // Remove consecutive blank lines.
        val lines = input.lines()
        val result = mutableListOf<String>()
        for (line in lines) {
            if (line.isBlank() && result.lastOrNull()?.isBlank() == true) continue
            result += line
        }
        return result.joinToString("\n")
    }

    private fun ephemeralNotification(msg: String, project: Project?) {
        val n = Notification("ClipCraft", "ClipCraft", msg, NotificationType.INFORMATION)
        Notifications.Bus.notify(n, project)
    }
}
