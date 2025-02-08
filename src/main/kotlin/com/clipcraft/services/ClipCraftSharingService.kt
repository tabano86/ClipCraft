package com.clipcraft.services

import com.clipcraft.model.OutputFormat
import com.intellij.openapi.project.Project
import java.io.File

/**
 * Handles optional sharing or exporting to external services.
 */
object ClipCraftSharingService {

    fun shareToGist(content: String, project: Project?): Boolean {
        // Stub: Implement actual networking here
        return try {
            true
        } catch (e: Exception) {
            false
        }
    }

    fun exportToCloud(content: String, provider: String, project: Project?): Boolean {
        // Stub: Implement actual cloud integration here
        return try {
            true
        } catch (e: Exception) {
            false
        }
    }

    fun exportMultipleFormats(baseFilePath: String, formats: Set<OutputFormat>, combined: String): List<String> {
        val saved = mutableListOf<String>()
        for (fmt in formats) {
            val ext = when (fmt) {
                OutputFormat.MARKDOWN -> "md"
                OutputFormat.HTML -> "html"
                else -> "txt"
            }
            val path = "$baseFilePath.$ext"
            File(path).writeText(combined)
            saved += path
        }
        return saved
    }
}
