package com.clipcraft.actions

import com.clipcraft.integration.ClipCraftGitIntegration
import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.Snippet
import com.clipcraft.model.SnippetGroup
import com.clipcraft.services.ClipCraftNotificationCenter
import com.clipcraft.services.ClipCraftPerformanceMetrics
import com.clipcraft.services.ClipCraftProjectProfileManager
import com.clipcraft.services.ClipCraftSettings
import com.clipcraft.util.ClipCraftFormatter
import com.clipcraft.util.ClipCraftIgnoreUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import java.io.File
import kotlin.concurrent.thread

class ClipCraftAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val virtualFiles = e.getData(LangDataKeys.VIRTUAL_FILE_ARRAY) ?: return

        val profileManager = project.getService(ClipCraftProjectProfileManager::class.java)
        val activeProfile = profileManager?.getProfile(
            ClipCraftSettings.getInstance().state.activeProfile.profileName
        ) ?: ClipCraftSettings.getInstance().state.activeProfile

        // Possibly parse .gitignore
        ClipCraftIgnoreUtil.parseGitIgnoreIfEnabled(activeProfile.options, project.basePath ?: "")

        if (activeProfile.options.concurrencyEnabled) {
            runConcurrently(project, virtualFiles.map { it.path }, activeProfile.options)
        } else {
            runSequentially(project, virtualFiles.map { it.path }, activeProfile.options)
        }
    }

    private fun runSequentially(project: Project, paths: List<String>, options: ClipCraftOptions) {
        val metrics = project.getService(ClipCraftPerformanceMetrics::class.java)
        metrics.startProcessing()

        val snippetGroup = SnippetGroup("Collected Snippets")
        for (path in paths) {
            processFileOrDirectory(File(path), project, options, snippetGroup)
        }

        val outputs = ClipCraftFormatter.formatSnippets(snippetGroup.snippets, options)
        handleFinalOutputs(project, outputs)

        metrics.stopProcessingAndLog("ClipCraftAction (sequential)")
    }

    private fun runConcurrently(project: Project, paths: List<String>, options: ClipCraftOptions) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Processing with ClipCraft", true) {
            override fun run(indicator: ProgressIndicator) {
                val metrics = project.getService(ClipCraftPerformanceMetrics::class.java)
                metrics.startProcessing()

                val snippetGroup = SnippetGroup("Collected Snippets")

                // We'll do concurrency in a simplistic approach: each file/dir in a separate thread
                val threads = mutableListOf<Thread>()
                for (path in paths) {
                    val t = thread {
                        processFileOrDirectory(File(path), project, options, snippetGroup)
                    }
                    threads.add(t)
                }
                // Wait for all
                threads.forEach { it.join() }

                val outputs = ClipCraftFormatter.formatSnippets(snippetGroup.snippets, options)
                handleFinalOutputs(project, outputs)

                metrics.stopProcessingAndLog("ClipCraftAction (concurrent)")
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
        if (ClipCraftIgnoreUtil.shouldIgnore(file, options, project.basePath ?: "")) return

        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                processFileOrDirectory(child, project, options, snippetGroup)
            }
        } else {
            val snippet = createSnippet(file, project, options)
            snippetGroup.snippets.add(snippet)
        }
    }

    private fun createSnippet(file: File, project: Project, options: ClipCraftOptions): Snippet {
        val content = file.readText()
        var snippet = Snippet(
            filePath = file.absolutePath,
            relativePath = file.relativeTo(File(project.basePath ?: "")).path,
            content = content,
            fileSizeBytes = file.length()
        )
        if (options.includeGitInfo) {
            snippet = ClipCraftGitIntegration.enrichSnippetWithGitInfo(project, snippet)
        }
        return snippet
    }

    private fun handleFinalOutputs(project: Project, outputs: List<String>) {
        // For demonstration, copy the first chunk to the clipboard, or show a message
        val fullContent = outputs.joinToString("\n---\n")
        ClipCraftNotificationCenter.info("ClipCraft finished processing. Content length: ${fullContent.length}")

        // Actually copy to clipboard:
        val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
        val selection = java.awt.datatransfer.StringSelection(fullContent)
        clipboard.setContents(selection, selection)
    }
}
