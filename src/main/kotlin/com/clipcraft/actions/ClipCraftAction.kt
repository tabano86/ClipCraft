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
import java.util.concurrent.Executors
import javax.swing.JOptionPane

class ClipCraftAction : AnAction("ClipCraft: Copy Formatted Code") {

    private val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val settings = ClipCraftSettings.getInstance()
        var opts = settings.getActiveOptions()

        if (opts.perProjectConfig && project != null) {
            opts = ClipCraftProjectProfileManager.getInstance(project).state
        }

        if (e.inputEvent is MouseEvent && (e.inputEvent as MouseEvent).isAltDown) {
            val quickPanel = ClipCraftQuickOptionsPanel(opts, project)
            val result = JOptionPane.showConfirmDialog(
                null,
                quickPanel,
                "Quick ClipCraft Options",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
            )
            if (result == JOptionPane.OK_OPTION) {
                opts = quickPanel.getOptions()
            }
        } else if (!opts.autoProcess) {
            val dialog = ClipCraftOptionsDialog(opts)
            if (dialog.showAndGet()) {
                opts = dialog.getOptions()
            }
        }

        settings.saveProfile(settings.state.activeProfileName, opts)
        if (opts.perProjectConfig && project != null) {
            ClipCraftProjectProfileManager.getInstance(project).loadState(opts)
        }

        val selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        if (selectedFiles.isNullOrEmpty()) {
            ClipCraftNotificationCenter.notifyInfo("No files or directories selected.", project)
            return
        }

        val finalContent = if (opts.measurePerformance) {
            ClipCraftPerformanceMetrics.measure("ClipCraft Processing") {
                processAllFiles(selectedFiles, opts, project)
            }
        } else {
            processAllFiles(selectedFiles, opts, project)
        }

        handleOutput(finalContent, opts, project, selectedFiles)
    }

    private fun processAllFiles(files: Array<VirtualFile>, opts: ClipCraftOptions, project: Project?): String {
        val tasks = files.map { vf ->
            executor.submit<String> {
                processVirtualFile(vf, opts, project)
            }
        }
        val blocks = tasks.map { it.get() }.filter { it.isNotEmpty() }
        val joined = blocks.joinToString(separator = System.lineSeparator().repeat(2))
        return if (opts.minimizeWhitespace) minimizeWhitespace(joined) else joined
    }

    private fun handleOutput(
        combinedContent: String,
        opts: ClipCraftOptions,
        project: Project?,
        selectedFiles: Array<VirtualFile>
    ) {
        var finalContent = combinedContent

        if (opts.includeDirectorySummary) {
            val summary = buildDirectorySummary(selectedFiles, opts)
            finalContent = "$summary\n\n$finalContent"
        }

        if (opts.enableChunkingForGPT) {
            val chunks = chunkContent(finalContent, opts.maxChunkSize)
            chunks.forEachIndexed { index, chunk ->
                if (opts.exportToFile) {
                    val outBase = if (opts.exportFilePath.isNotEmpty()) opts.exportFilePath
                    else (project?.basePath ?: "") + "/clipcraft_output_chunk${index + 1}.txt"
                    try {
                        File(outBase).writeText(chunk, Charsets.UTF_8)
                    } catch (ex: Exception) {
                        ClipCraftNotificationCenter.notifyError("Export failed: ${ex.message}", project)
                    }
                } else {
                    if (index == chunks.size - 1) {
                        CopyPasteManager.getInstance().setContents(StringSelection(chunk))
                    }
                }
            }
            ClipCraftNotificationCenter.notifyInfo("Output was split into ${chunks.size} chunks.", project)
        } else {
            if (opts.simultaneousExports.isNotEmpty()) {
                val basePath = opts.exportFilePath.ifEmpty { (project?.basePath ?: "") + "/clipcraft_output" }
                val exportedPaths =
                    ClipCraftSharingService.exportMultipleFormats(basePath, opts.simultaneousExports, finalContent)
                ClipCraftNotificationCenter.notifyInfo("Simultaneously exported: ${exportedPaths.joinToString()}", project)
            } else if (opts.exportToFile) {
                val outPath = if (opts.exportFilePath.isNotEmpty()) {
                    opts.exportFilePath
                } else {
                    (project?.basePath ?: "") + "/clipcraft_output.txt"
                }
                try {
                    File(outPath).writeText(finalContent, Charsets.UTF_8)
                    ClipCraftNotificationCenter.notifyInfo("Output exported to $outPath", project)
                } catch (ex: Exception) {
                    ClipCraftNotificationCenter.notifyError("Export failed: ${ex.message}", project)
                }
            } else {
                CopyPasteManager.getInstance().setContents(StringSelection(finalContent))
                ClipCraftNotificationCenter.notifyInfo("Code copied to clipboard.", project)
            }
        }

        if (opts.shareToGistEnabled) {
            val success = ClipCraftSharingService.shareToGist(finalContent, project)
            if (success) {
                ClipCraftNotificationCenter.notifyInfo("Shared to Gist!", project)
            } else {
                ClipCraftNotificationCenter.notifyError("Failed to share to Gist", project)
            }
        }
        if (opts.exportToCloudServices) {
            ClipCraftSharingService.exportToCloud(finalContent, "GoogleDrive", project)
        }
    }

    private fun processVirtualFile(file: VirtualFile, opts: ClipCraftOptions, project: Project?): String {
        // Use filterRegex from options (make sure it exists in ClipCraftOptions)
        if (opts.filterRegex.isNotBlank() && !Regex(opts.filterRegex).containsMatchIn(file.path)) {
            return ""
        }
        if (shouldIgnore(file, opts)) {
            return ""
        }

        return if (file.isDirectory) {
            file.children
                .map { processVirtualFile(it, opts, project) }
                .filter { it.isNotEmpty() }
                .joinToString(System.lineSeparator())
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
                    if (gitInfo.isNotEmpty()) {
                        append(" $gitInfo")
                    }
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
        val lines = text.lines()
        val sb = StringBuilder()
        var lineNumber = 1
        for (rawLine in lines) {
            if (opts.removeLeadingBlankLines && sb.isEmpty() && rawLine.isBlank()) {
                continue
            }
            val line = if (opts.trimLineWhitespace) rawLine.trimEnd() else rawLine
            val finalLine = if (opts.includeLineNumbers) "%4d: %s".format(lineNumber, line) else line
            sb.append(finalLine).append("\n")
            lineNumber++
        }

        var processed = sb.toString()

        if (opts.removeComments) {
            processed = removeComments(processed, language)
        }
        if (opts.removeImports) {
            processed = removeImports(processed, language)
        }
        if (opts.collapseBlankLines) {
            processed = collapseConsecutiveBlankLines(processed)
        }
        if (opts.singleLineOutput) {
            processed = processed.replace("\n", " ")
        }
        processed = ClipCraftMacroManager.applyMacros(processed, opts.macros)
        return processed.trimEnd()
    }

    private fun removeComments(input: String, language: String): String {
        return when (language) {
            "java", "kotlin", "javascript", "typescript", "csharp", "cpp" -> {
                val noBlock = input.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
                noBlock.lines().filter { !it.trim().startsWith("//") }.joinToString("\n")
            }
            "python", "ruby", "sh", "bash" -> {
                input.lines().filter { !it.trim().startsWith("#") }.joinToString("\n")
            }
            else -> input
        }
    }

    private fun removeImports(input: String, language: String): String {
        return when (language) {
            "java", "kotlin", "javascript", "typescript", "csharp", "cpp" ->
                input.lines().filter { !it.trim().startsWith("import ") }.joinToString("\n")
            "python" ->
                input.lines().filter { !it.trim().startsWith("import ") && !it.trim().startsWith("from ") }
                    .joinToString("\n")
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
                // skip repeated blank lines
            } else {
                result += line
            }
        }
        return result.joinToString("\n")
    }

    private fun isTextFile(file: VirtualFile): Boolean {
        val sample = file.contentsToByteArray().take(8000)
        return sample.none { it.toInt() == 0 }
    }

    fun chunkContent(text: String, chunkSize: Int): List<String> {
        val chunks = mutableListOf<String>()
        var index = 0
        while (index < text.length) {
            val end = minOf(index + chunkSize, text.length)
            chunks.add(text.substring(index, end))
            index = end
        }
        return chunks
    }

    private fun buildDirectorySummary(files: Array<VirtualFile>, opts: ClipCraftOptions): String {
        val sb = StringBuilder("Directory Structure Summary:\n")
        files.forEach { buildDirectoryTree(it, 0, sb, opts) }
        return sb.toString()
    }

    private fun buildDirectoryTree(file: VirtualFile, depth: Int, sb: StringBuilder, opts: ClipCraftOptions) {
        if (depth > opts.directorySummaryDepth) return
        repeat(depth) { sb.append("  ") }
        sb.append("• ").append(file.name).append("\n")
        if (file.isDirectory) {
            file.children.forEach {
                buildDirectoryTree(it, depth + 1, sb, opts)
            }
        }
    }

    // Added missing function to collapse consecutive blank lines
    private fun collapseConsecutiveBlankLines(text: String): String {
        val lines = text.lines()
        val sb = StringBuilder()
        var lastLineBlank = false
        for (line in lines) {
            if (line.isBlank()) {
                if (!lastLineBlank) {
                    sb.append("\n")
                    lastLineBlank = true
                }
            } else {
                sb.append(line).append("\n")
                lastLineBlank = false
            }
        }
        return sb.toString().trimEnd()
    }
}
