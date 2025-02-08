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

class ClipCraftAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        var opts = getOptions(project, e) ?: return

        // Apply theme according to settings.
        ClipCraftThemeManager.applyTheme(opts.themeMode)

        val selected = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        if (selected.isNullOrEmpty()) {
            ClipCraftNotificationCenter.notifyInfo("No files or directories selected.", project)
            return
        }

        val combinedContent = if (opts.measurePerformance) {
            ClipCraftPerformanceMetrics.measure("ClipCraft Processing") {
                processFiles(selected, opts, project)
            }
        } else {
            processFiles(selected, opts, project)
        }

        if (opts.simultaneousExports.isNotEmpty()) {
            val basePath = opts.exportFilePath.ifEmpty { (project?.basePath ?: "") + "/clipcraft_output" }
            val exportedPaths = ClipCraftSharingService.exportMultipleFormats(basePath, opts.simultaneousExports, combinedContent)
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

    private fun getOptions(project: Project?, e: AnActionEvent): ClipCraftOptions? {
        val settings = ClipCraftSettings.getInstance()
        var curOpts = settings.state

        if (curOpts.perProjectConfig && project != null) {
            val projService = ClipCraftProjectProfileManager.getInstance(project)
            curOpts = projService.state
        }

        curOpts = if (e.inputEvent is MouseEvent && (e.inputEvent as MouseEvent).isAltDown) {
            val quickPanel = ClipCraftQuickOptionsPanel(curOpts, project)
            val result = JOptionPane.showConfirmDialog(
                null,
                quickPanel,
                "Quick ClipCraft Options",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
            )
            if (result == JOptionPane.OK_OPTION) quickPanel.getOptions() else return null
        } else if (!curOpts.autoProcess) {
            val dialog = ClipCraftOptionsDialog(curOpts)
            if (!dialog.showAndGet()) return null else dialog.getOptions()
        } else {
            curOpts
        }

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
        val joined = blocks.joinToString(System.lineSeparator() + System.lineSeparator())
        return if (opts.minimizeWhitespace) minimizeWhitespace(joined) else joined
    }

    private fun processVirtualFile(file: VirtualFile, opts: ClipCraftOptions, project: Project?): String {
        if (opts.filterRegex.isNotBlank()) {
            if (!Regex(opts.filterRegex).containsMatchIn(file.path)) {
                return ""
            }
        }

        if (shouldIgnoreFile(file, opts)) return ""
        if (file.isDirectory) {
            return file.children.joinToString(System.lineSeparator()) { processVirtualFile(it, opts, project) }
        } else {
            if (!isTextFile(file)) return ""
            val content = loadFile(file, opts, project)
            val relPath = file.path
            val header = buildString {
                append("File: $relPath")
                if (opts.includeMetadata) {
                    append(" [Size: ${file.length} bytes, Modified: ${file.timeStamp}]")
                    if (opts.displayGitMetadata && project != null) {
                        val gitInfo = ClipCraftGitIntegration.getGitMetadata(project, file.path)
                        if (gitInfo.isNotEmpty()) append(" $gitInfo")
                    }
                }
            }
            val language = detectLanguage(file)
            val processed = processContent(content, opts, language)
            val formattedBlock = formatBlock(processed, language, opts.outputFormat)
            return "$header\n$formattedBlock"
        }
    }

    private fun shouldIgnoreFile(file: VirtualFile, opts: ClipCraftOptions): Boolean {
        if (file.isDirectory && opts.ignoreFolders.any { file.name.equals(it, ignoreCase = true) }) return true
        if (!file.isDirectory && opts.ignoreFiles.any { file.name.equals(it, ignoreCase = true) }) return true
        if (!file.isDirectory && opts.ignorePatterns.any { Regex(it).containsMatchIn(file.name) }) return true
        return false
    }

    private fun loadFile(file: VirtualFile, opts: ClipCraftOptions, project: Project?): String {
        return if (file.length > opts.largeFileThreshold && opts.showProgressInStatusBar) {
            loadFileWithProgress(file, project)
        } else {
            loadFileContent(file)
        }
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
                text = loadFileContent(file)
            }
        })
        return text
    }

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

    fun processContent(text: String, opts: ClipCraftOptions, language: String): String {
        val sb = StringBuilder()
        var lineNum = 1
        for (line in text.lines()) {
            val processedLine = if (opts.trimLineWhitespace) line.trimEnd() else line
            val numberedLine = if (opts.includeLineNumbers) "%4d: %s".format(lineNum, processedLine) else processedLine
            sb.append(numberedLine).append(System.lineSeparator())
            lineNum++
        }
        var processed = sb.toString().trimEnd()
        if (opts.removeComments) processed = removeComments(processed, language)
        if (opts.removeImports) processed = removeImportStatements(processed, language)
        processed = ClipCraftMacroManager.applyMacros(processed, opts.macros)
        return processed
    }

    private fun removeComments(text: String, language: String): String {
        return when (language) {
            "java", "kotlin", "javascript", "typescript", "csharp", "cpp" -> {
                val noBlock = text.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
                noBlock.lines().filter { !it.trim().startsWith("//") }.joinToString(System.lineSeparator())
            }
            "python", "ruby", "sh", "bash" -> {
                text.lines().filter { !it.trim().startsWith("#") }.joinToString(System.lineSeparator())
            }
            else -> text
        }
    }

    private fun removeImportStatements(text: String, language: String): String {
        return when (language) {
            "java", "kotlin", "javascript", "typescript", "csharp", "cpp" -> {
                text.lines().filter { !it.trim().startsWith("import ") }.joinToString(System.lineSeparator())
            }
            "python" -> {
                text.lines().filter {
                    !it.trim().startsWith("import ") && !it.trim().startsWith("from ")
                }.joinToString(System.lineSeparator())
            }
            else -> text
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
            if (line.isBlank() && result.lastOrNull()?.isBlank() == true) return@forEach
            result += line
        }
        return result.joinToString(System.lineSeparator())
    }

    private fun isTextFile(file: VirtualFile): Boolean {
        val sample = file.contentsToByteArray().take(8000)
        return sample.none { it.toInt() == 0 }
    }
}
