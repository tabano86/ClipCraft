package com.clipcraft.actions

import ClipCraftNotificationCenter
import com.clipcraft.lint.LintService
import com.clipcraft.model.Snippet
import com.clipcraft.model.SnippetGroup
import com.clipcraft.services.ClipCraftMacroManager
import com.clipcraft.services.ClipCraftQueueService
import com.clipcraft.services.ClipCraftSettings
import com.clipcraft.services.ClipCraftSnippetsManager
import com.clipcraft.ui.extractSnippetOrMethod
import com.clipcraft.util.CodeFormatter
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class ClipCraftAddSnippetFromCursorAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR)
        val psiFile = event.getData(CommonDataKeys.PSI_FILE)
        if (editor == null || psiFile == null) {
            ClipCraftNotificationCenter.warn("No editor or file found.")
            return
        }

        val snippetText = editor.extractSnippetOrMethod(psiFile)
        if (snippetText.isBlank()) {
            ClipCraftNotificationCenter.warn("No valid snippet could be extracted.")
            return
        }

        val project = event.project ?: return
        val options = ClipCraftSettings.getInstance().getCurrentProfile().options
        val finalSnippetContent = snippetText.withClipCraftHeaders()
        val snippet = Snippet(
            filePath = psiFile.virtualFile?.path ?: "InMemory",
            relativePath = null,
            fileName = psiFile.name,
            fileSizeBytes = finalSnippetContent.length.toLong(),
            lastModified = System.currentTimeMillis(),
            content = finalSnippetContent,
        )

        if (options.addSnippetToQueue) {
            ClipCraftQueueService.getInstance(project).addSnippet(snippet)
        } else {
            ClipCraftSnippetsManager.getInstance(project).addSnippet(snippet)
        }

        // Build aggregated snippet output
        val allSnippets = ClipCraftSnippetsManager.getInstance(project).getAllSnippets()
        val group = SnippetGroup("Aggregated Snippets").apply {
            snippets.addAll(allSnippets)
        }

        val lintResults = if (options.showLint) LintService.lintGroup(group, options) else emptyList()
        val finalOutput = aggregateFinalOutput(project, group, options, lintResults)
        deliverOutput(finalOutput, options)

        ClipCraftNotificationCenter.info("Snippet added and aggregated output processed.")
    }

    private fun aggregateFinalOutput(
        project: com.intellij.openapi.project.Project,
        group: SnippetGroup,
        options: com.clipcraft.model.ClipCraftOptions,
        lintResults: List<com.clipcraft.lint.LintIssue>,
    ): String {
        val codeBlocks = CodeFormatter.formatSnippets(group.snippets, options)
        // ... optional directory summary, lint summary, etc. omitted for brevity
        val finalOutput = buildString {
            append(codeBlocks)
            if (options.includeLintInOutput && lintResults.isNotEmpty()) {
                appendLine("\n\nLint Summary:")
                lintResults.forEach { appendLine("- ${it.formatMessage()}") }
            }
        }
        if (!options.outputMacroTemplate.isNullOrBlank()) {
            val context = mapOf("output" to finalOutput, "timestamp" to System.currentTimeMillis().toString())
            return ClipCraftMacroManager.getInstance(project).expandMacro(options.outputMacroTemplate!!, context)
        }
        return finalOutput
    }

    private fun deliverOutput(text: String, options: com.clipcraft.model.ClipCraftOptions) {
        when (options.outputTarget) {
            com.clipcraft.model.OutputTarget.CLIPBOARD,
            com.clipcraft.model.OutputTarget.BOTH,
            -> {
                val success = try {
                    val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
                    clipboard.setContents(java.awt.datatransfer.StringSelection(text), null)
                    true
                } catch (ex: Exception) {
                    false
                }
                if (!success) {
                    ClipCraftNotificationCenter.error("Failed to copy to clipboard.")
                }
            }

            else -> {}
        }
    }

    private fun String.withClipCraftHeaders(): String {
        val settings = ClipCraftSettings.getInstance()
        val prefix = settings.getSnippetPrefix().trim()
        val suffix = settings.getSnippetSuffix().trim()
        return buildString {
            if (prefix.isNotEmpty()) appendLine(prefix).appendLine()
            append(this@withClipCraftHeaders)
            if (suffix.isNotEmpty()) appendLine().appendLine(suffix)
        }
    }
}
