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

        // Build group from all current snippets
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

    /**
     * Combine snippet text, hierarchical directory summary, lint results, and possibly IDE problems.
     */
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
            // The CodeFormatter itself handles snippetHeaderText + metadata + snippetFooterText on each snippet.
            // If you want a top-level header, you can add it here.
            append(codeBlocks)
            if (dirStruct.isNotEmpty()) appendLine().appendLine(dirStruct)
            if (lintSummary.isNotEmpty()) appendLine(lintSummary)
            if (problemsSection.isNotEmpty()) appendLine(problemsSection)
        }

        // Optionally expand macros
        if (!options.outputMacroTemplate.isNullOrBlank()) {
            val context = mapOf("output" to finalOutput, "timestamp" to System.currentTimeMillis().toString())
            return ClipCraftMacroManager.getInstance(project).expandMacro(options.outputMacroTemplate!!, context)
        }
        return finalOutput
    }

    /**
     * Actually delivers the final output according to the user’s setting:
     *  - CLIPBOARD
     *  - MACRO_ONLY
     *  - BOTH
     */
    private fun deliverOutput(project: Project, text: String, options: com.clipcraft.model.ClipCraftOptions) {
        when (options.outputTarget) {
            com.clipcraft.model.OutputTarget.CLIPBOARD -> copyToClipboard(text)
            com.clipcraft.model.OutputTarget.MACRO_ONLY -> {
                // You could do anything else here, but typically the macro expansion is done above
                // so "MACRO_ONLY" means we just skip the direct clipboard copy
            }
            com.clipcraft.model.OutputTarget.BOTH -> {
                copyToClipboard(text)
                // Macro expansion is already done in aggregateFinalOutput if desired
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val selection = StringSelection(text)
        CopyPasteManager.getInstance().setContents(selection)
    }

    /**
     * Build a hierarchical directory summary in Markdown, for example:
     * ```
     * RootFolder
     * ├─ subfolder
     * │  └─ SomeFile.kt
     * └─ AnotherFile.kt
     * ```
     */
    private fun buildHierarchicalDirectoryTree(snippets: List<Snippet>): String {
        // We'll do a very simple representation: group by directory path
        // This is just an illustration and can be made more sophisticated.
        val rootMap = mutableMapOf<String, MutableList<String>>() // path -> children
        for (s in snippets) {
            val path = s.filePath
            // parse path segments
            val segments = path.split(Regex("[/\\\\]"))
            // We won't implement a real tree for brevity, just a naive approach
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

    /**
     * Simple flat directory summary
     */
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

    /**
     * Example method to gather IntelliJ “File Problems” from the PSI or highlighting system.
     * This is a placeholder that you can adapt to your environment.
     */
    private fun gatherIdeProblems(psiFile: PsiFile): List<String> {
        // TODO: implement a real method that queries the daemon highlight passes, etc.
        // For now, we return a fake placeholder.
        return listOf("Example: 'Missing semicolon' at line 10", "Example: 'Unused import' at line 24")
    }
}
