package com.clipcraft.lint

data class LintIssue(val severity: LintSeverity, val filePath: String, val lineNumber: Int, val message: String) {
    fun formatMessage() = "[${severity.name}] $filePath:$lineNumber  $message"
}
enum class LintSeverity { ERROR, WARNING }
