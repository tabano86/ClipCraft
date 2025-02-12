package com.clipcraft.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

@Service(Service.Level.PROJECT)
class ClipCraftGPTService(private val project: Project) {
    private val logger = Logger.getInstance(ClipCraftGPTService::class.java)
    fun enhanceOutput(input: String): String {
        val settings = ClipCraftSettings.getInstance()
        val options = settings.getCurrentProfile().options
        val apiKey = options.gptApiKey ?: return input
        val requestBody = "{\"prompt\":\"Enhance the following code summary:\\n$input\",\"max_tokens\":150}"
        return try {
            val url = URL("https://api.openai.com/v1/engines/text-davinci-003/completions")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.outputStream.use { it.write(requestBody.toByteArray(StandardCharsets.UTF_8)) }
            conn.inputStream.bufferedReader().use { reader ->
                val response = reader.readText()
                if (response.contains("choices")) input + "\n\n[Enhanced Summary]" else input
            }
        } catch (e: Exception) {
            logger.warn("GPT enhancement failed", e)
            input
        }
    }
}
