package com.clipcraft

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.OutputFormat
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
import java.awt.event.MouseEvent
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Paths
import javax.swing.JOptionPane

/**
 * Main action that collects and formats code from selected files.
 *
 * New features include:
 * • Optionally removing import statements.
 * • Multiple output formats (Markdown, Plain Text, HTML).
 * • Additional compatibility for more languages.
 * • Availability in many context menus (including editor tab right-click).
 */
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

        // Quick options override: if Alt is held, show the quick options panel.
        curOpts = if (e.inputEvent is MouseEvent && (e.inputEvent as MouseEvent).isAltDown) {
            val quickPanel = ClipCraftQuickOptionsPanel(curOpts, project)
            val result = JOptionPane.showConfirmDialog(
                null,
                quickPanel,
                "Quick ClipCraft Options",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
            )
            if (result == JOptionPane.OK_OPTION) quickPanel.getOptions() else return
        } else if (!curOpts.autoProcess) {
            // Otherwise, if autoProcess is disabled, show the full options dialog.
            val dialog = ClipCraftOptionsDialog(curOpts)
            if (!dialog.showAndGet()) return else dialog.getOptions()
        } else {
            curOpts
        }

        // Persist any changes.
        settings.loadState(curOpts)

        val basePath = project?.basePath ?: ""
        val blocks = mutableListOf<String>()

        // Process each selected file or directory.
        selected.forEach { vf ->
            val block = processVirtualFile(vf, basePath, curOpts, project)
            if (block.isNotEmpty()) blocks += block
        }

        // Combine blocks per user settings.
        val combined = if (curOpts.singleCodeBlock) {
            val merged = blocks.joinToString(System.lineSeparator() + System.lineSeparator())
            if (curOpts.minimizeWhitespace) minimizeWhitespace(merged) else merged
        } else {
            val joined = blocks.joinToString(System.lineSeparator() + System.lineSeparator())
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

    /**
     * Processes a VirtualFile recursively.
     * For files, applies ignore rules and formatting options (comment/import removal, etc.)
     * then wraps the code block according to the chosen output format.
     */
    private fun processVirtualFile(
        file: VirtualFile,
        basePath: String,
        opts: ClipCraftOptions,
        project: Project?
    ): String {
        if (shouldIgnoreFile(file, opts)) return ""
        if (file.isDirectory) {
            return file.children.joinToString(System.lineSeparator() + System.lineSeparator()) {
                processVirtualFile(it, basePath, opts, project)
            }
        } else {
            if (!isTextFile(file)) return ""
            val content = if (file.length > opts.largeFileThreshold) {
                loadFileWithProgress(file, project)
            } else {
                loadFileContent(file)
            }
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
            val formattedBlock = when (opts.outputFormat) {
                OutputFormat.MARKDOWN -> "```$language\n$processedContent\n```"
                OutputFormat.PLAIN -> processedContent
                OutputFormat.HTML -> "<pre><code class=\"$language\">$processedContent</code></pre>"
            }
            return "$header\n$formattedBlock"
        }
    }

    /**
     * Checks whether a file (or folder) should be skipped per ignore rules.
     */
    private fun shouldIgnoreFile(file: VirtualFile, opts: ClipCraftOptions): Boolean {
        if (file.isDirectory && opts.ignoreFolders.any { file.name.equals(it, ignoreCase = true) }) return true
        if (!file.isDirectory && opts.ignoreFiles.any { file.name.equals(it, ignoreCase = true) }) return true
        if (!file.isDirectory && opts.ignorePatterns.any { Regex(it).containsMatchIn(file.name) }) return true
        return false
    }

    /**
     * Determines if a file is textual.
     */
    private fun isTextFile(file: VirtualFile): Boolean {
        val sample = file.contentsToByteArray().take(8000)
        return sample.none { it.toInt() == 0 }
    }

    private fun loadFileContent(file: VirtualFile): String =
        ReadAction.compute<String, Exception> {
            String(file.contentsToByteArray(), Charset.forName("UTF-8"))
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

    /**
     * Uses file extension to determine language for syntax highlighting.
     */
    private fun detectLanguage(file: VirtualFile): String = when (file.extension?.lowercase()) {
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
        "c", "cpp" -> "cpp"
        "cs" -> "csharp"
        else -> "txt"
    }

    /**
     * Processes file content: applies line numbering, optional trimming,
     * comment removal, and import removal.
     */
    fun processContent(text: String, opts: ClipCraftOptions, language: String): String {
        val newline = System.lineSeparator()
        val sb = StringBuilder()
        var lineNum = 1
        for (line in text.lines()) {
            val processedLine = if (opts.trimLineWhitespace) line.trimEnd() else line
            val numberedLine = if (opts.includeLineNumbers) {
                "%4d: %s".format(lineNum, processedLine)
            } else processedLine
            sb.append(numberedLine).append(newline)
            lineNum++
        }
        var processed = sb.toString().trimEnd()
        if (opts.removeComments) {
            processed = removeComments(processed, language)
        }
        if (opts.removeImports) {
            processed = removeImportStatements(processed, language)
        }
        return processed
    }

    /**
     * Removes comments using language-specific rules.
     */
    private fun removeComments(text: String, language: String): String = when (language) {
        "java", "kotlin", "javascript", "typescript", "csharp", "cpp" -> {
            val noBlockComments = text.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
            noBlockComments.lines().filter { !it.trim().startsWith("//") }.joinToString(System.lineSeparator())
        }
        "python", "ruby", "sh", "bash" -> {
            text.lines().filter { !it.trim().startsWith("#") }.joinToString(System.lineSeparator())
        }
        else -> text
    }

    /**
     * Removes import statements based on language.
     */
    private fun removeImportStatements(text: String, language: String): String {
        return when (language) {
            "java", "kotlin", "javascript", "typescript", "csharp", "cpp" -> {
                text.lines().filter { !it.trim().startsWith("import ") }.joinToString(System.lineSeparator())
            }

            "python" -> {
                text.lines().filter { !it.trim().startsWith("import ") && !it.trim().startsWith("from ") }
                    .joinToString(System.lineSeparator())
            }

            else -> text
        }
    }

    /**
     * Collapses consecutive blank lines.
     */
    private fun minimizeWhitespace(input: String): String {
        val result = mutableListOf<String>()
        input.lines().forEach { line ->
            if (line.isBlank() && result.lastOrNull()?.isBlank() == true) return@forEach
            result += line
        }
        return result.joinToString(System.lineSeparator())
    }

    private fun ephemeralNotification(msg: String, project: Project?) {
        val n = Notification("ClipCraft", "ClipCraft", msg, NotificationType.INFORMATION)
        Notifications.Bus.notify(n, project)
    }
}
