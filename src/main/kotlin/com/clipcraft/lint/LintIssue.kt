package com.clipcraft.lint

/**
 * Represents a lint finding, with a severity (Error/Warning),
 * a message, and optional file/line data.
 */
data class LintIssue(
    val severity: LintSeverity,
    val filePath: String,
    val lineNumber: Int,
    val message: String,
) {
    fun formatMessage(): String {
        return "[${severity.name}] $filePath:$lineNumber  $message"
    }
}

enum class LintSeverity {
    ERROR,
    WARNING,
}
