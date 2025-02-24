package com.clipcraft.actions

import com.clipcraft.integration.ClipCraftGitIntegration
import com.clipcraft.lint.LintIssue
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
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.awt.Toolkit
import java.io.File
import java.nio.file.Files
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ClipCraftAction : AnAction() {

    private data class ProcessingStats(var processedFiles: Int = 0, var errorFiles: Int = 0)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val vfsFiles = e.getData(LangDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        val paths = vfsFiles.map { it.path }
        val profileManager = project.getService(ClipCraftProjectProfileManager::class.java)
        val activeProfile = profileManager?.getActiveProfile() ?: ClipCraftSettings.getInstance().getCurrentProfile()
        val options = activeProfile.options.apply { resolveConflicts() }
        IgnoreUtil.parseCustomIgnoreFiles(options, project.basePath ?: "", emptyList())
        when (options.concurrencyMode) {
            ConcurrencyMode.DISABLED -> runSequential(project, paths, options)
            ConcurrencyMode.THREAD_POOL -> runThreadPool(project, paths, options)
            ConcurrencyMode.COROUTINES -> runCoroutines(project, paths, options)
        }
    }

    private fun runSequential(project: Project, paths: List<String>, opts: ClipCraftOptions) {
        project.getService(ClipCraftPerformanceMetrics::class.java)?.startProcessing()
        val group = SnippetGroup("Snippets")
        val stats = ProcessingStats()
        paths.forEach { processPath(File(it), project, opts, group, null, stats) }
        finalizeAndCopyOutput(project, group, opts, "ClipCraftAction(sequential)", stats)
    }

    private fun runThreadPool(project: Project, paths: List<String>, opts: ClipCraftOptions) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "ClipCraft ThreadPool", true) {
            override fun run(indicator: ProgressIndicator) {
                project.getService(ClipCraftPerformanceMetrics::class.java)?.startProcessing()
                val group = SnippetGroup("Snippets")
                val stats = ProcessingStats()
                val pool = Executors.newFixedThreadPool(opts.maxConcurrentTasks)
                paths.forEach { pool.submit { processPath(File(it), project, opts, group, indicator, stats) } }
                pool.shutdown()
                pool.awaitTermination(15, TimeUnit.MINUTES)
                finalizeAndCopyOutput(project, group, opts, "ClipCraftAction(thread pool)", stats)
            }
        })
    }

    private fun runCoroutines(project: Project, paths: List<String>, opts: ClipCraftOptions) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "ClipCraft Coroutines", true) {
            override fun run(indicator: ProgressIndicator) {
                project.getService(ClipCraftPerformanceMetrics::class.java)?.startProcessing()
                val group = SnippetGroup("Snippets")
                val stats = ProcessingStats()
                runBlocking(Dispatchers.Default) {
                    paths.map { async { processPath(File(it), project, opts, group, indicator, stats) } }.awaitAll()
                }
                finalizeAndCopyOutput(project, group, opts, "ClipCraftAction(coroutines)", stats)
            }
        })
    }

    private fun finalizeAndCopyOutput(
        project: Project,
        group: SnippetGroup,
        opts: ClipCraftOptions,
        opLabel: String,
        stats: ProcessingStats,
    ) {
        val lintResults = if (opts.showLint) LintService.lintGroup(group, opts) else emptyList()
        project.getService(LintResultsService::class.java)?.storeResults(lintResults)
        var output = buildFinalOutput(project, group, opts, lintResults, stats)
        Toolkit.getDefaultToolkit().systemClipboard.setContents(
            java.awt.datatransfer.StringSelection(output),
            null,
        )
        project.getService(ClipCraftPerformanceMetrics::class.java)?.stopProcessingAndLog(opLabel)
        ClipCraftNotificationCenter.info("ClipCraft finished. Total length: ${output.length}")
    }

    private fun processPath(
        file: File,
        project: Project,
        opts: ClipCraftOptions,
        group: SnippetGroup,
        indicator: ProgressIndicator?,
        stats: ProcessingStats,
    ) {
        if (indicator?.isCanceled == true) return
        if (!file.exists() || IgnoreUtil.shouldIgnore(file, opts, project.basePath ?: "")) return
        if (file.isDirectory) {
            Files.walk(file.toPath()).use { stream ->
                stream.filter { Files.isRegularFile(it) }
                    .forEach {
                        if (indicator?.isCanceled == true) return@forEach
                        processSingleFile(it.toFile(), project, opts, group, indicator, stats)
                    }
            }
        } else {
            processSingleFile(file, project, opts, group, indicator, stats)
        }
    }

    private fun processSingleFile(
        file: File,
        project: Project,
        opts: ClipCraftOptions,
        group: SnippetGroup,
        indicator: ProgressIndicator?,
        stats: ProcessingStats,
    ) {
        if (indicator?.isCanceled == true) return
        if (file.isHidden) return
        val ext = file.extension.lowercase()
        if (ext in listOf("svg", "png", "jpg", "jpeg", "gif") && !opts.includeImageFiles) {
            addSnippet(project, group, file, "[Image file: ${file.name} not included]", opts.includeGitInfo)
            synchronized(stats) { stats.processedFiles++ }
            return
        }
        val content = try {
            file.readText()
        } catch (ex: Exception) {
            synchronized(stats) { stats.errorFiles++ }
            ClipCraftNotificationCenter.warn("Failed to read file '${file.absolutePath}': ${ex.message}")
            "<Error: ${ex.message}>"
        }
        val snippet = addSnippet(project, group, file, content, opts.includeGitInfo)
        synchronized(stats) { stats.processedFiles++ }
        detectPsiLanguage(project, snippet)
    }

    private fun addSnippet(
        project: Project,
        group: SnippetGroup,
        file: File,
        content: String,
        enrichWithGit: Boolean,
    ): Snippet {
        var snippet = Snippet(
            filePath = file.absolutePath,
            relativePath = computeRelativePath(project.basePath, file),
            fileName = file.name,
            fileSizeBytes = file.length(),
            lastModified = file.lastModified(),
            content = content,
        )
        if (enrichWithGit) snippet = ClipCraftGitIntegration.enrichSnippetWithGitInfo(project, snippet)
        synchronized(group) { group.snippets.add(snippet) }
        return snippet
    }

    private fun computeRelativePath(basePath: String?, file: File): String? {
        if (basePath == null) return null
        return try {
            java.nio.file.Paths.get(basePath).relativize(file.toPath()).toString()
        } catch (ex: Exception) {
            file.absolutePath
        }
    }

    private fun detectPsiLanguage(project: Project, snippet: Snippet) {
        val vfile = LocalFileSystem.getInstance().findFileByIoFile(File(snippet.filePath)) ?: return
        val psiFile = PsiManager.getInstance(project).findFile(vfile) ?: return
        snippet.language = psiFile.language.id.ifBlank { guessLang(snippet.fileName) }
    }

    fun guessLang(filename: String?): String {
        val ext = filename?.substringAfterLast('.', "")?.lowercase() ?: return "none"
        return when (ext) {
            "java" -> "java"
            "kt", "kts" -> "kotlin"
            "js" -> "javascript"
            "ts" -> "typescript"
            "py" -> "python"
            "cpp", "cxx", "cc" -> "cpp"
            "cs" -> "csharp"
            "html" -> "html"
            "css" -> "css"
            else -> "none"
        }
    }

    private fun buildFinalOutput(
        project: Project,
        group: SnippetGroup,
        opts: ClipCraftOptions,
        lintResults: List<LintIssue>,
        stats: ProcessingStats,
    ): String {
        val header = opts.snippetHeaderText.orEmpty()
        val footer = opts.snippetFooterText.orEmpty()
        val sortedSnippets = group.snippets.sortedBy { it.fileName }
        val code = CodeFormatter.formatSnippets(sortedSnippets, opts).joinToString("\n---\n")
        val dirStruct = if (opts.includeDirectorySummary) {
            "Directory Structure:\n" + sortedSnippets.mapNotNull { it.relativePath }
                .distinct().sorted().joinToString("\n") { "  $it" } + "\n\n"
        } else {
            ""
        }
        val lintSummary = if (opts.showLint && lintResults.isNotEmpty() && opts.includeLintInOutput) {
            "\n\nLint Summary:\n" + lintResults.joinToString("\n") { "- ${it.formatMessage()}" }
        } else {
            ""
        }
        val languageSummary = if (sortedSnippets.isNotEmpty()) {
            "\n\nLanguage Summary:\n" + sortedSnippets.groupBy { it.language ?: "none" }
                .map { (lang, snippets) -> "$lang: ${snippets.size}" }
                .sorted().joinToString("\n")
        } else {
            ""
        }
        val processingStats =
            "\n\nProcessing Statistics:\nFiles Processed: ${stats.processedFiles}\nErrors: ${stats.errorFiles}"
        var output = buildString {
            if (header.isNotEmpty()) {
                appendLine(header); appendLine()
            }
            if (dirStruct.isNotEmpty()) appendLine(dirStruct)
            append(code)
            if (footer.isNotEmpty()) {
                appendLine(); appendLine(footer)
            }
            append(languageSummary)
            append(processingStats)
            append(lintSummary)
        }
        if (!opts.outputMacroTemplate.isNullOrBlank()) {
            val context = mapOf("output" to output, "timestamp" to System.currentTimeMillis().toString())
            output = ClipCraftMacroManager.getInstance(project).expandMacro(opts.outputMacroTemplate!!, context)
        }
        return output
    }
}
