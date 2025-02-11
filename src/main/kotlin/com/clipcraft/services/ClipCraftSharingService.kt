package com.clipcraft.services

import com.clipcraft.model.Snippet
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class ClipCraftSharingService(private val project: Project) {

    var gptApiKey: String? = null

    fun sendToGpt(snippet: Snippet, promptTemplate: String): String {
        // Hypothetical GPT integration:
        // val response = openAiApi.sendRequest(promptTemplate + "\n" + snippet.content)
        // return response

        return """
            [GPT MOCK RESPONSE]
            Prompt: $promptTemplate
            Code (first 100 chars): ${snippet.content.take(100)}...
        """.trimIndent()
    }
}
