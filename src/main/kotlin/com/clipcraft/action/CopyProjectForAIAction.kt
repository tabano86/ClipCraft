package com.clipcraft.action

import com.clipcraft.model.ExportOptions
import com.clipcraft.model.OutputFormat
import com.clipcraft.services.EnhancedFileProcessingService
import com.clipcraft.services.NotificationService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import java.awt.datatransfer.StringSelection
import java.nio.file.Paths

/**
 * One-click action to copy entire project optimized for AI assistants
 * No dialogs, no configuration - just instant results!
 */
class CopyProjectForAIAction : DumbAwareAction() {

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val projectBasePath = project.basePath?.let { Paths.get(it) } ?: return

        // Use AI/LLM optimized settings
        val options = ExportOptions(
            includeGlobs = "**/*.kt,**/*.java,**/*.xml,**/*.md",
            excludeGlobs = "**/build/**,**/target/**,**/.gradle/**,**/.idea/**",
            maxFileSizeKb = 500,
            outputFormat = OutputFormat.MARKDOWN_WITH_TOC,
            includeMetadata = true,
            includeGitInfo = true,
            includeTimestamp = true,
            includeTableOfContents = true,
            includeStatistics = true,
            groupByDirectory = true
        )

        ProgressManager.getInstance().run(
            object : Task.Backgroundable(project, "ClipCraft: Copying project for AI...", true) {
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = false

                    val result = EnhancedFileProcessingService.processFiles(
                        project, emptyList(), options, projectBasePath, indicator
                    )

                    if (result.content.isBlank()) {
                        NotificationService.showWarning(project, "ClipCraft: No files matched filters")
                        return
                    }

                    CopyPasteManager.getInstance().setContents(StringSelection(result.content))
                    
                    NotificationService.showSuccess(
                        project,
                        "✓ Copied ${result.metadata.filesProcessed} files - Ready for AI!"
                    )
                }
            }
        )
    }
}
