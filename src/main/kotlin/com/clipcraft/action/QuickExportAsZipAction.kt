package com.clipcraft.action

import com.clipcraft.services.NotificationService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import java.io.FileOutputStream
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class QuickExportAsZipAction : DumbAwareAction() {

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

        // Show file chooser for ZIP
        val descriptor = FileSaverDescriptor(
            "Export as ZIP",
            "Choose where to save the ZIP archive",
            "zip"
        )

        val fileChooser = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
        val fileWrapper = fileChooser.save(project.baseDir, "clipcraft-export.zip")

        val outputFile = fileWrapper?.file ?: return

        ProgressManager.getInstance().run(
            object : Task.Backgroundable(project, "ClipCraft: Creating ZIP archive", true) {
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = false

                    try {
                        ZipOutputStream(FileOutputStream(outputFile)).use { zipOut ->
                            val allFiles = mutableListOf<com.intellij.openapi.vfs.VirtualFile>()
                            
                            // Recursively collect all files
                            fun collectFiles(file: com.intellij.openapi.vfs.VirtualFile) {
                                if (file.isDirectory) {
                                    file.children?.forEach { collectFiles(it) }
                                } else {
                                    allFiles.add(file)
                                }
                            }
                            
                            files.forEach { collectFiles(it) }
                            
                            allFiles.forEachIndexed { index, virtualFile ->
                                indicator.fraction = index.toDouble() / allFiles.size
                                indicator.text = "Adding ${virtualFile.name}..."

                                val relativePath = virtualFile.path.removePrefix(projectBasePath.toString())
                                    .removePrefix("/").removePrefix("\\")

                                val entry = ZipEntry(relativePath)
                                zipOut.putNextEntry(entry)
                                zipOut.write(virtualFile.contentsToByteArray())
                                zipOut.closeEntry()
                            }
                        }
                        NotificationService.showSuccess(
                            project,
                            "ClipCraft: Exported ${files.size} items to ${outputFile.name}"
                        )
                    } catch (ex: Exception) {
                        NotificationService.showWarning(
                            project,
                            "ClipCraft: Failed to create ZIP: ${ex.message}"
                        )
                    }
                }
            }
        )
    }
}
