package com.clipcraft.action

import com.clipcraft.model.ExportOptions
import com.clipcraft.model.OutputFormat
import com.clipcraft.services.ClipboardService
import com.clipcraft.services.EnhancedFileProcessingService
import com.clipcraft.services.NotificationService
import com.clipcraft.settings.SettingsStateProvider
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import java.nio.file.Paths

class QuickExportCurrentFileAction : DumbAwareAction() {

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = file != null && !file.isDirectory
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val projectBasePath = project.basePath?.let { Paths.get(it) } ?: return

        val settings = SettingsStateProvider.getInstance().state

        // Create simple options for single file export
        val options = ExportOptions(
            includeGlobs = "**/*",
            excludeGlobs = "",
            maxFileSizeKb = settings.maxFileSizeKb,
            outputFormat = OutputFormat.MARKDOWN,
            includeMetadata = true,
            includeTimestamp = true,
            includeStatistics = true,
        )

        ProgressManager.getInstance().run(
            object : Task.Backgroundable(project, "ClipCraft: Exporting current file", true) {
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = false

                    val result = EnhancedFileProcessingService.processFiles(
                        project,
                        listOf(file),
                        options,
                        projectBasePath,
                        indicator,
                    )

                    if (result.content.isBlank()) {
                        NotificationService.showWarning(project, "ClipCraft: Could not export file")
                        return
                    }

                    ClipboardService.copyToClipboard(result.content)
                    NotificationService.showSuccess(
                        project,
                        "ClipCraft: Exported ${file.name} (${result.metadata.estimatedTokens} tokens)",
                    )
                }
            },
        )
    }
}
