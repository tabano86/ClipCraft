package com.clipcraft.services

import com.clipcraft.model.ExportOptions
import com.clipcraft.model.OutputFormat
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class FormattedOutput(
    val content: String,
    val metadata: ExportMetadata
)

data class ExportMetadata(
    val filesProcessed: Int,
    val filesSkipped: Int,
    val totalBytes: Long,
    val estimatedTokens: Int,
    val exportTime: LocalDateTime,
    val projectName: String?,
    val gitBranch: String?,
    val gitCommit: String?
)

data class FileEntry(
    val file: VirtualFile,
    val relativePath: Path,
    val content: String,
    val language: String,
    val lineCount: Int,
    val byteSize: Long
)

object OutputFormatter {

    fun format(
        entries: List<FileEntry>,
        options: ExportOptions,
        metadata: ExportMetadata
    ): FormattedOutput {
        val content = when (options.outputFormat) {
            OutputFormat.MARKDOWN -> formatMarkdown(entries, options, metadata)
            OutputFormat.MARKDOWN_WITH_TOC -> formatMarkdownWithTOC(entries, options, metadata)
            OutputFormat.XML -> formatXML(entries, options, metadata)
            OutputFormat.JSON -> formatJSON(entries, options, metadata)
            OutputFormat.PLAIN_TEXT -> formatPlainText(entries, options, metadata)
            OutputFormat.HTML -> formatHTML(entries, options, metadata)
            OutputFormat.CLAUDE_OPTIMIZED -> formatClaudeOptimized(entries, options, metadata)
            OutputFormat.CHATGPT_OPTIMIZED -> formatChatGPTOptimized(entries, options, metadata)
            OutputFormat.GEMINI_OPTIMIZED -> formatGeminiOptimized(entries, options, metadata)
        }

        return FormattedOutput(content, metadata)
    }

    private fun formatMarkdown(
        entries: List<FileEntry>,
        options: ExportOptions,
        metadata: ExportMetadata
    ): String {
        val sb = StringBuilder()

        if (options.includeMetadata) {
            sb.append(buildMetadataHeader(metadata, options))
        }

        val sortedEntries = sortEntries(entries, options)
        val groupedEntries = if (options.groupByDirectory) {
            sortedEntries.groupBy { it.file.parent?.path ?: "" }
        } else {
            mapOf("" to sortedEntries)
        }

        groupedEntries.forEach { (dir, dirEntries) ->
            if (options.groupByDirectory && dir.isNotEmpty()) {
                sb.append("\n## Directory: `$dir`\n\n")
            }

            dirEntries.forEach { entry ->
                sb.append("---\n")
                sb.append("### ${formatFilePath(entry.relativePath, options)}\n\n")

                if (options.includeStatistics) {
                    sb.append("*Size: ${TokenEstimator.formatByteSize(entry.byteSize)} | ")
                    sb.append("Lines: ${entry.lineCount} | ")
                    sb.append("Language: ${entry.language}*\n\n")
                }

                sb.append("```${entry.language}\n")
                sb.append(formatContent(entry.content, options))
                sb.append("\n```\n\n")
            }
        }

        return sb.toString()
    }

    private fun formatMarkdownWithTOC(
        entries: List<FileEntry>,
        options: ExportOptions,
        metadata: ExportMetadata
    ): String {
        val sb = StringBuilder()

        if (options.includeMetadata) {
            sb.append(buildMetadataHeader(metadata, options))
        }

        // Table of Contents
        sb.append("## Table of Contents\n\n")
        entries.forEachIndexed { index, entry ->
            val anchor = entry.relativePath.toString()
                .replace("[^a-zA-Z0-9-]".toRegex(), "-")
                .toLowerCase()
            sb.append("${index + 1}. [${entry.relativePath}](#${anchor})\n")
        }
        sb.append("\n---\n\n")

        // Content
        sb.append(formatMarkdown(entries, options.copy(includeMetadata = false), metadata))

        return sb.toString()
    }

    private fun formatXML(
        entries: List<FileEntry>,
        options: ExportOptions,
        metadata: ExportMetadata
    ): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<export>\n")

        if (options.includeMetadata) {
            sb.append("  <metadata>\n")
            sb.append("    <filesProcessed>${metadata.filesProcessed}</filesProcessed>\n")
            sb.append("    <filesSkipped>${metadata.filesSkipped}</filesSkipped>\n")
            sb.append("    <totalBytes>${metadata.totalBytes}</totalBytes>\n")
            sb.append("    <estimatedTokens>${metadata.estimatedTokens}</estimatedTokens>\n")
            sb.append("    <exportTime>${metadata.exportTime}</exportTime>\n")
            metadata.projectName?.let { sb.append("    <projectName>$it</projectName>\n") }
            metadata.gitBranch?.let { sb.append("    <gitBranch>$it</gitBranch>\n") }
            metadata.gitCommit?.let { sb.append("    <gitCommit>$it</gitCommit>\n") }
            sb.append("  </metadata>\n")
        }

        sb.append("  <files>\n")
        entries.forEach { entry ->
            sb.append("    <file>\n")
            sb.append("      <path>${escapeXml(entry.relativePath.toString())}</path>\n")
            sb.append("      <language>${entry.language}</language>\n")
            sb.append("      <lineCount>${entry.lineCount}</lineCount>\n")
            sb.append("      <byteSize>${entry.byteSize}</byteSize>\n")
            sb.append("      <content><![CDATA[${formatContent(entry.content, options)}]]></content>\n")
            sb.append("    </file>\n")
        }
        sb.append("  </files>\n")
        sb.append("</export>\n")

        return sb.toString()
    }

    private fun formatJSON(
        entries: List<FileEntry>,
        options: ExportOptions,
        metadata: ExportMetadata
    ): String {
        val sb = StringBuilder()
        sb.append("{\n")

        if (options.includeMetadata) {
            sb.append("  \"metadata\": {\n")
            sb.append("    \"filesProcessed\": ${metadata.filesProcessed},\n")
            sb.append("    \"filesSkipped\": ${metadata.filesSkipped},\n")
            sb.append("    \"totalBytes\": ${metadata.totalBytes},\n")
            sb.append("    \"estimatedTokens\": ${metadata.estimatedTokens},\n")
            sb.append("    \"exportTime\": \"${metadata.exportTime}\",\n")
            metadata.projectName?.let { sb.append("    \"projectName\": \"$it\",\n") }
            metadata.gitBranch?.let { sb.append("    \"gitBranch\": \"$it\",\n") }
            metadata.gitCommit?.let { sb.append("    \"gitCommit\": \"$it\",\n") }
            sb.append("  },\n")
        }

        sb.append("  \"files\": [\n")
        entries.forEachIndexed { index, entry ->
            sb.append("    {\n")
            sb.append("      \"path\": \"${escapeJson(entry.relativePath.toString())}\",\n")
            sb.append("      \"language\": \"${entry.language}\",\n")
            sb.append("      \"lineCount\": ${entry.lineCount},\n")
            sb.append("      \"byteSize\": ${entry.byteSize},\n")
            sb.append("      \"content\": \"${escapeJson(formatContent(entry.content, options))}\"\n")
            sb.append("    }")
            if (index < entries.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ]\n")
        sb.append("}\n")

        return sb.toString()
    }

    private fun formatPlainText(
        entries: List<FileEntry>,
        options: ExportOptions,
        metadata: ExportMetadata
    ): String {
        val sb = StringBuilder()

        if (options.includeMetadata) {
            sb.append("=" .repeat(80))
            sb.append("\nEXPORT METADATA\n")
            sb.append("=".repeat(80))
            sb.append("\nFiles Processed: ${metadata.filesProcessed}\n")
            sb.append("Files Skipped: ${metadata.filesSkipped}\n")
            sb.append("Total Bytes: ${TokenEstimator.formatByteSize(metadata.totalBytes)}\n")
            sb.append("Estimated Tokens: ${TokenEstimator.formatTokenCount(metadata.estimatedTokens)}\n")
            sb.append("Export Time: ${metadata.exportTime}\n")
            metadata.projectName?.let { sb.append("Project: $it\n") }
            metadata.gitBranch?.let { sb.append("Git Branch: $it\n") }
            metadata.gitCommit?.let { sb.append("Git Commit: $it\n") }
            sb.append("=".repeat(80))
            sb.append("\n\n")
        }

        entries.forEach { entry ->
            sb.append("-".repeat(80))
            sb.append("\nFile: ${entry.relativePath}\n")
            sb.append("Language: ${entry.language} | Lines: ${entry.lineCount} | Size: ${TokenEstimator.formatByteSize(entry.byteSize)}\n")
            sb.append("-".repeat(80))
            sb.append("\n")
            sb.append(formatContent(entry.content, options))
            sb.append("\n\n")
        }

        return sb.toString()
    }

    private fun formatHTML(
        entries: List<FileEntry>,
        options: ExportOptions,
        metadata: ExportMetadata
    ): String {
        val sb = StringBuilder()
        sb.append("<!DOCTYPE html>\n<html>\n<head>\n")
        sb.append("  <meta charset=\"UTF-8\">\n")
        sb.append("  <title>ClipCraft Export</title>\n")
        sb.append("  <style>\n")
        sb.append("    body { font-family: monospace; margin: 20px; }\n")
        sb.append("    .metadata { background: #f0f0f0; padding: 15px; margin-bottom: 20px; }\n")
        sb.append("    .file-entry { margin-bottom: 30px; border: 1px solid #ccc; padding: 10px; }\n")
        sb.append("    .file-header { background: #e0e0e0; padding: 10px; font-weight: bold; }\n")
        sb.append("    pre { background: #fafafa; padding: 15px; overflow-x: auto; }\n")
        sb.append("  </style>\n")
        sb.append("</head>\n<body>\n")

        if (options.includeMetadata) {
            sb.append("  <div class=\"metadata\">\n")
            sb.append("    <h2>Export Metadata</h2>\n")
            sb.append("    <p>Files Processed: ${metadata.filesProcessed}</p>\n")
            sb.append("    <p>Files Skipped: ${metadata.filesSkipped}</p>\n")
            sb.append("    <p>Total Bytes: ${TokenEstimator.formatByteSize(metadata.totalBytes)}</p>\n")
            sb.append("    <p>Estimated Tokens: ${TokenEstimator.formatTokenCount(metadata.estimatedTokens)}</p>\n")
            sb.append("    <p>Export Time: ${metadata.exportTime}</p>\n")
            metadata.projectName?.let { sb.append("    <p>Project: $it</p>\n") }
            metadata.gitBranch?.let { sb.append("    <p>Git Branch: $it</p>\n") }
            metadata.gitCommit?.let { sb.append("    <p>Git Commit: $it</p>\n") }
            sb.append("  </div>\n")
        }

        entries.forEach { entry ->
            sb.append("  <div class=\"file-entry\">\n")
            sb.append("    <div class=\"file-header\">${escapeHtml(entry.relativePath.toString())}</div>\n")
            sb.append("    <p>Language: ${entry.language} | Lines: ${entry.lineCount} | Size: ${TokenEstimator.formatByteSize(entry.byteSize)}</p>\n")
            sb.append("    <pre><code>${escapeHtml(formatContent(entry.content, options))}</code></pre>\n")
            sb.append("  </div>\n")
        }

        sb.append("</body>\n</html>\n")
        return sb.toString()
    }

    private fun formatClaudeOptimized(
        entries: List<FileEntry>,
        options: ExportOptions,
        metadata: ExportMetadata
    ): String {
        val sb = StringBuilder()

        sb.append("# Project Export for Claude\n\n")

        if (options.includeMetadata) {
            sb.append("## Context Information\n\n")
            sb.append("- Files: ${metadata.filesProcessed}\n")
            sb.append("- Estimated tokens: ${TokenEstimator.formatTokenCount(metadata.estimatedTokens)}\n")
            sb.append("- Context window fit: ${TokenEstimator.getContextWindowFit(metadata.estimatedTokens)}\n")
            metadata.projectName?.let { sb.append("- Project: $it\n") }
            metadata.gitBranch?.let { sb.append("- Branch: $it\n") }
            sb.append("\n")
        }

        sb.append("## Files\n\n")

        entries.forEach { entry ->
            sb.append("<file path=\"${entry.relativePath}\">\n")
            sb.append(formatContent(entry.content, options))
            sb.append("\n</file>\n\n")
        }

        return sb.toString()
    }

    private fun formatChatGPTOptimized(
        entries: List<FileEntry>,
        options: ExportOptions,
        metadata: ExportMetadata
    ): String {
        val sb = StringBuilder()

        sb.append("# Code Context\n\n")

        if (options.includeMetadata) {
            sb.append("**Export Summary:**\n")
            sb.append("- ${metadata.filesProcessed} files, ${TokenEstimator.formatTokenCount(metadata.estimatedTokens)}\n")
            metadata.projectName?.let { sb.append("- Project: $it\n") }
            sb.append("\n---\n\n")
        }

        entries.forEach { entry ->
            sb.append("## 📄 `${entry.relativePath}`\n\n")
            sb.append("```${entry.language}\n")
            sb.append(formatContent(entry.content, options))
            sb.append("\n```\n\n")
        }

        return sb.toString()
    }

    private fun formatGeminiOptimized(
        entries: List<FileEntry>,
        options: ExportOptions,
        metadata: ExportMetadata
    ): String {
        val sb = StringBuilder()

        sb.append("# Code Analysis Context\n\n")

        if (options.includeMetadata) {
            sb.append("## Metadata\n")
            sb.append("Total files: ${metadata.filesProcessed} | ")
            sb.append("Estimated tokens: ${TokenEstimator.formatTokenCount(metadata.estimatedTokens)}\n\n")
        }

        entries.forEachIndexed { index, entry ->
            sb.append("### File ${index + 1}: ${entry.relativePath}\n")
            sb.append("Language: ${entry.language}, Lines: ${entry.lineCount}\n\n")
            sb.append("```${entry.language}\n")
            sb.append(formatContent(entry.content, options))
            sb.append("\n```\n\n")
        }

        return sb.toString()
    }

    private fun buildMetadataHeader(metadata: ExportMetadata, options: ExportOptions): String {
        val sb = StringBuilder()
        sb.append("# ClipCraft Export\n\n")
        sb.append("## Export Statistics\n\n")
        sb.append("- **Files Processed:** ${metadata.filesProcessed}\n")
        sb.append("- **Files Skipped:** ${metadata.filesSkipped}\n")
        sb.append("- **Total Size:** ${TokenEstimator.formatByteSize(metadata.totalBytes)}\n")
        sb.append("- **Estimated Tokens:** ${TokenEstimator.formatTokenCount(metadata.estimatedTokens)}\n")
        sb.append("- **Context Window:** ${TokenEstimator.getContextWindowFit(metadata.estimatedTokens)}\n")

        if (options.includeTimestamp) {
            sb.append("- **Export Time:** ${metadata.exportTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}\n")
        }

        metadata.projectName?.let { sb.append("- **Project:** $it\n") }

        if (options.includeGitInfo) {
            metadata.gitBranch?.let { sb.append("- **Git Branch:** $it\n") }
            metadata.gitCommit?.let { sb.append("- **Git Commit:** $it\n") }
        }

        sb.append("\n---\n\n")
        return sb.toString()
    }

    private fun formatFilePath(path: Path, options: ExportOptions): String {
        return when (options.pathFormat) {
            com.clipcraft.model.PathFormat.RELATIVE -> "`$path`"
            com.clipcraft.model.PathFormat.ABSOLUTE -> "`${path.toAbsolutePath()}`"
            com.clipcraft.model.PathFormat.PROJECT_RELATIVE -> "`$path`"
            com.clipcraft.model.PathFormat.CUSTOM -> "`${options.customPathPrefix}$path`"
        }
    }

    private fun formatContent(content: String, options: ExportOptions): String {
        var formatted = content

        if (options.stripComments) {
            // Basic comment stripping (can be enhanced)
            formatted = formatted.lines().filter { line ->
                val trimmed = line.trim()
                !trimmed.startsWith("//") && !trimmed.startsWith("#") && !trimmed.startsWith("/*")
            }.joinToString("\n")
        }

        if (options.stripWhitespace) {
            formatted = formatted.lines().map { it.trim() }.joinToString("\n")
        }

        if (options.includeLineNumbers) {
            formatted = formatted.lines().mapIndexed { index, line ->
                "${(index + 1).toString().padStart(4)}: $line"
            }.joinToString("\n")
        }

        return formatted
    }

    private fun sortEntries(entries: List<FileEntry>, options: ExportOptions): List<FileEntry> {
        return when (options.sortFiles) {
            com.clipcraft.model.FileSortOrder.PATH_ALPHABETICAL -> entries.sortedBy { it.relativePath.toString() }
            com.clipcraft.model.FileSortOrder.NAME_ALPHABETICAL -> entries.sortedBy { it.file.name }
            com.clipcraft.model.FileSortOrder.SIZE_ASCENDING -> entries.sortedBy { it.byteSize }
            com.clipcraft.model.FileSortOrder.SIZE_DESCENDING -> entries.sortedByDescending { it.byteSize }
            com.clipcraft.model.FileSortOrder.MODIFIED_DATE -> entries.sortedByDescending { it.file.timeStamp }
            com.clipcraft.model.FileSortOrder.EXTENSION -> entries.sortedBy { it.file.extension ?: "" }
        }
    }

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun escapeJson(text: String): String {
        return text.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }
}
