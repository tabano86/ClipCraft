package com.clipcraft.services

import com.clipcraft.model.Snippet
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

/**
 * Stub for snippet-sharing or GPT integration features.
 */
@Service(Service.Level.PROJECT)
class ClipCraftSharingService(private val project: Project) {

    var gptApiKey: String? = null

    /**
     * Example method to send snippet to GPT with a chosen prompt.
     * This is a stub – replace with an actual API call in production.
     */
    fun sendToGpt(snippet: Snippet, promptTemplate: String): String {
        // Hypothetical code:
        // val response = openAiApi.sendRequest(promptTemplate + "\n" + snippet.content)
        // return response

        return """
            [GPT MOCK RESPONSE] 
            Prompt: $promptTemplate
            Code: ${snippet.content.take(100)}...
        """.trimIndent()
    }
}
