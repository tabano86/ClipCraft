package com.clipcraft.lint

/**
 * Represents a linting message (error or warning).
 */
data class LintIssue(
    val severity: LintSeverity,
    val filePath: String,
    val lineNumber: Int,
    val message: String,
) {
    fun formatMessage(): String = "[${severity.name}] $filePath:$lineNumber  $message"
}

enum class LintSeverity {
    ERROR, WARNING
}
