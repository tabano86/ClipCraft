package com.clipcraft.lint

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.Snippet
import com.clipcraft.model.SnippetGroup

/**
 * Central linting engine that checks code for basic issues.
 */
object LintService {

    /**
     * Run lint checks on all snippets in a group, returning a combined list of issues.
     */
    fun lintGroup(group: SnippetGroup, options: ClipCraftOptions): List<LintIssue> {
        val result = mutableListOf<LintIssue>()
        group.snippets.forEach { snippet ->
            result += lintSnippet(snippet)
        }
        return result
    }

    /**
     * A simple lint pass:
     * 1. Lines > 120 characters => WARNING
     * 2. Tabs => ERROR
     * 3. Trailing whitespace => WARNING
     */
    fun lintSnippet(snippet: Snippet): List<LintIssue> {
        val lines = snippet.content.lines()
        val issues = mutableListOf<LintIssue>()
        lines.forEachIndexed { index, line ->
            val lineNumber = index + 1
            // 1. Check length
            if (line.length > 120) {
                issues += LintIssue(
                    LintSeverity.WARNING,
                    snippet.filePath,
                    lineNumber,
                    "Line exceeds 120 characters (${line.length} chars).",
                )
            }
            // 2. Tabs
            if (line.contains('\t')) {
                issues += LintIssue(
                    LintSeverity.ERROR,
                    snippet.filePath,
                    lineNumber,
                    "Tabs are not allowed; use spaces instead.",
                )
            }
            // 3. Trailing whitespace
            if (line.endsWith(" ") || line.endsWith("\t")) {
                issues += LintIssue(
                    LintSeverity.WARNING,
                    snippet.filePath,
                    lineNumber,
                    "Trailing whitespace found.",
                )
            }
        }
        return issues
    }
}
