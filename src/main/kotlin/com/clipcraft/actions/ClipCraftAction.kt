package com.clipcraft.actions

import com.clipcraft.integration.ClipCraftGitIntegration
import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.ConcurrencyMode
import com.clipcraft.model.Snippet
import com.clipcraft.model.SnippetGroup
import com.clipcraft.services.ClipCraftNotificationCenter
import com.clipcraft.services.ClipCraftPerformanceMetrics
import com.clipcraft.services.ClipCraftProjectProfileManager
import com.clipcraft.services.ClipCraftSettings
import com.clipcraft.util.CodeFormatter
import com.clipcraft.util.IgnoreUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Main action that copies formatted code from selected files/directories to the clipboard.
 */
class ClipCraftAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val virtualFiles = e.getData(LangDataKeys.VIRTUAL_FILE_ARRAY) ?: return

        // Obtain the active profile from the project-level manager or fallback to global.
        val profileManager = project.getService(ClipCraftProjectProfileManager::class.java)
        val activeProfile = profileManager?.getActiveProfile() ?: ClipCraftSettings.getInstance().getCurrentProfile()
        val options = activeProfile.options

        // Ensure concurrency + chunking settings are up to date.
        options.resolveConflicts()

        // Optionally parse .gitignore if user has enabled it.
        IgnoreUtil.parseGitIgnoreIfEnabled(options, project.basePath ?: "")

        // Depending on concurrency mode, run differently.
        when (options.concurrencyMode) {
            ConcurrencyMode.DISABLED -> runSequentially(project, virtualFiles.map { it.path }, options)
            ConcurrencyMode.THREAD_POOL -> runWithThreadPool(project, virtualFiles.map { it.path }, options)
            ConcurrencyMode.COROUTINES -> runWithCoroutines(project, virtualFiles.map { it.path }, options)
        }
    }

    private fun runSequentially(project: Project, paths: List<String>, options: ClipCraftOptions) {
        val metrics = project.getService(ClipCraftPerformanceMetrics::class.java)
        metrics?.startProcessing()

        val snippetGroup = SnippetGroup("Collected Snippets")
        for (path in paths) {
            processFileOrDirectory(File(path), project, options, snippetGroup)
        }

        val outputs = formatFinalOutputs(snippetGroup, options)
        handleFinalOutputs(project, outputs)

        metrics?.stopProcessingAndLog("ClipCraftAction (sequential)")
    }

    private fun runWithThreadPool(project: Project, paths: List<String>, options: ClipCraftOptions) {
        ProgressManager.getInstance()
            .run(object : Task.Backgroundable(project, "ClipCraft Concurrent Processing", true) {
                override fun run(indicator: ProgressIndicator) {
                    val metrics = project.getService(ClipCraftPerformanceMetrics::class.java)
                    metrics?.startProcessing()

                    val snippetGroup = SnippetGroup("Collected Snippets")
                    val pool = Executors.newFixedThreadPool(options.maxConcurrentTasks)
                    for (path in paths) {
                        pool.submit {
                            processFileOrDirectory(File(path), project, options, snippetGroup)
                        }
                    }
                    pool.shutdown()
                    pool.awaitTermination(10, TimeUnit.MINUTES)

                    val outputs = formatFinalOutputs(snippetGroup, options)
                    handleFinalOutputs(project, outputs)

                    metrics?.stopProcessingAndLog("ClipCraftAction (thread pool)")
                }
            })
    }

    private fun runWithCoroutines(project: Project, paths: List<String>, options: ClipCraftOptions) {
        ProgressManager.getInstance()
            .run(object : Task.Backgroundable(project, "ClipCraft Coroutine Processing", true) {
                override fun run(indicator: ProgressIndicator) {
                    val metrics = project.getService(ClipCraftPerformanceMetrics::class.java)
                    metrics?.startProcessing()

                    val snippetGroup = SnippetGroup("Collected Snippets")
                    runBlocking(Dispatchers.Default) {
                        paths.map { path ->
                            async {
                                processFileOrDirectory(File(path), project, options, snippetGroup)
                            }
                        }.awaitAll()
                    }

                    val outputs = formatFinalOutputs(snippetGroup, options)
                    handleFinalOutputs(project, outputs)

                    metrics?.stopProcessingAndLog("ClipCraftAction (coroutines)")
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
                try {
                    file.relativeTo(File(base)).path
                } catch (_: IllegalArgumentException) {
                    file.absolutePath
                }
            },
            fileName = file.name,
            fileSizeBytes = file.length(),
            lastModified = file.lastModified(),
            content = content
        )
        // Optionally enrich snippet with Git info
        if (options.includeGitInfo) {
            snippet = ClipCraftGitIntegration.enrichSnippetWithGitInfo(project, snippet)
        }
        return snippet
    }

    /**
     * Builds the final output string, including optional directory structure, user-defined header/footer,
     * and the formatted snippet text.
     */
    private fun formatFinalOutputs(snippetGroup: SnippetGroup, options: ClipCraftOptions): String {
        val header = options.gptHeaderText.orEmpty()
        val footer = options.gptFooterText.orEmpty()

        val snippetText = CodeFormatter.formatSnippets(snippetGroup.snippets, options)
            .joinToString("\n---\n")

        val dirStructure = if (options.includeDirectorySummary) {
            buildString {
                append("Directory Structure:\n")
                snippetGroup.snippets
                    .mapNotNull { it.relativePath }
                    .distinct()
                    .sorted()
                    .forEach { path -> append("  $path\n") }
                append("\n")
            }
        } else ""

        return buildString {
            if (header.isNotEmpty()) appendLine(header).appendLine()
            if (dirStructure.isNotEmpty()) appendLine(dirStructure)
            append(snippetText)
            if (footer.isNotEmpty()) {
                appendLine()
                appendLine()
                append(footer)
            }
        }
    }

    /**
     * Copies the final output to the system clipboard and notifies the user.
     */
    private fun handleFinalOutputs(project: Project, finalOutput: String) {
        ClipCraftNotificationCenter.info("ClipCraft finished. Content length: ${finalOutput.length}")
        val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
        val selection = java.awt.datatransfer.StringSelection(finalOutput)
        clipboard.setContents(selection, selection)
    }
}
