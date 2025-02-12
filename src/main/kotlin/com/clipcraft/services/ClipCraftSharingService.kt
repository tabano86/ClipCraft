package com.clipcraft.services

import com.clipcraft.model.Snippet
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project

/**
 * Service for sharing snippets externally.
 * This stub implementation logs the snippet and returns a dummy URL.
 */
@Service(Service.Level.PROJECT)
class ClipCraftSharingService(private val project: Project) {
    private val logger = Logger.getInstance(ClipCraftSharingService::class.java)

    /**
     * Share the provided snippet and return a shareable URL.
     */
    fun shareSnippet(snippet: Snippet): String {
        logger.info("Sharing snippet from file: ${snippet.filePath}")
        // Replace this with real API logic if needed.
        return "https://share.clipcraft.com/snippet/${snippet.id}"
    }
}
