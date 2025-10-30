package com.clipcraft.action

import com.clipcraft.model.ExportOptions
import com.clipcraft.model.ExportPreset
import com.clipcraft.services.ClipboardService
import com.clipcraft.services.EnhancedFileProcessingService
import com.clipcraft.services.NotificationService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBList
import java.nio.file.Paths
import javax.swing.DefaultListCellRenderer
import javax.swing.JList

class ExportWithPresetAction : DumbAwareAction() {

    override fun update(e: AnActionEvent) {
        val project = e.project
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        e.presentation.isEnabledAndVisible = project != null && !files.isNullOrEmpty()
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)?.toList() ?: return
        val projectBasePath = project.basePath?.let { Paths.get(it) } ?: return

        val presets = ExportPreset.getAllPresets()
        val listModel = JBList(presets.map { "${it.name} - ${it.description}" })

        listModel.cellRenderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ) = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus).apply {
                if (index >= 0 && index < presets.size) {
                    val preset = presets[index]
                    text = "<html><b>${preset.name}</b><br><small>${preset.description}</small></html>"
                }
            }
        }

        JBPopupFactory.getInstance()
            .createListPopupBuilder(listModel)
            .setTitle("Select Export Preset")
            .setItemChosenCallback {
                val selectedIndex = listModel.selectedIndex
                if (selectedIndex >= 0) {
                    val preset = presets[selectedIndex]
                    executeExport(project, files, preset, projectBasePath)
                }
            }
            .createPopup()
            .showInBestPositionFor(e.dataContext)
    }

    private fun executeExport(
        project: com.intellij.openapi.project.Project,
        files: List<com.intellij.openapi.vfs.VirtualFile>,
        preset: ExportPreset,
        projectBasePath: java.nio.file.Path
    ) {
        val options = ExportOptions(
            includeGlobs = preset.includeGlobs,
            excludeGlobs = preset.excludeGlobs,
            maxFileSizeKb = preset.maxFileSizeKb,
            outputFormat = preset.outputFormat,
            includeLineNumbers = preset.includeLineNumbers,
            stripComments = preset.stripComments,
            includeMetadata = preset.includeMetadata,
            includeGitInfo = true,
            includeTimestamp = true,
            includeStatistics = true,
            groupByDirectory = true
        )

        ProgressManager.getInstance().run(
            object : Task.Backgroundable(project, "ClipCraft: Exporting with ${preset.name}", true) {
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = false

                    val result = EnhancedFileProcessingService.processFiles(
                        project, files, options, projectBasePath, indicator
                    )

                    if (result.content.isBlank()) {
                        NotificationService.showWarning(project, "ClipCraft: No files matched preset filters")
                        return
                    }

                    ClipboardService.copyToClipboard(result.content)
                    NotificationService.showSuccess(
                        project,
                        "ClipCraft: Exported ${result.metadata.filesProcessed} files with ${preset.name} preset"
                    )
                }
            }
        )
    }
}
