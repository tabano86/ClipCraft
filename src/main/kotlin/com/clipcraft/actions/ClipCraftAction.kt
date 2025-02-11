package com.clipcraft.actions

import com.clipcraft.integration.ClipCraftGitIntegration
import com.clipcraft.model.*
import com.clipcraft.services.*
import com.clipcraft.util.CodeFormatter
import com.clipcraft.util.IgnoreUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ClipCraftAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val virtualFiles = e.getData(LangDataKeys.VIRTUAL_FILE_ARRAY) ?: return

        val profileManager = project.getService(ClipCraftProjectProfileManager::class.java)
        val activeProfile = profileManager?.getActiveProfile()
            ?: ClipCraftSettings.getInstance().getCurrentProfile()
        val options = activeProfile.options

        // Ensure final settings are consistent
        options.resolveConflicts()

        // Possibly parse .gitignore
        IgnoreUtil.parseGitIgnoreIfEnabled(options, project.basePath ?: "")

        when (options.concurrencyMode) {
            ConcurrencyMode.DISABLED -> {
                runSequentially(project, virtualFiles.map { it.path }, options)
            }
            ConcurrencyMode.THREAD_POOL -> {
                runWithThreadPool(project, virtualFiles.map { it.path }, options)
            }
            ConcurrencyMode.COROUTINES -> {
                runWithCoroutines(project, virtualFiles.map { it.path }, options)
            }
        }
    }

    private fun runSequentially(project: Project, paths: List<String>, options: ClipCraftOptions) {
        val metrics = project.getService(ClipCraftPerformanceMetrics::class.java)
        metrics.startProcessing()

        val snippetGroup = SnippetGroup("Collected Snippets")
        for (path in paths) {
            processFileOrDirectory(File(path), project, options, snippetGroup)
        }

        val outputs = CodeFormatter.formatSnippets(snippetGroup.snippets, options)
        handleFinalOutputs(project, outputs)

        metrics.stopProcessingAndLog("ClipCraftAction (sequential)")
    }

    private fun runWithThreadPool(project: Project, paths: List<String>, options: ClipCraftOptions) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "ClipCraft Concurrent Processing", true) {
            override fun run(indicator: ProgressIndicator) {
                val metrics = project.getService(ClipCraftPerformanceMetrics::class.java)
                metrics.startProcessing()

                val snippetGroup = SnippetGroup("Collected Snippets")
                val pool = Executors.newFixedThreadPool(options.maxConcurrentTasks)

                for (path in paths) {
                    pool.submit {
                        processFileOrDirectory(File(path), project, options, snippetGroup)
                    }
                }

                pool.shutdown()
                pool.awaitTermination(10, TimeUnit.MINUTES)

                val outputs = CodeFormatter.formatSnippets(snippetGroup.snippets, options)
                handleFinalOutputs(project, outputs)

                metrics.stopProcessingAndLog("ClipCraftAction (thread pool)")
            }
        })
    }

    /**
     * Example COROUTINES approach.
     * We dispatch file processing in parallel using launch/async within a runBlocking block,
     * then collect them and format.
     */
    private fun runWithCoroutines(project: Project, paths: List<String>, options: ClipCraftOptions) {
        // Use IntelliJ's background task to avoid blocking UI
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "ClipCraft Coroutine Processing", true) {
            override fun run(indicator: ProgressIndicator) {
                val metrics = project.getService(ClipCraftPerformanceMetrics::class.java)
                metrics.startProcessing()

                val snippetGroup = SnippetGroup("Collected Snippets")

                // Use runBlocking to keep it synchronous in background thread
                runBlocking(Dispatchers.Default) {
                    val jobs = paths.map { path ->
                        async {
                            processFileOrDirectory(File(path), project, options, snippetGroup)
                        }
                    }
                    jobs.awaitAll() // Wait for all coroutines to complete
                }

                val outputs = CodeFormatter.formatSnippets(snippetGroup.snippets, options)
                handleFinalOutputs(project, outputs)

                metrics.stopProcessingAndLog("ClipCraftAction (coroutines)")
            }
        })
    }

    private fun processFileOrDirectory(
        file: File,
        project: Project,
        options: ClipCraftOptions,
        snippetGroup: SnippetGroup
    ) {
        if (!file.exists()) return
        if (IgnoreUtil.shouldIgnore(file, options, project.basePath ?: "")) return

        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                processFileOrDirectory(child, project, options, snippetGroup)
            }
        } else {
            val snippet = createSnippet(file, project, options)
            synchronized(snippetGroup) {
                snippetGroup.snippets.add(snippet)
            }
        }
    }

    private fun createSnippet(file: File, project: Project, options: ClipCraftOptions): Snippet {
        val content = try {
            file.readText()
        } catch (e: Exception) {
            "<Error reading file: ${e.message}>"
        }

        var snippet = Snippet(
            filePath = file.absolutePath,
            relativePath = project.basePath?.let { base ->
                file.relativeTo(File(base)).path
            },
            fileName = file.name,
            fileSizeBytes = file.length(),
            lastModified = file.lastModified(),
            content = content
        )

        if (options.includeGitInfo) {
            snippet = ClipCraftGitIntegration.enrichSnippetWithGitInfo(project, snippet)
        }
        return snippet
    }

    private fun handleFinalOutputs(project: Project, outputs: List<String>) {
        val fullContent = outputs.joinToString("\n---\n")
        ClipCraftNotificationCenter.info("ClipCraft finished processing. Content length: ${fullContent.length}")

        val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
        val selection = java.awt.datatransfer.StringSelection(fullContent)
        clipboard.setContents(selection, selection)
    }
}
