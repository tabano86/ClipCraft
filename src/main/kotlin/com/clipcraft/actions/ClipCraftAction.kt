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
import java.awt.Toolkit
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ClipCraftAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val p = e.project ?: return
        val vf = e.getData(LangDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        val mgr = p.getService(ClipCraftProjectProfileManager::class.java)
        val profile = mgr?.getActiveProfile() ?: ClipCraftSettings.getInstance().getCurrentProfile()
        val o = profile.options
        o.resolveConflicts()
        IgnoreUtil.parseCustomIgnoreFiles(o, p.basePath ?: "", emptyList())
        val paths = vf.map { it.path }
        when (o.concurrencyMode) {
            ConcurrencyMode.DISABLED -> runSequential(p, paths, o)
            ConcurrencyMode.THREAD_POOL -> runThreadPool(p, paths, o)
            ConcurrencyMode.COROUTINES -> runCoroutines(p, paths, o)
        }
    }

    fun runSequential(proj: Project, paths: List<String>, o: ClipCraftOptions) {
        val metrics = proj.getService(ClipCraftPerformanceMetrics::class.java)
        metrics?.startProcessing()
        val group = SnippetGroup("Snippets")
        paths.forEach { processFileOrDir(File(it), proj, o, group) }
        val finalOutput = buildFinalOutput(group, o)
        copyToClipboard(proj, finalOutput)
        metrics?.stopProcessingAndLog("ClipCraftAction(sequential)")
    }

    fun runThreadPool(proj: Project, paths: List<String>, o: ClipCraftOptions) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(proj, "ClipCraft ThreadPool", true) {
            override fun run(indicator: ProgressIndicator) {
                val metrics = proj.getService(ClipCraftPerformanceMetrics::class.java)
                metrics?.startProcessing()
                val group = SnippetGroup("Snippets")
                val pool = Executors.newFixedThreadPool(o.maxConcurrentTasks)
                paths.forEach {
                    pool.submit { processFileOrDir(File(it), proj, o, group) }
                }
                pool.shutdown()
                pool.awaitTermination(15, TimeUnit.MINUTES)
                val finalOutput = buildFinalOutput(group, o)
                copyToClipboard(proj, finalOutput)
                metrics?.stopProcessingAndLog("ClipCraftAction(thread pool)")
            }
        })
    }

    fun runCoroutines(proj: Project, paths: List<String>, o: ClipCraftOptions) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(proj, "ClipCraft Coroutines", true) {
            override fun run(indicator: ProgressIndicator) {
                val metrics = proj.getService(ClipCraftPerformanceMetrics::class.java)
                metrics?.startProcessing()
                val group = SnippetGroup("Snippets")
                runBlocking(Dispatchers.Default) {
                    paths.map {
                        async { processFileOrDir(File(it), proj, o, group) }
                    }.awaitAll()
                }
                val finalOutput = buildFinalOutput(group, o)
                copyToClipboard(proj, finalOutput)
                metrics?.stopProcessingAndLog("ClipCraftAction(coroutines)")
            }
        })
    }

    fun processFileOrDir(file: File, proj: Project, o: ClipCraftOptions, group: SnippetGroup) {
        if (!file.exists()) return
        if (IgnoreUtil.shouldIgnore(file, o, proj.basePath ?: "")) return
        if (file.isDirectory) {
            file.listFiles()?.forEach { processFileOrDir(it, proj, o, group) }
            return
        }
        if (o.detectBinary && isBinary(file, o.binaryCheckThreshold)) return
        val content = try { file.readText() } catch (ex: Exception) { "<Error: ${ex.message}>" }
        var s = Snippet(
            filePath = file.absolutePath,
            relativePath = proj.basePath?.let { base -> try { file.relativeTo(File(base)).path } catch (_: Exception) { file.absolutePath } },
            fileName = file.name,
            fileSizeBytes = file.length(),
            lastModified = file.lastModified(),
            content = content
        )
        if (o.includeGitInfo) s = ClipCraftGitIntegration.enrichSnippetWithGitInfo(proj, s)
        synchronized(group) { group.snippets.add(s) }
    }

    fun isBinary(file: File, threshold: Int): Boolean {
        return try {
            val bytes = ByteArray(threshold)
            val read = file.inputStream().read(bytes)
            if (read <= 0) false else bytes.take(read).count { it < 9 || it == 127.toByte() || it > 126 } > read / 3
        } catch (e: Exception) {
            false
        }
    }

    fun buildFinalOutput(group: SnippetGroup, o: ClipCraftOptions): String {
        val h = o.gptHeaderText.orEmpty()
        val f = o.gptFooterText.orEmpty()
        val text = CodeFormatter.formatSnippets(group.snippets, o).joinToString("\n---\n")
        val dirStruct = if (o.includeDirectorySummary) {
            val sorted = group.snippets.mapNotNull { it.relativePath }.distinct().sorted()
            "Directory Structure:\n" + sorted.joinToString("\n") { "  $it" } + "\n\n"
        } else ""
        return buildString {
            if (h.isNotEmpty()) appendLine(h).appendLine()
            if (dirStruct.isNotEmpty()) appendLine(dirStruct)
            append(text)
            if (f.isNotEmpty()) {
                appendLine().appendLine()
                append(f)
            }
        }
    }

    fun copyToClipboard(proj: Project, output: String) {
        ClipCraftNotificationCenter.info("ClipCraft finished. Length: ${output.length}")
        val cb = Toolkit.getDefaultToolkit().systemClipboard
        val sel = java.awt.datatransfer.StringSelection(output)
        cb.setContents(sel, sel)
    }
}
