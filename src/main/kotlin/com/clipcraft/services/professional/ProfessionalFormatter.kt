package com.clipcraft.services.professional

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.google.gson.GsonBuilder
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Professional formatters using industry-standard libraries.
 * Jackson for JSON/XML, CommonMark for Markdown, JSoup for HTML.
 */
object ProfessionalFormatter {

    // Pre-configured Jackson mappers
    private val jsonMapper = jacksonObjectMapper().apply {
        registerKotlinModule()
        enable(SerializationFeature.INDENT_OUTPUT)
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    private val xmlMapper = XmlMapper().apply {
        registerKotlinModule()
        enable(SerializationFeature.INDENT_OUTPUT)
    }

    private val yamlMapper = YAMLMapper().apply {
        registerKotlinModule()
    }

    // Pre-configured Gson for fast simple JSON
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .create()

    // CommonMark parser and renderer
    private val markdownParser = Parser.builder().build()
    private val htmlRenderer = HtmlRenderer.builder().build()

    data class ExportData(
        val metadata: MetadataInfo,
        val files: List<FileInfo>
    )

    data class MetadataInfo(
        val exportTime: String,
        val projectName: String?,
        val totalFiles: Int,
        val totalBytes: Long,
        val estimatedTokens: Int,
        val gitBranch: String?,
        val gitCommit: String?
    )

    data class FileInfo(
        val path: String,
        val language: String,
        val content: String,
        val lineCount: Int,
        val byteSize: Long,
        val tokens: Int?
    )

    data class FormattedOutput(
        val content: String,
        val metadata: FormattedMetadata
    )

    data class FormattedMetadata(
        val filesProcessed: Int,
        val filesSkipped: Int,
        val totalBytes: Long,
        val estimatedTokens: Int
    )

    /**
     * Export to JSON using Jackson (industry standard).
     */
    fun toJson(data: ExportData): String {
        return jsonMapper.writeValueAsString(data)
    }

    /**
     * Export to JSON using Gson (faster for simple cases).
     */
    fun toJsonFast(data: ExportData): String {
        return gson.toJson(data)
    }

    /**
     * Export to XML using Jackson XML.
     */
    fun toXml(data: ExportData): String {
        return xmlMapper.writeValueAsString(data)
    }

    /**
     * Export to YAML using Jackson YAML.
     */
    fun toYaml(data: ExportData): String {
        return yamlMapper.writeValueAsString(data)
    }

    /**
     * Export to Markdown (GitHub-flavored).
     */
    fun toMarkdown(data: ExportData, includeMetadata: Boolean = true, includeToc: Boolean = false): String {
        return buildString {
            if (includeMetadata) {
                appendLine("# ${data.metadata.projectName ?: "Code Export"}")
                appendLine()
                appendLine("## Export Information")
                appendLine()
                appendLine("- **Export Time:** ${data.metadata.exportTime}")
                appendLine("- **Total Files:** ${data.metadata.totalFiles}")
                appendLine("- **Total Size:** ${formatBytes(data.metadata.totalBytes)}")
                appendLine("- **Estimated Tokens:** ${ProfessionalTokenEstimator.formatTokenCount(data.metadata.estimatedTokens)}")
                data.metadata.gitBranch?.let { appendLine("- **Git Branch:** $it") }
                data.metadata.gitCommit?.let { appendLine("- **Git Commit:** $it") }
                appendLine()
                appendLine("---")
                appendLine()
            }

            if (includeToc) {
                appendLine("## Table of Contents")
                appendLine()
                data.files.forEachIndexed { index, file ->
                    val anchor = file.path.replace("[^a-zA-Z0-9-]".toRegex(), "-").lowercase()
                    appendLine("${index + 1}. [${file.path}](#$anchor)")
                }
                appendLine()
                appendLine("---")
                appendLine()
            }

            data.files.forEach { file ->
                appendLine("## `${file.path}`")
                appendLine()
                appendLine("*Language: ${file.language} | Lines: ${file.lineCount} | Size: ${formatBytes(file.byteSize)}*")
                appendLine()
                appendLine("```${file.language}")
                appendLine(file.content)
                appendLine("```")
                appendLine()
            }
        }
    }

    /**
     * Export to HTML using JSoup for professional structure.
     */
    fun toHtml(data: ExportData, includeMetadata: Boolean = true): String {
        val doc: Document = Jsoup.parse("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>${data.metadata.projectName ?: "Code Export"}</title>
                <style>
                    :root {
                        --primary-color: #0066cc;
                        --bg-gradient-start: #667eea;
                        --bg-gradient-end: #764ba2;
                        --text-color: #333;
                        --code-bg: #f5f5f5;
                        --border-color: #ddd;
                    }

                    @media (prefers-color-scheme: dark) {
                        :root {
                            --text-color: #e0e0e0;
                            --code-bg: #1e1e1e;
                            --border-color: #444;
                        }
                    }

                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                        line-height: 1.6;
                        color: var(--text-color);
                        max-width: 1200px;
                        margin: 0 auto;
                        padding: 20px;
                        background: linear-gradient(135deg, var(--bg-gradient-start), var(--bg-gradient-end));
                    }

                    .container {
                        background: white;
                        border-radius: 12px;
                        box-shadow: 0 10px 40px rgba(0,0,0,0.1);
                        padding: 40px;
                    }

                    @media (prefers-color-scheme: dark) {
                        .container {
                            background: #2d2d2d;
                        }
                    }

                    h1 {
                        background: linear-gradient(135deg, var(--bg-gradient-start), var(--bg-gradient-end));
                        -webkit-background-clip: text;
                        -webkit-text-fill-color: transparent;
                        background-clip: text;
                        font-size: 2.5em;
                        margin-bottom: 10px;
                    }

                    .metadata {
                        background: var(--code-bg);
                        border-left: 4px solid var(--primary-color);
                        padding: 20px;
                        margin: 20px 0;
                        border-radius: 8px;
                    }

                    .metadata-item {
                        display: flex;
                        margin: 10px 0;
                    }

                    .metadata-label {
                        font-weight: bold;
                        min-width: 150px;
                        color: var(--primary-color);
                    }

                    .file-entry {
                        margin: 30px 0;
                        border: 1px solid var(--border-color);
                        border-radius: 8px;
                        overflow: hidden;
                        transition: transform 0.2s, box-shadow 0.2s;
                    }

                    .file-entry:hover {
                        transform: translateY(-2px);
                        box-shadow: 0 4px 12px rgba(0,0,0,0.1);
                    }

                    .file-header {
                        background: linear-gradient(135deg, var(--bg-gradient-start), var(--bg-gradient-end));
                        color: white;
                        padding: 15px 20px;
                        font-weight: bold;
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                    }

                    .file-info {
                        font-size: 0.9em;
                        opacity: 0.9;
                    }

                    .file-content {
                        background: var(--code-bg);
                        padding: 20px;
                        overflow-x: auto;
                    }

                    pre {
                        margin: 0;
                        font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
                        font-size: 14px;
                        line-height: 1.5;
                    }

                    code {
                        font-family: inherit;
                    }

                    .copy-btn {
                        background: rgba(255, 255, 255, 0.2);
                        border: 1px solid rgba(255, 255, 255, 0.3);
                        color: white;
                        padding: 8px 16px;
                        border-radius: 6px;
                        cursor: pointer;
                        transition: all 0.2s;
                        font-size: 0.9em;
                    }

                    .copy-btn:hover {
                        background: rgba(255, 255, 255, 0.3);
                        transform: scale(1.05);
                    }

                    .copy-btn:active {
                        transform: scale(0.95);
                    }

                    @media (max-width: 768px) {
                        body {
                            padding: 10px;
                        }

                        .container {
                            padding: 20px;
                        }

                        h1 {
                            font-size: 1.8em;
                        }
                    }
                </style>
                <script>
                    function copyToClipboard(elementId) {
                        const element = document.getElementById(elementId);
                        const text = element.innerText;
                        navigator.clipboard.writeText(text).then(() => {
                            const btn = event.target;
                            const originalText = btn.textContent;
                            btn.textContent = '✓ Copied!';
                            setTimeout(() => {
                                btn.textContent = originalText;
                            }, 2000);
                        });
                    }
                </script>
            </head>
            <body>
                <div class="container">
                    <h1>${data.metadata.projectName ?: "Code Export"}</h1>
                </div>
            </body>
            </html>
        """.trimIndent())

        val container = doc.select(".container").first()!!

        if (includeMetadata) {
            val metadataDiv = doc.createElement("div").addClass("metadata")
            metadataDiv.appendElement("h2").text("Export Information")

            val metadataItems = listOf(
                "Export Time" to data.metadata.exportTime,
                "Total Files" to data.metadata.totalFiles.toString(),
                "Total Size" to formatBytes(data.metadata.totalBytes),
                "Estimated Tokens" to ProfessionalTokenEstimator.formatTokenCount(data.metadata.estimatedTokens),
                "Git Branch" to data.metadata.gitBranch,
                "Git Commit" to data.metadata.gitCommit
            ).filter { it.second != null }

            metadataItems.forEach { (label, value) ->
                val itemDiv = metadataDiv.appendElement("div").addClass("metadata-item")
                itemDiv.appendElement("span").addClass("metadata-label").text("$label:")
                itemDiv.appendElement("span").text(value ?: "")
            }

            container.appendChild(metadataDiv)
        }

        data.files.forEachIndexed { index, file ->
            val fileDiv = doc.createElement("div").addClass("file-entry")

            val headerDiv = fileDiv.appendElement("div").addClass("file-header")
            headerDiv.appendElement("span").text(file.path)
            val infoSpan = headerDiv.appendElement("span").addClass("file-info")
            infoSpan.text("${file.language} | ${file.lineCount} lines | ${formatBytes(file.byteSize)}")

            headerDiv.appendElement("button")
                .addClass("copy-btn")
                .attr("onclick", "copyToClipboard('code-$index')")
                .text("📋 Copy")

            val contentDiv = fileDiv.appendElement("div").addClass("file-content")
            val pre = contentDiv.appendElement("pre")
            pre.appendElement("code")
                .attr("id", "code-$index")
                .addClass("language-${file.language}")
                .text(file.content)

            container.appendChild(fileDiv)
        }

        return doc.outerHtml()
    }

    /**
     * Export to plain text (simple, universal).
     */
    fun toPlainText(data: ExportData, includeMetadata: Boolean = true): String {
        return buildString {
            if (includeMetadata) {
                appendLine("=" .repeat(80))
                appendLine("CODE EXPORT - ${data.metadata.projectName ?: "Untitled"}")
                appendLine("=".repeat(80))
                appendLine()
                appendLine("Export Time: ${data.metadata.exportTime}")
                appendLine("Total Files: ${data.metadata.totalFiles}")
                appendLine("Total Size: ${formatBytes(data.metadata.totalBytes)}")
                appendLine("Estimated Tokens: ${ProfessionalTokenEstimator.formatTokenCount(data.metadata.estimatedTokens)}")
                data.metadata.gitBranch?.let { appendLine("Git Branch: $it") }
                data.metadata.gitCommit?.let { appendLine("Git Commit: $it") }
                appendLine()
                appendLine("=".repeat(80))
                appendLine()
            }

            data.files.forEach { file ->
                appendLine("-".repeat(80))
                appendLine("FILE: ${file.path}")
                appendLine("Language: ${file.language} | Lines: ${file.lineCount} | Size: ${formatBytes(file.byteSize)}")
                appendLine("-".repeat(80))
                appendLine()
                appendLine(file.content)
                appendLine()
                appendLine()
            }
        }
    }

    /**
     * Format bytes to human-readable format.
     */
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
            else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }

    /**
     * Get current timestamp formatted.
     */
    fun getCurrentTimestamp(): String {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }
}
