package com.clipcraft.action

import com.clipcraft.model.ExportOptions
import com.clipcraft.model.OutputFormat
import com.clipcraft.services.EnhancedFileProcessingService
import com.clipcraft.services.NotificationService
import com.clipcraft.settings.SettingsStateProvider
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.vfs.VirtualFileWrapper
import java.nio.file.Paths

class ExportToFileAction : DumbAwareAction() {

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

        // Show file chooser
        val descriptor = FileSaverDescriptor(
            "Export to File",
            "Choose where to save the export",
            "md", "txt", "xml", "json", "html"
        )

        val fileChooser = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
        val fileWrapper = fileChooser.save(null as VirtualFileWrapper?, "clipcraft-export.md")

        val outputFile = fileWrapper?.file ?: return
        val extension = outputFile.extension.lowercase()

        val outputFormat = when (extension) {
            "md" -> OutputFormat.MARKDOWN_WITH_TOC
            "xml" -> OutputFormat.XML
            "json" -> OutputFormat.JSON
            "html" -> OutputFormat.HTML
            "txt" -> OutputFormat.PLAIN_TEXT
            else -> OutputFormat.MARKDOWN
        }

        val settings = SettingsStateProvider.getInstance().state

        val options = ExportOptions(
            includeGlobs = settings.includeGlobs,
            excludeGlobs = settings.excludeGlobs,
            maxFileSizeKb = settings.maxFileSizeKb,
            outputFormat = outputFormat,
            exportToFile = true,
            exportFilePath = outputFile.toPath(),
            includeMetadata = true,
            includeGitInfo = true,
            includeTimestamp = true,
            includeTableOfContents = true,
            includeStatistics = true,
            groupByDirectory = true
        )

        ProgressManager.getInstance().run(
            object : Task.Backgroundable(project, "ClipCraft: Exporting to file", true) {
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = false

                    val result = EnhancedFileProcessingService.processFiles(
                        project, files, options, projectBasePath, indicator
                    )

                    if (result.content.isBlank()) {
                        NotificationService.showWarning(project, "ClipCraft: No files matched filters")
                        return
                    }

                    try {
                        outputFile.writeText(result.content)
                        NotificationService.showSuccess(
                            project,
                            "ClipCraft: Exported ${result.metadata.filesProcessed} files to ${outputFile.name}"
                        )
                    } catch (ex: Exception) {
                        NotificationService.showWarning(
                            project,
                            "ClipCraft: Failed to write file: ${ex.message}"
                        )
                    }
                }
            }
        )
    }
}
