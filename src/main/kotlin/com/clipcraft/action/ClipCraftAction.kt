package com.clipcraft.action

import com.clipcraft.services.ClipboardService
import com.clipcraft.services.EnhancedFileProcessingService
import com.clipcraft.services.NotificationService
import com.clipcraft.services.professional.ProfessionalTokenEstimator
import com.clipcraft.settings.SettingsStateProvider
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Paths

class ClipCraftAction : DumbAwareAction() {

    override fun update(e: AnActionEvent) {
        val project = e.project
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        e.presentation.isEnabledAndVisible = project != null && !files.isNullOrEmpty()
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project!!
        val files: List<VirtualFile> = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)!!.toList()
        val projectBasePath = project.basePath?.let { Paths.get(it) } ?: return
        val settings = SettingsStateProvider.getInstance().state.copy()
        val options = settings.toExportOptions()

        ProgressManager.getInstance().run(
            object : Task.Backgroundable(project, "ClipCraft: Exporting files", true) {
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = false

                    val result = EnhancedFileProcessingService.processFiles(
                        project,
                        files,
                        options,
                        projectBasePath,
                        indicator,
                    )

                    if (result.content.isBlank()) {
                        NotificationService.showWarning(project, "ClipCraft: No files matched filters")
                        return
                    }

                    ClipboardService.copyToClipboard(result.content)

                    val tokenInfo = ProfessionalTokenEstimator.formatTokenCount(result.metadata.estimatedTokens)
                    val successMsg = "ClipCraft: Copied ${result.metadata.filesProcessed} files ($tokenInfo)"
                    val finalMsg = if (result.metadata.filesSkipped > 0) {
                        "$successMsg - ${result.metadata.filesSkipped} skipped"
                    } else {
                        successMsg
                    }

                    NotificationService.showSuccess(project, finalMsg)
                }
            },
        )
    }
}
