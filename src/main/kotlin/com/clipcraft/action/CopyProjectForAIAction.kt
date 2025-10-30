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
import com.intellij.openapi.vfs.LocalFileSystem
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

        // Get project root as VirtualFile
        val projectRoot = LocalFileSystem.getInstance().findFileByPath(project.basePath!!) ?: return

        // User-friendly AI optimized settings - focus on source code
        val options = ExportOptions(
            includeGlobs = "**/*.kt,**/*.java,**/*.py,**/*.js,**/*.ts,**/*.jsx,**/*.tsx,**/*.go,**/*.rs,**/*.xml,**/*.gradle,**/*.md",
            excludeGlobs = "**/build/**,**/target/**,**/.gradle/**,**/.idea/**,**/node_modules/**,**/dist/**,**/out/**,**/.git/**",
            maxFileSizeKb = 500,
            outputFormat = OutputFormat.MARKDOWN,
            includeMetadata = false,
            includeGitInfo = false,
            includeTimestamp = false,
            includeTableOfContents = true,
            includeStatistics = true,
            groupByDirectory = true,
            includeLineNumbers = false
        )

        ProgressManager.getInstance().run(
            object : Task.Backgroundable(project, "ClipCraft: Copying project for AI...", true) {
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = false

                    val result = EnhancedFileProcessingService.processFiles(
                        project, listOf(projectRoot), options, projectBasePath, indicator
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
