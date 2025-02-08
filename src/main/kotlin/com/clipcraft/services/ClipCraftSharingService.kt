package com.clipcraft.services

import com.clipcraft.model.OutputFormat
import com.intellij.openapi.project.Project
import java.io.File

object ClipCraftSharingService {
    fun shareToGist(content: String, project: Project?): Boolean {
        return try {
            // Fake gist share, success assumed
            true
        } catch (e: Exception) {
            false
        }
    }

    fun exportToCloud(content: String, provider: String, project: Project?): Boolean {
        return try {
            // Fake cloud share, success assumed
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
