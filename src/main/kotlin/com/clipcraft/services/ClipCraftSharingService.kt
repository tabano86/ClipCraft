package com.clipcraft.services

import com.clipcraft.model.Snippet
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class ClipCraftSharingService(private val project: Project) {
    private val logger = Logger.getInstance(ClipCraftSharingService::class.java)

    fun shareSnippet(snippet: Snippet): String {
        logger.info("Sharing snippet from file: ${snippet.filePath}")
        return "https://share.clipcraft.com/snippet/${snippet.id}"
    }
}
