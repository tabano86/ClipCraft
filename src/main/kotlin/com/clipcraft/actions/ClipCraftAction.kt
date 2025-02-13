package com.clipcraft.actions

import com.clipcraft.integration.ClipCraftGitIntegration
import com.clipcraft.lint.LintService
import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.ConcurrencyMode
import com.clipcraft.model.Snippet
import com.clipcraft.model.SnippetGroup
import com.clipcraft.services.ClipCraftMacroManager
import com.clipcraft.services.ClipCraftNotificationCenter
import com.clipcraft.services.ClipCraftPerformanceMetrics
import com.clipcraft.services.ClipCraftProjectProfileManager
import com.clipcraft.services.ClipCraftSettings
import com.clipcraft.services.LintResultsService
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
import java.awt.Toolkit
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ClipCraftAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val files = e.getData(LangDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        val mgr = project.getService(ClipCraftProjectProfileManager::class.java)
        val profile = mgr?.getActiveProfile() ?: ClipCraftSettings.getInstance().getCurrentProfile()
        val options = profile.options.also { it.resolveConflicts() }
        IgnoreUtil.parseCustomIgnoreFiles(options, project.basePath ?: "", emptyList())
        val paths = files.map { it.path }
        when (options.concurrencyMode) {
            ConcurrencyMode.DISABLED -> runSequential(project, paths, options)
            ConcurrencyMode.THREAD_POOL -> runThreadPool(project, paths, options)
            ConcurrencyMode.COROUTINES -> runCoroutines(project, paths, options)
        }
    }

    private fun runSequential(proj: Project, paths: List<String>, options: ClipCraftOptions) {
        val metrics = proj.getService(ClipCraftPerformanceMetrics::class.java)
        metrics?.startProcessing()
        val group = SnippetGroup("Snippets")
        paths.forEach { processFileOrDir(File(it), proj, options, group) }
        val lintResults = if (options.showLint) LintService.lintGroup(group, options) else emptyList()
        proj.getService(LintResultsService::class.java)?.storeResults(lintResults)
        val finalOutput = buildFinalOutput(proj, group, options, lintResults)
        copyToClipboard(proj, finalOutput)
        metrics?.stopProcessingAndLog("ClipCraftAction(sequential)")
    }

    private fun runThreadPool(proj: Project, paths: List<String>, options: ClipCraftOptions) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(proj, "ClipCraft ThreadPool", true) {
            override fun run(indicator: ProgressIndicator) {
                val metrics = proj.getService(ClipCraftPerformanceMetrics::class.java)
                metrics?.startProcessing()
                val group = SnippetGroup("Snippets")
                val pool = Executors.newFixedThreadPool(options.maxConcurrentTasks)
                paths.forEach { pool.submit { processFileOrDir(File(it), proj, options, group) } }
                pool.shutdown()
                pool.awaitTermination(15, TimeUnit.MINUTES)
                val lintResults = if (options.showLint) LintService.lintGroup(group, options) else emptyList()
                proj.getService(LintResultsService::class.java)?.storeResults(lintResults)
                val finalOutput = buildFinalOutput(proj, group, options, lintResults)
                copyToClipboard(proj, finalOutput)
                metrics?.stopProcessingAndLog("ClipCraftAction(thread pool)")
            }
        })
    }

    private fun runCoroutines(proj: Project, paths: List<String>, options: ClipCraftOptions) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(proj, "ClipCraft Coroutines", true) {
            override fun run(indicator: ProgressIndicator) {
                val metrics = proj.getService(ClipCraftPerformanceMetrics::class.java)
                metrics?.startProcessing()
                val group = SnippetGroup("Snippets")
                runBlocking(Dispatchers.Default) {
                    paths.map { async { processFileOrDir(File(it), proj, options, group) } }.awaitAll()
                }
                val lintResults = if (options.showLint) LintService.lintGroup(group, options) else emptyList()
                proj.getService(LintResultsService::class.java)?.storeResults(lintResults)
                val finalOutput = buildFinalOutput(proj, group, options, lintResults)
                copyToClipboard(proj, finalOutput)
                metrics?.stopProcessingAndLog("ClipCraftAction(coroutines)")
            }
        })
    }

    private fun processFileOrDir(file: File, proj: Project, options: ClipCraftOptions, group: SnippetGroup) {
        if (!file.exists() || IgnoreUtil.shouldIgnore(file, options, proj.basePath ?: "")) return
        if (file.isDirectory) {
            file.listFiles()?.forEach { processFileOrDir(it, proj, options, group) }
            return
        }
        val ext = file.extension.lowercase()
        if (ext in listOf("svg", "png", "jpg", "jpeg", "gif") && !options.includeImageFiles) {
            val placeholder = "[Image file: ${file.name} not included]"
            val snippet = Snippet(
                filePath = file.absolutePath,
                relativePath = proj.basePath?.let {
                    try {
                        file.relativeTo(File(it)).path
                    } catch (_: Exception) {
                        file.absolutePath
                    }
                },
                fileName = file.name,
                fileSizeBytes = file.length(),
                lastModified = file.lastModified(),
                content = placeholder,
            )
            val enriched =
                if (options.includeGitInfo) ClipCraftGitIntegration.enrichSnippetWithGitInfo(proj, snippet) else snippet
            synchronized(group) { group.snippets.add(enriched) }
            return
        }
        val content = try {
            file.readText()
        } catch (ex: Exception) {
            "<Error: ${ex.message}>"
        }
        var snippet = Snippet(
            filePath = file.absolutePath,
            relativePath = proj.basePath?.let {
                try {
                    file.relativeTo(File(it)).path
                } catch (_: Exception) {
                    file.absolutePath
                }
            },
            fileName = file.name,
            fileSizeBytes = file.length(),
            lastModified = file.lastModified(),
            content = content,
        )
        if (options.includeGitInfo) snippet = ClipCraftGitIntegration.enrichSnippetWithGitInfo(proj, snippet)
        synchronized(group) { group.snippets.add(snippet) }
    }

    private fun buildFinalOutput(
        proj: Project,
        group: SnippetGroup,
        options: ClipCraftOptions,
        lintResults: List<com.clipcraft.lint.LintIssue>,
    ): String {
        val header = options.snippetHeaderText.orEmpty()
        val footer = options.snippetFooterText.orEmpty()
        val code = CodeFormatter.formatSnippets(group.snippets, options).joinToString("\n---\n")
        val dirStruct = if (options.includeDirectorySummary) {
            "Directory Structure:\n" + group.snippets.mapNotNull { it.relativePath }
                .distinct().sorted().joinToString("\n") { "  $it" } + "\n\n"
        } else {
            ""
        }
        val lintSummary = if (options.showLint && lintResults.isNotEmpty() && options.includeLintInOutput) {
            "\n\nLint Summary:\n" + lintResults.joinToString("\n") { "- ${it.formatMessage()}" }
        } else {
            ""
        }
        var output = buildString {
            if (header.isNotEmpty()) appendLine(header).appendLine()
            if (dirStruct.isNotEmpty()) appendLine(dirStruct)
            append(code)
            if (footer.isNotEmpty()) appendLine().appendLine(footer)
            if (lintSummary.isNotEmpty()) appendLine(lintSummary)
        }
        if (!options.outputMacroTemplate.isNullOrBlank()) {
            val context = mapOf(
                "output" to output,
                "timestamp" to System.currentTimeMillis().toString(),
            )
            output = ClipCraftMacroManager.getInstance(proj).expandMacro(options.outputMacroTemplate!!, context)
        }
        return output
    }

    private fun copyToClipboard(proj: Project, output: String) {
        ClipCraftNotificationCenter.info("ClipCraft finished. Total length: ${output.length}")
        Toolkit.getDefaultToolkit().systemClipboard.setContents(java.awt.datatransfer.StringSelection(output), null)
    }
}
