package com.clipcraft.services

import com.clipcraft.model.OutputFormat
import com.intellij.openapi.project.Project
import org.slf4j.LoggerFactory
import java.io.File

object ClipCraftSharingService {

    private val log = LoggerFactory.getLogger(ClipCraftSharingService::class.java)

    fun shareToGist(content: String, project: Project?): Boolean {
        return try {
            log.debug("Pretending to upload to Gist. Content length: ${content.length}")
            true
        } catch (e: Exception) {
            log.error("Failed to share to Gist: ${e.message}")
            false
        }
    }

    fun exportToCloud(content: String, provider: String, project: Project?): Boolean {
        return try {
            log.debug("Pretending to export to cloud provider: $provider. Length: ${content.length}")
            true
        } catch (e: Exception) {
            log.error("Failed to export to cloud: ${e.message}")
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
