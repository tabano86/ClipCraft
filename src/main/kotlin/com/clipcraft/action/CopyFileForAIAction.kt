package com.clipcraft.action

import com.clipcraft.model.ExportOptions
import com.clipcraft.model.OutputFormat
import com.clipcraft.services.EnhancedFileProcessingService
import com.clipcraft.services.NotificationService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.project.DumbAwareAction
import java.awt.datatransfer.StringSelection
import java.nio.file.Paths

/**
 * Ultra-simple one-click action: Copy current file for AI
 */
class CopyFileForAIAction : DumbAwareAction() {

    override fun update(e: AnActionEvent) {
        val project = e.project
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = project != null && file != null && !file.isDirectory
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val projectBasePath = project.basePath?.let { Paths.get(it) } ?: return

        // Simple, clean output - just the code for AI
        val options = ExportOptions(
            includeGlobs = "**/*",
            excludeGlobs = "",
            maxFileSizeKb = 1024,
            outputFormat = OutputFormat.MARKDOWN,
            includeMetadata = false,
            includeGitInfo = false,
            includeTimestamp = false,
            includeTableOfContents = false,
            includeStatistics = false,
            groupByDirectory = false,
            includeLineNumbers = false
        )

        val result = EnhancedFileProcessingService.processFiles(
            project, listOf(file), options, projectBasePath, EmptyProgressIndicator()
        )

        CopyPasteManager.getInstance().setContents(StringSelection(result.content))
        
        NotificationService.showSuccess(
            project,
            "✓ Copied ${file.name} - Ready for AI!"
        )
    }
}
