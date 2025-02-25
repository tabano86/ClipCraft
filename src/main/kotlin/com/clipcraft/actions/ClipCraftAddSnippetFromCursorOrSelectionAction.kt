package com.clipcraft.actions

import com.clipcraft.lint.LintService
import com.clipcraft.model.Snippet
import com.clipcraft.model.SnippetGroup
import com.clipcraft.services.ClipCraftMacroManager
import com.clipcraft.services.ClipCraftNotificationCenter
import com.clipcraft.services.ClipCraftQueueService
import com.clipcraft.services.ClipCraftSettings
import com.clipcraft.services.ClipCraftSnippetsManager
import com.clipcraft.util.CodeFormatter
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import java.awt.datatransfer.StringSelection
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UastFacade

class ClipCraftAddSnippetFromCursorOrSelectionAction : AnAction() {

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
        val finalSnippetContent = snippetText.withClipCraftHeaders()

        val snippet = Snippet(
            filePath = "InMemory",
            relativePath = null,
            fileName = "Snippet",
            fileSizeBytes = finalSnippetContent.length.toLong(),
            lastModified = System.currentTimeMillis(),
            content = finalSnippetContent,
        )

        val options = ClipCraftSettings.getInstance().getCurrentProfile().options
        val snippetManager = ClipCraftSnippetsManager.getInstance(project)

        // Decide where to store the newly created snippet
        if (options.addSnippetToQueue) {
            ClipCraftQueueService.getInstance(project).addSnippet(snippet)
        } else {
            snippetManager.addSnippet(snippet)
        }

        // Get all existing snippets and build a group
        val allSnippets = snippetManager.getAllSnippets()
        val group = SnippetGroup("Aggregated Snippets").apply {
            snippets.addAll(allSnippets)
        }

        // Lint if requested
        val lintResults = if (options.showLint) LintService.lintGroup(group, options) else emptyList()
        val finalOutput = aggregateFinalOutput(project, group, options, lintResults)

        CopyPasteManager.getInstance().setContents(StringSelection(finalOutput))
        ClipCraftNotificationCenter.info("Snippet added and aggregated output copied to clipboard.")
    }

    /**
     * Build the final output text from all snippets plus optional directory structure,
     * snippet headers, lint info, etc.
     */
    private fun aggregateFinalOutput(
        project: Project,
        group: SnippetGroup,
        options: com.clipcraft.model.ClipCraftOptions,
        lintResults: List<com.clipcraft.lint.LintIssue>,
    ): String {
        val header = options.snippetHeaderText.orEmpty()
        val footer = options.snippetFooterText.orEmpty()

        val code = CodeFormatter.formatSnippets(group.snippets, options).joinToString("\n---\n")

        val dirStruct = if (options.includeDirectorySummary) {
            "Directory Structure:\n" +
                group.snippets.mapNotNull { it.relativePath }
                    .distinct().sorted()
                    .joinToString("\n") { "  $it" } + "\n\n"
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
            val context = mapOf("output" to output, "timestamp" to System.currentTimeMillis().toString())
            output = ClipCraftMacroManager.getInstance(project).expandMacro(options.outputMacroTemplate!!, context)
        }
        return output
    }

    /**
     * Extension function that tries to extract user-selected text; if none, falls back
     * to finding a method around caret in the PSI.
     */
    private fun Editor.extractSnippetOrMethod(psiFile: PsiFile): String {
        selectionModel.selectedText?.let { return it }

        // findElementAt(offset) returns PsiElement?, so we do a safe-call or fallback
        val elem = psiFile.findElementAt(caretModel.offset) ?: return ""

        // Convert to a UMethod if possible
        val method = UastFacade.convertElementWithParent(elem, UMethod::class.java) as? UMethod
        return method?.sourcePsi?.text.orEmpty()
    }

    /**
     * Prepend snippet prefix and append snippet suffix, if configured in ClipCraftSettings.
     */
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
