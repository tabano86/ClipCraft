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
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UastFacade
import java.awt.datatransfer.StringSelection

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
        val options = ClipCraftSettings.getInstance().getCurrentProfile().options

        // Build snippet
        val finalSnippetContent = snippetText.withClipCraftHeaders()
        val snippet = Snippet(
            filePath = psiFile.virtualFile?.path ?: "InMemory",
            relativePath = null,
            fileName = psiFile.name,
            fileSizeBytes = finalSnippetContent.length.toLong(),
            lastModified = System.currentTimeMillis(),
            content = finalSnippetContent,
        )

        // Decide how to store the snippet
        if (options.addSnippetToQueue) {
            ClipCraftQueueService.getInstance(project).addSnippet(snippet)
        } else {
            ClipCraftSnippetsManager.getInstance(project).addSnippet(snippet)
        }

        // Combine all existing snippets
        val allSnippets = ClipCraftSnippetsManager.getInstance(project).getAllSnippets()
        val group = SnippetGroup("Aggregated Snippets").apply {
            snippets.addAll(allSnippets)
        }

        // Lint if desired
        val lintResults = if (options.showLint) LintService.lintGroup(group, options) else emptyList()

        // Optionally gather IntelliJ Problems
        val ideProblems = if (options.includeIdeProblems) gatherIdeProblems(psiFile) else emptyList()

        val finalOutput = aggregateFinalOutput(project, group, options, lintResults, ideProblems)
        deliverOutput(project, finalOutput, options)

        ClipCraftNotificationCenter.info("Snippet added and aggregated output processed.")
    }

    private fun aggregateFinalOutput(
        project: Project,
        group: SnippetGroup,
        options: com.clipcraft.model.ClipCraftOptions,
        lintResults: List<com.clipcraft.lint.LintIssue>,
        ideProblems: List<String>,
    ): String {
        val codeBlocks = CodeFormatter.formatSnippets(group.snippets, options).joinToString("\n---\n")

        val dirStruct = if (options.includeDirectorySummary) {
            if (options.hierarchicalDirectorySummary) {
                buildHierarchicalDirectoryTree(group.snippets)
            } else {
                buildFlatDirectorySummary(group.snippets)
            }
        } else {
            ""
        }

        val lintSummary = if (options.showLint && lintResults.isNotEmpty() && options.includeLintInOutput) {
            "\n\nLint Summary:\n" + lintResults.joinToString("\n") { "- ${it.formatMessage()}" }
        } else {
            ""
        }

        val problemsSection = if (ideProblems.isNotEmpty()) {
            "\n\nIDE Problems:\n" + ideProblems.joinToString("\n") { "- $it" }
        } else {
            ""
        }

        val finalOutput = buildString {
            // CodeFormatter handles snippet-level metadata, headers, footers
            append(codeBlocks)
            if (dirStruct.isNotEmpty()) appendLine().appendLine(dirStruct)
            if (lintSummary.isNotEmpty()) appendLine(lintSummary)
            if (problemsSection.isNotEmpty()) appendLine(problemsSection)
        }

        // Optionally apply macros
        if (!options.outputMacroTemplate.isNullOrBlank()) {
            val context = mapOf("output" to finalOutput, "timestamp" to System.currentTimeMillis().toString())
            return ClipCraftMacroManager.getInstance(project).expandMacro(options.outputMacroTemplate!!, context)
        }
        return finalOutput
    }

    private fun deliverOutput(project: Project, text: String, options: com.clipcraft.model.ClipCraftOptions) {
        when (options.outputTarget) {
            com.clipcraft.model.OutputTarget.CLIPBOARD -> copyToClipboard(text)
            com.clipcraft.model.OutputTarget.MACRO_ONLY -> {
                // In MACRO_ONLY mode, we do nothing else here unless you want logging, etc.
            }

            com.clipcraft.model.OutputTarget.BOTH -> {
                // Macro is presumably done above, now also copy to clipboard
                copyToClipboard(text)
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val selection = StringSelection(text)
        CopyPasteManager.getInstance().setContents(selection)
    }

    // Example: hierarchical directory summary
    private fun buildHierarchicalDirectoryTree(snippets: List<Snippet>): String {
        val rootMap = mutableMapOf<String, MutableList<String>>() // top-level folder -> list of sub-paths
        for (s in snippets) {
            val segments = s.filePath.split(Regex("[/\\\\]"))
            rootMap.getOrPut(segments.first()) { mutableListOf() } += segments.drop(1).joinToString("/")
        }
        val sb = StringBuilder("\nHierarchical Directory Tree:\n")
        for ((folder, children) in rootMap) {
            sb.append(folder).append("\n")
            for (child in children) {
                sb.append("  └─ ").append(child).append("\n")
            }
        }
        return sb.toString()
    }

    // Example: flat directory summary
    private fun buildFlatDirectorySummary(snippets: List<Snippet>): String {
        val lines = snippets.mapNotNull { it.relativePath }.distinct().sorted()
        if (lines.isEmpty()) return ""
        return "Directory Structure:\n" + lines.joinToString("\n") { "  $it" }
    }

    private fun Editor.extractSnippetOrMethod(psiFile: PsiFile): String {
        selectionModel.selectedText?.let { return it }
        val elem = psiFile.findElementAt(caretModel.offset) ?: return ""
        val method = UastFacade.convertElementWithParent(elem, UMethod::class.java) as? UMethod
        return method?.sourcePsi?.text.orEmpty()
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

    private fun gatherIdeProblems(psiFile: PsiFile): List<String> {
        val project = psiFile.project
        val document = PsiDocumentManager.getInstance(project).getDocument(psiFile) ?: return emptyList()
        val result = mutableListOf<String>()

        ApplicationManager.getApplication().runReadAction {
            // Collect all HighlightInfo objects for this file
            val highlightInfos = mutableListOf<HighlightInfo>()
            val startOffset = 0
            val endOffset = document.textLength

            DaemonCodeAnalyzerEx.processHighlights(
                document,
                project,
                null, // Pass null to collect all severities
                startOffset,
                endOffset,
            ) { info ->
                highlightInfos.add(info)
                true // returning true means "keep processing"
            }

            // Convert each HighlightInfo into a textual summary
            for (info in highlightInfos) {
                val lineNumber = document.getLineNumber(info.startOffset) + 1
                val severity = info.severity // e.g. HighlightSeverity.WARNING, ERROR, etc.
                val message = info.description // The user-visible text (e.g., "Unused import")

                // Some highlights may have a null 'description', fallback to summary if needed:
                val msg = message ?: info.text ?: "Unknown issue"

                // Only show issues of a certain severity if desired,
                // or keep everything:
                if (severity >= HighlightSeverity.INFORMATION) {
                    result += "[$severity] line:$lineNumber  $msg"
                }
            }
        }

        return result
    }
}
