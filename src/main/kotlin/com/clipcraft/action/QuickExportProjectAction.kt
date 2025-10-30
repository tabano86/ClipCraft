package com.clipcraft.action

import com.clipcraft.model.ExportOptions
import com.clipcraft.model.ExportPreset
import com.clipcraft.services.ClipboardService
import com.clipcraft.services.EnhancedFileProcessingService
import com.clipcraft.services.NotificationService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.vfs.VirtualFileManager
import java.nio.file.Paths

class QuickExportProjectAction : DumbAwareAction() {

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val projectBasePath = project.basePath?.let { Paths.get(it) } ?: return
        val projectDir = VirtualFileManager.getInstance().findFileByNioPath(projectBasePath) ?: return

        // Use AI/LLM preset for project export
        val preset = ExportPreset.AI_LLM_PRESET

        val options = ExportOptions(
            includeGlobs = preset.includeGlobs,
            excludeGlobs = preset.excludeGlobs,
            maxFileSizeKb = preset.maxFileSizeKb,
            outputFormat = preset.outputFormat,
            includeMetadata = true,
            includeGitInfo = true,
            includeTimestamp = true,
            includeTableOfContents = true,
            includeStatistics = true,
            groupByDirectory = true
        )

        ProgressManager.getInstance().run(
            object : Task.Backgroundable(project, "ClipCraft: Exporting project", true) {
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = false

                    val result = EnhancedFileProcessingService.processFiles(
                        project, listOf(projectDir), options, projectBasePath, indicator
                    )

                    if (result.content.isBlank()) {
                        NotificationService.showWarning(project, "ClipCraft: No files matched filters")
                        return
                    }

                    ClipboardService.copyToClipboard(result.content)
                    NotificationService.showSuccess(
                        project,
                        "ClipCraft: Exported ${result.metadata.filesProcessed} files " +
                                "(${result.metadata.estimatedTokens} tokens)"
                    )
                }
            }
        )
    }
}
