package com.clipcraft.model

import java.time.Instant

/**
 * Represents a single export in the history
 */
data class ExportHistoryEntry(
    val id: String,
    val timestamp: Long,
    val exportType: String, // "file", "project", "preset", "custom"
    val format: String, // "markdown", "xml", "json", etc.
    val filesExported: Int,
    val totalSize: Long,
    val estimatedTokens: Int,
    val presetName: String? = null,
    val exportedPaths: List<String>, // Relative paths of exported files
    val preview: String, // First 200 chars of export
    val success: Boolean = true,
    val errorMessage: String? = null
) {
    companion object {
        fun create(
            exportType: String,
            format: String,
            filesExported: Int,
            totalSize: Long,
            estimatedTokens: Int,
            presetName: String? = null,
            exportedPaths: List<String>,
            preview: String
        ): ExportHistoryEntry {
            return ExportHistoryEntry(
                id = java.util.UUID.randomUUID().toString(),
                timestamp = Instant.now().toEpochMilli(),
                exportType = exportType,
                format = format,
                filesExported = filesExported,
                totalSize = totalSize,
                estimatedTokens = estimatedTokens,
                presetName = presetName,
                exportedPaths = exportedPaths,
                preview = preview.take(200)
            )
        }
    }

    fun getFormattedTimestamp(): String {
        val instant = Instant.ofEpochMilli(timestamp)
        return instant.toString()
    }

    fun getFormattedSize(): String {
        return when {
            totalSize < 1024 -> "$totalSize B"
            totalSize < 1024 * 1024 -> "${totalSize / 1024} KB"
            else -> "${totalSize / (1024 * 1024)} MB"
        }
    }

    // Convert to XML format for storage
    fun toXml(): String {
        return """
            <entry>
                <id>$id</id>
                <timestamp>$timestamp</timestamp>
                <exportType>$exportType</exportType>
                <format>$format</format>
                <filesExported>$filesExported</filesExported>
                <totalSize>$totalSize</totalSize>
                <estimatedTokens>$estimatedTokens</estimatedTokens>
                <presetName>${presetName ?: ""}</presetName>
                <exportedPaths>${exportedPaths.joinToString(",")}</exportedPaths>
                <preview><![CDATA[${preview}]]></preview>
                <success>$success</success>
                <errorMessage>${errorMessage ?: ""}</errorMessage>
            </entry>
        """.trimIndent()
    }
}
