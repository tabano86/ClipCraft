package com.clipcraft.actions

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.ConcurrencyMode
import com.clipcraft.model.Snippet
import com.clipcraft.services.ClipCraftNotificationCenter
import com.clipcraft.services.ClipCraftSettingsState
import com.clipcraft.util.CodeFormatter
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.util.concurrent.Executors
import javax.swing.JOptionPane

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
        for (vf in files) {
            val content = readFileOrNull(vf)
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
        postCopy(project, resultText, failed, files.size)
    }

    private suspend fun copyContentsCoroutines(files: List<VirtualFile>, project: Project, options: ClipCraftOptions) {
        val failed = mutableListOf<String>()
        val snippetJobs = mutableListOf<Deferred<Snippet?>>()

        // Instead of 'newFixedThreadPoolContext', do:
        val executor = Executors.newFixedThreadPool(options.maxConcurrentTasks)
        val dispatcher = executor.asCoroutineDispatcher()
        val scope = CoroutineScope(dispatcher)

        try {
            files.forEach { vf ->
                snippetJobs.add(
                    scope.async {
                        val content = readFileOrNull(vf) ?: run {
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
            postCopy(project, resultText, failed, files.size)
        } finally {
            // Shut down the executor
            executor.shutdown()
        }
    }

    private fun readFileOrNull(vf: VirtualFile): String? {
        return try {
            val bytes = vf.contentsToByteArray()
            String(bytes, vf.charset)
        } catch (ex: Exception) {
            logger.warn("Error reading file ${vf.path}: ${ex.message}", ex)
            null
        }
    }

    private fun postCopy(project: Project, text: String, failedFiles: List<String>, totalCount: Int) {
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
            ClipCraftNotificationCenter.info("Copied $totalCount file(s), length: ${text.length} chars.")
        } else {
            ClipCraftNotificationCenter.error("Failed to copy to clipboard.")
        }
        if (failedFiles.isNotEmpty()) {
            ClipCraftNotificationCenter.warn("Some files couldn't be read: ${failedFiles.joinToString()}")
        }
    }
}
