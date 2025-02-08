package com.clipcraft.actions

import com.clipcraft.integration.ClipCraftGitIntegration
import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.OutputFormat
import com.clipcraft.services.*
import com.clipcraft.ui.ClipCraftOptionsDialog
import com.clipcraft.ui.ClipCraftQuickOptionsPanel
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
import javax.swing.JOptionPane

class ClipCraftAction : AnAction("ClipCraft: Copy Formatted Code") {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val opts = obtainOptions(project, e)
        // opts is guaranteed non-null
        ClipCraftThemeManager.applyTheme(opts.themeMode)

        val selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        if (selectedFiles.isNullOrEmpty()) {
            ClipCraftNotificationCenter.notifyInfo("No files or directories selected.", project)
            return
        }

        val combinedContent = if (opts.measurePerformance) {
            ClipCraftPerformanceMetrics.measure("ClipCraft Processing") {
                processFiles(selectedFiles, opts, project)
            }
        } else {
            processFiles(selectedFiles, opts, project)
        }

        handleOutput(combinedContent, opts, project)
    }

    private fun obtainOptions(project: Project?, e: AnActionEvent): ClipCraftOptions {
        val settings = ClipCraftSettings.getInstance()
        var curOpts = settings.state

        // Use per-project config if enabled.
        if (curOpts.perProjectConfig && project != null) {
            curOpts = ClipCraftProjectProfileManager.getInstance(project).state
        }

        // If ALT is held, show quick options.
        curOpts = if (e.inputEvent is MouseEvent && (e.inputEvent as MouseEvent).isAltDown) {
            val quickPanel = ClipCraftQuickOptionsPanel(curOpts, project)
            val result = JOptionPane.showConfirmDialog(
                null,
                quickPanel,
                "Quick ClipCraft Options",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
            )
            if (result == JOptionPane.OK_OPTION) quickPanel.getOptions() else curOpts
        }
        // If autoProcess is off, show the full options dialog.
        else if (!curOpts.autoProcess) {
            val dialog = ClipCraftOptionsDialog(curOpts)
            if (dialog.showAndGet()) dialog.getOptions() else curOpts
        } else {
            curOpts
        }

        // Save the updated settings.
        settings.loadState(curOpts)
        if (curOpts.perProjectConfig && project != null) {
            ClipCraftProjectProfileManager.getInstance(project).loadState(curOpts)
        }
        return curOpts
    }

    private fun processFiles(files: Array<VirtualFile>, opts: ClipCraftOptions, project: Project?): String {
        val blocks = mutableListOf<String>()
        for (vf in files) {
            val block = processVirtualFile(vf, opts, project)
            if (block.isNotEmpty()) blocks += block
        }
        // Join blocks with two line separators.
        val joined = blocks.joinToString(separator = System.lineSeparator().repeat(2))
        return if (opts.minimizeWhitespace) minimizeWhitespace(joined) else joined
    }

    private fun handleOutput(combinedContent: String, opts: ClipCraftOptions, project: Project?) {
        if (opts.simultaneousExports.isNotEmpty()) {
            val basePath = opts.exportFilePath.ifEmpty { (project?.basePath ?: "") + "/clipcraft_output" }
            val exportedPaths =
                ClipCraftSharingService.exportMultipleFormats(basePath, opts.simultaneousExports, combinedContent)
            ClipCraftNotificationCenter.notifyInfo("Simultaneously exported: ${exportedPaths.joinToString()}", project)
        } else if (opts.exportToFile) {
            val outPath = if (opts.exportFilePath.isNotEmpty()) opts.exportFilePath
            else (project?.basePath ?: "") + "/clipcraft_output.txt"
            try {
                File(outPath).writeText(combinedContent, Charsets.UTF_8)
                ClipCraftNotificationCenter.notifyInfo("Output exported to $outPath", project)
            } catch (ex: Exception) {
                ClipCraftNotificationCenter.notifyError("Export failed: ${ex.message}", project)
            }
        } else {
            CopyPasteManager.getInstance().setContents(StringSelection(combinedContent))
            ClipCraftNotificationCenter.notifyInfo("Code copied to clipboard.", project)
        }

        if (opts.shareToGistEnabled) {
            val success = ClipCraftSharingService.shareToGist(combinedContent, project)
            if (success) ClipCraftNotificationCenter.notifyInfo("Shared to Gist!", project)
            else ClipCraftNotificationCenter.notifyError("Failed to share to Gist", project)
        }
        if (opts.exportToCloudServices) {
            ClipCraftSharingService.exportToCloud(combinedContent, "GoogleDrive", project)
        }
    }

    private fun processVirtualFile(file: VirtualFile, opts: ClipCraftOptions, project: Project?): String {
        if (opts.filterRegex.isNotBlank() && !Regex(opts.filterRegex).containsMatchIn(file.path)) {
            return ""
        }

        if (shouldIgnore(file, opts)) return ""

        return if (file.isDirectory) {
            file.children.joinToString(separator = System.lineSeparator()) { processVirtualFile(it, opts, project) }
        } else {
            if (!isTextFile(file)) return ""
            val content = loadFileContent(file, opts, project)
            buildFileBlock(content, file, opts, project)
        }
    }

    private fun shouldIgnore(file: VirtualFile, opts: ClipCraftOptions): Boolean {
        return if (file.isDirectory) {
            opts.ignoreFolders.any { file.name.equals(it, ignoreCase = true) }
        } else {
            opts.ignoreFiles.any { file.name.equals(it, ignoreCase = true) } ||
                    opts.ignorePatterns.any { Regex(it).containsMatchIn(file.name) }
        }
    }

    private fun loadFileContent(file: VirtualFile, opts: ClipCraftOptions, project: Project?): String {
        return if (file.length > opts.largeFileThreshold && opts.showProgressInStatusBar) {
            loadFileWithProgress(file, project)
        } else {
            readFileBytes(file)
        }
    }

    private fun readFileBytes(file: VirtualFile): String {
        return ReadAction.compute<String, Exception> {
            String(file.contentsToByteArray(), Charset.forName("UTF-8"))
        }
    }

    private fun loadFileWithProgress(file: VirtualFile, project: Project?): String {
        var text = ""
        ProgressManager.getInstance()
            .run(object : Task.Backgroundable(project, "Loading large file: ${file.name}", false) {
                override fun run(indicator: ProgressIndicator) {
                    text = readFileBytes(file)
                }
            })
        return text
    }

    private fun buildFileBlock(content: String, file: VirtualFile, opts: ClipCraftOptions, project: Project?): String {
        val header = buildString {
            append("File: ${file.path}")
            if (opts.includeMetadata) {
                append(" [Size: ${file.length} bytes, Modified: ${file.timeStamp}]")
                if (opts.displayGitMetadata && project != null) {
                    val gitInfo = ClipCraftGitIntegration.getGitMetadata(project, file.path)
                    if (gitInfo.isNotEmpty()) append(" $gitInfo")
                }
            }
        }
        val language = detectLanguage(file)
        val processedContent = processContent(content, opts, language)
        val formattedBlock = formatBlock(processedContent, language, opts.outputFormat)
        return "$header\n$formattedBlock"
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
            "c", "cpp" -> "cpp"
            "cs" -> "csharp"
            else -> "txt"
        }
    }

    fun processContent(text: String, opts: ClipCraftOptions, language: String): String {
        val builder = StringBuilder()
        var lineNumber = 1
        text.lines().forEach { line ->
            val trimmedLine = if (opts.trimLineWhitespace) line.trimEnd() else line
            val finalLine = if (opts.includeLineNumbers) "%4d: %s".format(lineNumber, trimmedLine) else trimmedLine
            builder.append(finalLine).append(System.lineSeparator())
            lineNumber++
        }
        var processed = builder.toString().trimEnd()
        if (opts.removeComments) processed = removeComments(processed, language)
        if (opts.removeImports) processed = removeImports(processed, language)
        processed = ClipCraftMacroManager.applyMacros(processed, opts.macros)
        return processed
    }

    private fun removeComments(input: String, language: String): String {
        return when (language) {
            "java", "kotlin", "javascript", "typescript", "csharp", "cpp" -> {
                val noBlockComments = input.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
                noBlockComments.lines().filter { !it.trim().startsWith("//") }.joinToString(System.lineSeparator())
            }

            "python", "ruby", "sh", "bash" -> {
                input.lines().filter { !it.trim().startsWith("#") }.joinToString(System.lineSeparator())
            }

            else -> input
        }
    }

    private fun removeImports(input: String, language: String): String {
        return when (language) {
            "java", "kotlin", "javascript", "typescript", "csharp", "cpp" -> {
                input.lines().filter { !it.trim().startsWith("import ") }.joinToString(System.lineSeparator())
            }

            "python" -> {
                input.lines().filter { !it.trim().startsWith("import ") && !it.trim().startsWith("from ") }
                    .joinToString(System.lineSeparator())
            }

            else -> input
        }
    }

    private fun formatBlock(content: String, language: String, format: OutputFormat): String {
        return when (format) {
            OutputFormat.MARKDOWN -> "```$language\n$content\n```"
            OutputFormat.PLAIN -> content
            OutputFormat.HTML -> "<pre><code class=\"$language\">$content</code></pre>"
        }
    }

    private fun minimizeWhitespace(input: String): String {
        val result = mutableListOf<String>()
        input.lines().forEach { line ->
            if (line.isBlank() && result.lastOrNull()?.isBlank() == true) {
                // Skip duplicate blank lines.
            } else {
                result += line
            }
        }
        return result.joinToString(System.lineSeparator())
    }

    private fun isTextFile(file: VirtualFile): Boolean {
        val sample = file.contentsToByteArray().take(8000)
        return sample.none { it.toInt() == 0 }
    }
}
