package com.clipcraft.model

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Represents a single export in the history
 */
@Serializable
data class ExportHistoryEntry(
    val id: String,
    val timestamp: Long,
    val exportType: String, // "file", "project", "preset", "custom"
    val format: String, // "markdown", "xml", "json", etc.
    val filesExported: Int,
    val totalSize: Long,
    val estimatedTokens: Int,
    val presetName: String? = null,
    val options: ExportOptions,
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
            options: ExportOptions,
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
                options = options,
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
}
