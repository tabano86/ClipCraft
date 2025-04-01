package com.clipcraft.lint

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.Snippet
import com.clipcraft.model.SnippetGroup

object LintService {
    fun lintGroup(group: SnippetGroup, options: ClipCraftOptions): List<LintIssue> {
        val allIssues = group.snippets.flatMap { lintSnippet(it) }
        return when {
            options.lintErrorsOnly && !options.lintWarningsOnly ->
                allIssues.filter { it.severity == LintSeverity.ERROR }

            options.lintWarningsOnly && !options.lintErrorsOnly ->
                allIssues.filter { it.severity == LintSeverity.WARNING }

            else -> allIssues
        }
    }

    private fun lintSnippet(snippet: Snippet): List<LintIssue> {
        val lines = snippet.content.lines()
        return lines.mapIndexedNotNull { index, line ->
            val num = index + 1
            when {
                line.length > 120 -> LintIssue(
                    LintSeverity.WARNING,
                    snippet.filePath,
                    num,
                    "Line exceeds 120 characters (${line.length} chars).",
                )

                line.contains('\t') -> LintIssue(
                    LintSeverity.ERROR,
                    snippet.filePath,
                    num,
                    "Tabs are not allowed; use spaces instead.",
                )

                line.endsWith(" ") || line.endsWith("\t") ->
                    LintIssue(LintSeverity.WARNING, snippet.filePath, num, "Trailing whitespace found.")

                else -> null
            }
        }
    }
}
