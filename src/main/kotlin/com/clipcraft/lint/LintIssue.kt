package com.clipcraft.lint

data class LintIssue(
    val severity: LintSeverity,
    val filePath: String,
    val lineNumber: Int,
    val message: String
) {
    fun formatMessage(): String = "[${severity.name}] $filePath:$lineNumber  $message"
}
