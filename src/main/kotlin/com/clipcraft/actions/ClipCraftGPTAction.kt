// File: ClipCraftUltraCleverGPTAction.kt
package com.clipcraft.actions

import com.clipcraft.model.Snippet
import com.clipcraft.services.SnippetsManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.ui.Messages
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * A cache for GPT responses keyed by a composite prompt.
 */
object GPTCache {
    private val cache = ConcurrentHashMap<String, String>()
    fun get(key: String): String? = cache[key]
    fun put(key: String, response: String) {
        cache[key] = response
    }
}

class ClipCraftGPTAction : AnAction("ClipCraft: GPT") {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val snippetsManager = project.getService(SnippetsManager::class.java) ?: return
        val snippets: List<Snippet> = snippetsManager.getAllSnippets()
        if (snippets.isEmpty()) {
            Messages.showInfoMessage(project, "No snippets available.", "ClipCraft Ultra Clever GPT")
            return
        }
        // Combine all snippets.
        val combinedCode = snippets.joinToString("\n\n") { it.content }
        // Pre-analyze the code.
        val lineCount = combinedCode.lines().size
        val charCount = combinedCode.length

        // Build a composite prompt that includes the metrics.
        val promptAnalysis = """
            The code consists of $lineCount lines and $charCount characters.
            Please provide a detailed analysis of its strengths, weaknesses,
            and potential performance pitfalls.
        """.trimIndent()

        val promptRefactor = """
            Based on the above analysis, please suggest concrete refactoring strategies
            to improve maintainability and performance.
        """.trimIndent()

        // Use a composite key for caching.
        val cacheKey = "ultra:$lineCount:$charCount"

        // Check if we already have a cached response.
        GPTCache.get(cacheKey)?.let { cachedResponse ->
            Messages.showInfoMessage(project, "Cached Response:\n\n$cachedResponse", "ClipCraft Ultra Clever GPT")
            return
        }

        // Run a modal progress task with multiple phases.
        ProgressManager.getInstance().run(object : com.intellij.openapi.progress.Task.Modal(project, "Processing GPT Request", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = false

                runBlocking(Dispatchers.IO) {
                    // Phase 1: Pre-analysis phase (simulate brief processing)
                    indicator.text = "Pre-analyzing code..."
                    delay(300)  // Simulate brief pre-analysis delay
                    indicator.fraction = 0.1

                    // Phase 2: Analysis streaming phase (from 0.1 to 0.55)
                    indicator.text = "Generating code analysis..."
                    val analysisResponse = simulateStreamingResponse(
                        promptAnalysis,
                        startFraction = 0.1,
                        endFraction = 0.55,
                        indicator = indicator
                    )

                    // Phase 3: Refactoring suggestions streaming phase (from 0.55 to 1.0)
                    indicator.text = "Generating refactoring suggestions..."
                    val refactorResponse = simulateStreamingResponse(
                        promptRefactor,
                        startFraction = 0.55,
                        endFraction = 1.0,
                        indicator = indicator
                    )

                    val fullResponse = buildString {
                        append("=== Code Analysis ===\n")
                        append(analysisResponse)
                        append("\n\n=== Refactoring Suggestions ===\n")
                        append(refactorResponse)
                    }
                    // Cache the full response.
                    GPTCache.put(cacheKey, fullResponse)
                    withContext(Dispatchers.Main) {
                        Messages.showInfoMessage(project, fullResponse, "ClipCraft Ultra Clever GPT")
                    }
                }
            }
        })
    }

    /**
     * Simulates a streaming GPT response.
     *
     * Splits the full simulated response into chunks and calls onChunk for each chunk.
     * The progress indicator fraction is updated based on the current phase progress.
     */
    private suspend fun simulateStreamingResponse(
        prompt: String,
        startFraction: Double,
        endFraction: Double,
        indicator: ProgressIndicator,
        onChunk: (String) -> Unit = {}
    ): String {
        // For demonstration, the simulated response is generated by repeating the prompt with embellishments.
        val simulatedResponse = """
            ${prompt.replace("\n", " ")} 
            ... [Simulated GPT analysis response content generated with deep insights and suggestions] ...
        """.trimIndent()
        val totalLength = simulatedResponse.length
        val chunkSize = 40  // Use a modest chunk size
        var currentIndex = 0
        val responseBuilder = StringBuilder()
        while (currentIndex < totalLength) {
            if (indicator.isCanceled) break
            val endIndex = minOf(currentIndex + chunkSize, totalLength)
            val chunk = simulatedResponse.substring(currentIndex, endIndex)
            responseBuilder.append(chunk)
            onChunk(chunk)
            currentIndex = endIndex
            // Update the indicator fraction based on phase progress.
            val progressInPhase = currentIndex.toDouble() / totalLength
            indicator.fraction = startFraction + progressInPhase * (endFraction - startFraction)
            delay(250) // Simulate delay per chunk
        }
        return responseBuilder.toString()
    }
}
