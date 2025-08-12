package com.clipcraft.action

import com.clipcraft.model.SettingsState
import com.clipcraft.services.ClipboardService
import com.clipcraft.services.FileProcessingService
import com.clipcraft.services.NotificationService
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
        val files: List<VirtualFile?> = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)!!.toList()
        val projectBasePath = project.basePath?.let { Paths.get(it) } ?: return
        val settings: SettingsState = SettingsStateProvider.getInstance().state.copy()

        ProgressManager.getInstance().run(
            object : Task.Backgroundable(project, "Clipcrafting markdown", true) {
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = false
                    indicator.text = "Discovering files..."

                    val result = FileProcessingService.processVirtualFiles(files, settings, projectBasePath, indicator)

                    if (result.markdownContent.isBlank()) {
                        NotificationService.showWarning(project, "ClipCraft: No files matched filters.")
                        return
                    }

                    ClipboardService.copyToClipboard(result.markdownContent)

                    val successMsg = "ClipCraft: Copied ${result.filesProcessed} files"
                    val finalMsg = if (result.filesSkipped > 0) "$successMsg (${result.filesSkipped} skipped)" else successMsg
                    NotificationService.showSuccess(project, finalMsg)
                }
            }
        )
    }
}