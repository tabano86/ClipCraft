// File: ClipCraftMasteryAction.kt
package com.clipcraft.actions

import com.clipcraft.model.Snippet
import com.clipcraft.services.ClipCraftSettingsService
import com.clipcraft.services.SnippetsManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.ui.Messages
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Data classes for the OpenAI Chat Completion API
 */
@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.7
)

@Serializable
data class ChatCompletionChoice(
    val index: Int,
    val message: ChatMessage,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class ChatCompletionResponse(
    val id: String,
    @SerialName("object") val objectType: String,
    val created: Long,
    val choices: List<ChatCompletionChoice>
)

/**
 * ClipCraftMasteryAction:
 *
 *  1. Gathers code snippets from the queue, merges them, computes metrics.
 *  2. If no GPT API key => "free" local mode with simple text.
 *  3. If GPT API key => calls GPT with a multi-phase request:
 *      - Summarize the code
 *      - Generate 2 competitor solutions (Method A and Method B)
 *      - Merge them into a final "master" solution
 *      - Provide a usage tutorial + test cases
 *  4. Displays the final, consolidated output to the user.
 *  5. Model is user-configurable; if absent defaults to "gpt-3.5-turbo".
 */
class ClipCraftMasteryAction : AnAction("ClipCraft: Code Mastery") {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val snippetsManager = project.getService(SnippetsManager::class.java) ?: return
        val allSnippets: List<Snippet> = snippetsManager.getAllSnippets()
        if (allSnippets.isEmpty()) {
            Messages.showInfoMessage(project, "No snippets in the queue.", "Code Mastery")
            return
        }

        // Combine code snippets and compute basic stats
        val mergedCode = allSnippets.joinToString("\n\n") { it.content }
        val lineCount = mergedCode.lines().size
        val charCount = mergedCode.length

        // Retrieve key & model from persistent settings
        val settingsService = ClipCraftSettingsService.getInstance()
        val apiKey = settingsService.state.gptApiKey
        // For demonstration, default to "gpt-3.5-turbo" if no model field is present.
        val chosenModel = "gpt-3.5-turbo"

        // If no API key is provided, use the free local mode.
        if (apiKey.isNullOrBlank()) {
            val localOutput = buildString {
                append("=== CODE MASTERY (Local Mode) ===\n\n")
                append("We have $lineCount lines ($charCount chars). Without a GPT API key,\n")
                append("we can only do a basic local summary:\n\n")
                append("1) Summarize: The code includes multiple snippets; manual analysis is recommended.\n")
                append("2) Competitor Solutions: In local mode, we do not generate them.\n")
                append("3) Master Merged Solution: Not available in local mode.\n")
                append("4) Usage & Test Ideas: Consider adding tests for edge cases and concurrency.\n\n")
                append("Provide an API key in your settings to unlock full GPT features!")
            }
            Messages.showInfoMessage(project, localOutput, "Code Mastery (Local Mode)")
            return
        }

        // Build a multi-phase GPT prompt.
        val prompt = """
            You are a next-generation Code Master. We have $lineCount lines and $charCount characters of code:
            
            $mergedCode

            We want a radical, multi-step transformation and analysis:
            
            1) Summarize the existing code.
            2) Generate two distinct "competitor solutions" (Method A, Method B) that achieve the same outcome but take different approaches (e.g. different patterns).
            3) Merge them into a single "Master" solution that combines their strengths.
            4) Provide a usage tutorial with a short sample code snippet demonstrating how to integrate or run this Master solution in a real-world scenario.
            5) Suggest at least 3 test cases focusing on edge conditions or unusual scenarios.
            
            Output each step in clearly separated sections labeled:
             - [SUMMARY]
             - [COMPETITOR A]
             - [COMPETITOR B]
             - [MASTER SOLUTION]
             - [USAGE TUTORIAL]
             - [TEST CASES]
            
            Make your suggestions surprising, imaginative, and domain-focused.
        """.trimIndent()

        // Dispatch the GPT call inside a modal progress.
        ProgressManager.getInstance().run(object : com.intellij.openapi.progress.Task.Modal(project, "Enacting Code Mastery...", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                try {
                    // Execute the GPT call on an IO dispatcher.
                    val gptResponse = runBlocking(Dispatchers.IO) {
                        callOpenAIGPT(apiKey, chosenModel, prompt, temperature = 0.8)
                    }
                    // Update UI on the EDT.
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showInfoMessage(project, gptResponse, "Code Mastery: GPT Output")
                    }
                } catch (ex: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(project, "Error calling GPT: ${ex.message}", "Code Mastery")
                    }
                }
            }
        })
    }

    /**
     * Calls the Chat Completions API, using Ktor 2.x plugin APIs properly.
     */
    private suspend fun callOpenAIGPT(apiKey: String, model: String, prompt: String, temperature: Double): String {
        val client = HttpClient {
            // ContentNegotiation with kotlinx-serialization
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = false
                    isLenient = true
                })
            }
            // Logging
            install(Logging) {
                level = LogLevel.INFO
            }
            // Timeout
            install(HttpTimeout) {
                requestTimeoutMillis = 30000
            }
        }

        val requestBody = ChatCompletionRequest(
            model = model,
            messages = listOf(ChatMessage(role = "user", content = prompt)),
            temperature = temperature
        )

        val response: ChatCompletionResponse = client.post("https://api.openai.com/v1/chat/completions") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }.body()

        client.close()
        return response.choices.firstOrNull()?.message?.content?.trim() ?: "No response from GPT API."
    }
}
