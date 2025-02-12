package com.clipcraft.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project

/**
 * Simulates a lightweight AI assistant that “refines” snippet output.
 */
@Service(Service.Level.PROJECT)
class ClipCraftAIAssistantService(private val project: Project) {
    private val logger = Logger.getInstance(ClipCraftAIAssistantService::class.java)
    fun refineOutput(input: String): String {
        logger.info("Refining output via AI assistant")
        // Dummy refinement: remove duplicate blank lines and trim overall.
        return input.replace(Regex("(\\n\\s*){3,}"), "\n\n").trim()
    }
}
