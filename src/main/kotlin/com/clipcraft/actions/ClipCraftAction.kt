package com.clipcraft.actions

import com.clipcraft.integration.ClipCraftGitIntegration
import com.clipcraft.model.*
import com.clipcraft.services.*
import com.clipcraft.ui.ClipCraftOptionsDialog
import com.clipcraft.ui.ClipCraftQuickOptionsPanel
import com.clipcraft.util.ClipCraftFormatter
import com.clipcraft.util.ClipCraftIgnoreUtil
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
import com.intellij.openapi.fileTypes.FileTypeRegistry
import kotlinx.coroutines.*
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseEvent
import java.io.File

class ClipCraftAction : AnAction("ClipCraft: Copy Formatted Code") {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val settings = ClipCraftSettings.getInstance()
        val activeProfileName = settings.state.activeProfileName
        var opts = settings.getActiveOptions()

        if (opts.perProjectConfig && project != null) {
            opts = ClipCraftProjectProfileManager.getInstance(project).state
        }

        // Check ALT-click to show quick options
        val mouseEvent = e.inputEvent as? MouseEvent
        if (mouseEvent?.isAltDown == true) {
            val quickPanel = ClipCraftQuickOptionsPanel(opts, project)
            val result = javax.swing.JOptionPane.showConfirmDialog(
                null,
                quickPanel,
                "Quick ClipCraft Options",
                javax.swing.JOptionPane.OK_CANCEL_OPTION,
                javax.swing.JOptionPane.PLAIN_MESSAGE
            )
            if (result == javax.swing.JOptionPane.OK_OPTION) {
                opts = quickPanel.getOptions()
            }
        } else if (!opts.autoProcess) {
            val dialog = ClipCraftOptionsDialog(opts)
            if (dialog.showAndGet()) {
                opts = dialog.getOptions()
            }
        }

        // Save updated options
        settings.saveProfile(activeProfileName, opts)
        if (opts.perProjectConfig && project != null) {
            ClipCraftProjectProfileManager.getInstance(project).loadState(opts)
        }

        // Get selected files
        val selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        if (selectedFiles.isNullOrEmpty()) {
            ClipCraftNotificationCenter.notifyInfo("No files or directories selected.", project)
            return
        }

        // Merge .gitignore if enabled
        if (opts.useGitIgnore && project != null) {
            try {
                ClipCraftIgnoreUtil.mergeGitIgnoreRules(opts, project)
            } catch (ex: Exception) {
                ClipCraftNotificationCenter.notifyError("Failed to parse .gitignore: ${ex.message}", project)
            }
        }

        // Process files concurrently
        val finalContent = try {
            if (opts.measurePerformance) {
                ClipCraftPerformanceMetrics.measure("ClipCraft Processing") {
                    runBlocking { processAllFiles(selectedFiles, opts, project) }
                }
            } else {
                runBlocking { processAllFiles(selectedFiles, opts, project) }
            }
        } catch (ex: Exception) {
            ClipCraftNotificationCenter.notifyError("Processing error: ${ex.message}", project)
            return
        }

        // Handle output
        val stats = handleOutput(finalContent, opts, project, selectedFiles, activeProfileName)
        ClipCraftNotificationCenter.notifyInfo(
            "Profile: $activeProfileName | Processed ${stats.files} file(s), ${stats.lines} line(s).",
            project
        )
    }

    /**
     * Processes all selected files concurrently using coroutines.
     */
    private suspend fun processAllFiles(files: Array<VirtualFile>, opts: ClipCraftOptions, project: Project?): String =
        coroutineScope {
            val blocks = files.map { async(Dispatchers.IO) { processVirtualFile(it, opts, project) } }
                .awaitAll()
                .filter { it.isNotEmpty() }
            // Combine them with two line breaks
            val joined = blocks.joinToString(separator = "\n\n")
            if (opts.minimizeWhitespace) ClipCraftFormatter.minimizeWhitespace(joined) else joined
        }

    private suspend fun processVirtualFile(file: VirtualFile, opts: ClipCraftOptions, project: Project?): String {
        // Filter regex
        if (opts.filterRegex.isNotBlank() && !Regex(opts.filterRegex).containsMatchIn(file.path)) return ""
        // Check ignore patterns
        if (ClipCraftIgnoreUtil.shouldIgnore(file, opts)) return ""

        return if (file.isDirectory) {
            file.children.joinToString("\n") { child ->
                runBlocking { processVirtualFile(child, opts, project) }
            }.trim()
        } else {
            // Check if it's text
            if (!isProbablyTextFile(file)) return ""
            // Maybe large file => show progress
            val content = if (file.length > opts.largeFileThreshold && opts.showProgressInStatusBar) {
                loadFileWithProgress(file, project)
            } else {
                readFileContent(file)
            }
            buildFileBlock(content, file, opts, project)
        }
    }

    private suspend fun loadFileWithProgress(file: VirtualFile, project: Project?): String = withContext(Dispatchers.IO) {
        var text = ""
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Loading large file: ${file.name}", false) {
            override fun run(indicator: ProgressIndicator) {
                text = runBlocking { readFileContent(file) }
            }
        })
        text
    }

    private suspend fun readFileContent(file: VirtualFile): String = withContext(Dispatchers.IO) {
        String(file.contentsToByteArray(), Charsets.UTF_8)
    }

    private fun buildFileBlock(content: String, file: VirtualFile, opts: ClipCraftOptions, project: Project?): String {
        // Build metadata if needed
        val header = StringBuilder("File: ${file.path}")
        if (opts.includeMetadata) {
            header.append(" [Size: ${file.length} bytes, Modified: ${file.timeStamp}]")
            if (opts.displayGitMetadata && project != null) {
                val gitInfo = ClipCraftGitIntegration.getGitMetadata(project, file.path)
                if (gitInfo.isNotEmpty()) header.append(" $gitInfo")
            }
        }
        val language = detectLanguage(file)
        val processed = ClipCraftFormatter.processContent(content, opts, language)
        val formattedBlock = ClipCraftFormatter.formatBlock(processed, language, opts.outputFormat)
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

    private fun isProbablyTextFile(file: VirtualFile): Boolean {
        // Use IntelliJ’s FileTypeRegistry to check if recognized as text
        val ft = FileTypeRegistry.getInstance().getFileTypeByFile(file)
        return !ft.isBinary
    }

    private fun handleOutput(
        combinedContent: String,
        opts: ClipCraftOptions,
        project: Project?,
        selectedFiles: Array<VirtualFile>,
        profileName: String
    ): ProcessStats {
        val totalLines = combinedContent.lines().size
        val totalFiles = selectedFiles.size
        var finalContent = combinedContent

        if (opts.includeDirectorySummary) {
            val summary = buildDirectorySummary(selectedFiles, opts)
            finalContent = "$summary\n\n$finalContent"
        }

        try {
            if (opts.enableChunkingForGPT) {
                // chunk content
                val chunks = ClipCraftFormatter.chunkContent(
                    finalContent,
                    opts.maxChunkSize,
                    respectLineBoundaries = true
                )
                if (opts.exportToFile) {
                    // Write each chunk to file
                    chunks.forEachIndexed { index, chunk ->
                        val outBase = if (opts.exportFilePath.isNotEmpty()) {
                            opts.exportFilePath.removeSuffix(".txt") + "_chunk${index + 1}.txt"
                        } else {
                            (project?.basePath ?: "") + "/clipcraft_output_chunk${index + 1}.txt"
                        }
                        try {
                            File(outBase).writeText(chunk, Charsets.UTF_8)
                        } catch (ex: Exception) {
                            ClipCraftNotificationCenter.notifyError("Export failed: ${ex.message}", project)
                        }
                    }
                    ClipCraftNotificationCenter.notifyInfo("Split into ${chunks.size} chunks and exported to files.", project)
                } else {
                    // Copy only the last chunk to clipboard
                    if (chunks.isNotEmpty()) {
                        val lastChunk = chunks.last()
                        CopyPasteManager.getInstance().setContents(StringSelection(lastChunk))
                        ClipCraftNotificationCenter.notifyInfo("Split into ${chunks.size} chunks. Last chunk copied to clipboard.", project)
                    }
                }
            } else {
                // No chunking
                when {
                    opts.simultaneousExports.isNotEmpty() -> {
                        val basePath = opts.exportFilePath.ifEmpty { (project?.basePath ?: "") + "/clipcraft_output" }
                        val exportedPaths = ClipCraftSharingService.exportMultipleFormats(
                            basePath,
                            opts.simultaneousExports,
                            finalContent
                        )
                        ClipCraftNotificationCenter.notifyInfo("Simultaneously exported: ${exportedPaths.joinToString()}", project)
                    }
                    opts.exportToFile -> {
                        val outPath = if (opts.exportFilePath.isNotEmpty())
                            opts.exportFilePath
                        else
                            (project?.basePath ?: "") + "/clipcraft_output.txt"
                        File(outPath).writeText(finalContent, Charsets.UTF_8)
                        ClipCraftNotificationCenter.notifyInfo("Output exported to $outPath", project)
                    }
                    else -> {
                        CopyPasteManager.getInstance().setContents(StringSelection(finalContent))
                        ClipCraftNotificationCenter.notifyInfo("Code copied to clipboard.", project)
                    }
                }
            }

            if (opts.shareToGistEnabled) {
                val success = ClipCraftSharingService.shareToGist(finalContent, project)
                if (success)
                    ClipCraftNotificationCenter.notifyInfo("Shared to Gist!", project)
                else
                    ClipCraftNotificationCenter.notifyError("Failed to share to Gist", project)
            }
            if (opts.exportToCloudServices) {
                ClipCraftSharingService.exportToCloud(finalContent, "GoogleDrive", project)
            }
        } catch (ex: Exception) {
            ClipCraftNotificationCenter.notifyError("Error handling output: ${ex.message}", project)
        }
        return ProcessStats(totalFiles, totalLines)
    }

    data class ProcessStats(val files: Int, val lines: Int)

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
            file.children.forEach { buildDirectoryTree(it, depth + 1, sb, opts) }
        }
    }
}
