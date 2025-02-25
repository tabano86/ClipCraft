package com.clipcraft.actions

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.ConcurrencyMode
import com.clipcraft.model.OutputTarget
import com.clipcraft.model.Snippet
import com.clipcraft.services.ClipCraftNotificationCenter
import com.clipcraft.services.ClipCraftSettingsState
import com.clipcraft.util.CodeFormatter
import com.clipcraft.util.IgnoreUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.util.concurrent.Executors
import javax.swing.JOptionPane
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

class ClipCraftAction : AnAction() {

    private val logger = Logger.getInstance(ClipCraftAction::class.java)

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val vFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        e.presentation.isVisible = vFiles != null && vFiles.isNotEmpty()
        e.presentation.isEnabled = vFiles != null && vFiles.isNotEmpty()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val vFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        if (vFiles.isEmpty()) {
            ClipCraftNotificationCenter.warn("No files selected to copy.")
            return
        }

        val globalState = ClipCraftSettingsState.getInstance()
        val options = globalState.advancedOptions
        options.resolveConflicts()

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "ClipCraft Copy", true) {
            override fun run(indicator: ProgressIndicator) {
                runBlocking {
                    copyContents(project, vFiles.toList(), options)
                }
            }
        })
    }

    private suspend fun copyContents(project: Project, files: List<VirtualFile>, options: ClipCraftOptions) {
        logger.info("Preparing to copy ${files.size} file(s) with concurrency=${options.concurrencyMode}")
        val allFiles = expandFiles(files)

        when (options.concurrencyMode) {
            ConcurrencyMode.DISABLED -> copyContentsSequential(allFiles, project, options)
            ConcurrencyMode.THREAD_POOL,
            ConcurrencyMode.COROUTINES,
            -> copyContentsCoroutines(allFiles, project, options)
        }
    }

    private fun expandFiles(files: List<VirtualFile>): List<VirtualFile> {
        val result = mutableListOf<VirtualFile>()
        fun recurse(f: VirtualFile) {
            if (f.isDirectory) {
                f.children?.forEach { recurse(it) }
            } else {
                result.add(f)
            }
        }
        files.forEach { recurse(it) }
        return result
    }

    private fun copyContentsSequential(files: List<VirtualFile>, project: Project, options: ClipCraftOptions) {
        val snippetList = mutableListOf<Snippet>()
        val failed = mutableListOf<String>()
        var skippedImages = 0
        var ignoredByGit = 0

        for (vf in files) {
            if (!options.includeImageFiles && vf.isImageFile()) {
                skippedImages++
                continue
            }
            if (options.useGitIgnore && vf.isGitIgnored(project.basePath ?: "", options)) {
                ignoredByGit++
                continue
            }
            val content = vf.readTextOrNull()
            if (content == null) {
                failed.add(vf.name)
                continue
            }
            snippetList.add(
                Snippet(
                    filePath = vf.path,
                    fileName = vf.name,
                    relativePath = null,
                    fileSizeBytes = content.length.toLong(),
                    lastModified = System.currentTimeMillis(),
                    content = content,
                ),
            )
        }

        val resultText = CodeFormatter.formatSnippets(snippetList, options).joinToString("\n---\n")
        postCopy(project, resultText, failed, snippetList.size)
        logger.info("Skipped images: $skippedImages, Git-ignored: $ignoredByGit")
    }

    private suspend fun copyContentsCoroutines(files: List<VirtualFile>, project: Project, options: ClipCraftOptions) {
        val failed = mutableListOf<String>()
        val snippetJobs = mutableListOf<Deferred<Snippet?>>()
        val executor = Executors.newFixedThreadPool(options.maxConcurrentTasks)
        val dispatcher = executor.asCoroutineDispatcher()
        val scope = CoroutineScope(SupervisorJob() + dispatcher)

        try {
            files.forEach { vf ->
                snippetJobs.add(
                    scope.async {
                        if (!options.includeImageFiles && vf.isImageFile()) return@async null
                        if (options.useGitIgnore && vf.isGitIgnored(project.basePath ?: "", options)) return@async null
                        val content = vf.readTextOrNull() ?: run {
                            failed.add(vf.name)
                            return@async null
                        }
                        Snippet(
                            filePath = vf.path,
                            fileName = vf.name,
                            relativePath = null,
                            fileSizeBytes = content.length.toLong(),
                            lastModified = System.currentTimeMillis(),
                            content = content,
                        )
                    },
                )
            }
            val snippetList = snippetJobs.awaitAll().filterNotNull()
            val resultText = CodeFormatter.formatSnippets(snippetList, options).joinToString("\n---\n")
            postCopy(project, resultText, failed, snippetList.size)
        } finally {
            executor.shutdown()
        }
    }

    private fun postCopy(project: Project, text: String, failedFiles: List<String>, snippetCount: Int) {
        val globalState = ClipCraftSettingsState.getInstance()
        if (globalState.maxCopyCharacters > 0 && text.length > globalState.maxCopyCharacters) {
            val res = JOptionPane.showConfirmDialog(
                null,
                "Output is ${text.length} chars, exceeding limit (${globalState.maxCopyCharacters}). Copy anyway?",
                "ClipCraft Large Copy",
                JOptionPane.YES_NO_OPTION,
            )
            if (res != JOptionPane.YES_OPTION) {
                logger.info("User canceled large copy.")
                return
            }
        }
        val success = try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(StringSelection(text), null)
            true
        } catch (ex: Exception) {
            logger.error("Clipboard copy failed", ex)
            false
        }
        if (success) {
            ClipCraftNotificationCenter.info("Copied $snippetCount snippet(s), length: ${text.length} chars.")
        } else {
            ClipCraftNotificationCenter.error("Failed to copy to clipboard.")
        }
        if (failedFiles.isNotEmpty()) {
            ClipCraftNotificationCenter.warn("Some files couldn't be read: ${failedFiles.joinToString()}")
        }
    }

    // Extension: read file text or null.
    private fun VirtualFile.readTextOrNull(): String? = try {
        String(contentsToByteArray(), charset)
    } catch (ex: Exception) {
        logger.warn("Error reading file $path: ${ex.message}", ex)
        null
    }

    // Extension: quick check for images.
    private fun VirtualFile.isImageFile(): Boolean {
        val ext = extension?.lowercase()
        return ext in listOf("png", "jpg", "jpeg", "gif", "bmp", "webp", "tiff", "ico", "svg")
    }

    // Extension: check if .gitignore excludes this file.
    private fun VirtualFile.isGitIgnored(projectBase: String, options: ClipCraftOptions): Boolean {
        return IgnoreUtil.shouldIgnore(java.io.File(path), options, projectBase)
    }

    private fun postCopy(project: Project, text: String, failedFiles: List<String>, snippetCount: Int, options: ClipCraftOptions) {
        val globalState = ClipCraftSettingsState.getInstance()
        if (globalState.maxCopyCharacters > 0 && text.length > globalState.maxCopyCharacters) {
            val res = JOptionPane.showConfirmDialog(
                null,
                "Output is ${text.length} chars, exceeding limit (${globalState.maxCopyCharacters}). Copy anyway?",
                "ClipCraft Large Copy",
                JOptionPane.YES_NO_OPTION,
            )
            if (res != JOptionPane.YES_OPTION) {
                logger.info("User canceled large copy.")
                return
            }
        }

        // If the output target is CLIPBOARD or BOTH, copy to the clipboard
        if (options.outputTarget == OutputTarget.CLIPBOARD || options.outputTarget == OutputTarget.BOTH) {
            val success = try {
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                clipboard.setContents(StringSelection(text), null)
                true
            } catch (ex: Exception) {
                logger.error("Clipboard copy failed", ex)
                false
            }
            if (success) {
                ClipCraftNotificationCenter.info("Copied $snippetCount snippet(s), length: ${text.length} chars.")
            } else {
                ClipCraftNotificationCenter.error("Failed to copy to clipboard.")
            }
        }

        // If output target is MACRO_ONLY or BOTH, we consider that the macro expansion
        // is already done in aggregator, so do any additional handling here if needed.
        // For example, you might do some specialized logging or uploading.

        if (failedFiles.isNotEmpty()) {
            ClipCraftNotificationCenter.warn("Some files couldn't be read: ${failedFiles.joinToString()}")
        }
    }
}
