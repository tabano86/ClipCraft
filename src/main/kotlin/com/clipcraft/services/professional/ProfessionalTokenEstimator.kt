package com.clipcraft.services.professional

import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.Encoding
import com.knuddels.jtokkit.api.EncodingRegistry
import com.knuddels.jtokkit.api.EncodingType

/**
 * Professional token estimation using tiktoken (OpenAI's actual tokenizer).
 * No more guessing - this is the real deal.
 */
object ProfessionalTokenEstimator {

    private val registry: EncodingRegistry = Encodings.newDefaultEncodingRegistry()

    // Pre-load encodings for performance
    private val cl100kEncoding: Encoding = registry.getEncoding(EncodingType.CL100K_BASE)
    private val gpt2Encoding: Encoding = registry.getEncoding(EncodingType.R50K_BASE)

    enum class AIModel(val modelName: String, val maxTokens: Int, val encodingType: EncodingType) {
        GPT_4("gpt-4", 8192, EncodingType.CL100K_BASE),
        GPT_4_32K("gpt-4-32k", 32768, EncodingType.CL100K_BASE),
        GPT_4_TURBO("gpt-4-turbo", 128000, EncodingType.CL100K_BASE),
        GPT_4O("gpt-4o", 128000, EncodingType.CL100K_BASE),
        GPT_3_5_TURBO("gpt-3.5-turbo", 16384, EncodingType.CL100K_BASE),
        CLAUDE_3_OPUS("claude-3-opus", 200000, EncodingType.CL100K_BASE),
        CLAUDE_3_SONNET("claude-3-sonnet", 200000, EncodingType.CL100K_BASE),
        CLAUDE_3_HAIKU("claude-3-haiku", 200000, EncodingType.CL100K_BASE),
        GEMINI_PRO("gemini-pro", 32760, EncodingType.CL100K_BASE),
        GEMINI_ULTRA("gemini-ultra", 32760, EncodingType.CL100K_BASE)
    }

    /**
     * Get accurate token count using tiktoken.
     */
    fun countTokens(text: String, encodingType: EncodingType = EncodingType.CL100K_BASE): Int {
        val encoding = registry.getEncoding(encodingType)
        return encoding.countTokens(text)
    }

    /**
     * Count tokens for a specific AI model.
     */
    fun countTokensForModel(text: String, model: AIModel): Int {
        return countTokens(text, model.encodingType)
    }

    /**
     * Check if text fits in model's context window.
     */
    fun fitsInContextWindow(text: String, model: AIModel): Boolean {
        val tokens = countTokensForModel(text, model)
        return tokens <= model.maxTokens
    }

    /**
     * Find all models that can handle this text.
     */
    fun getCompatibleModels(text: String): List<AIModel> {
        val tokens = countTokens(text)
        return AIModel.values().filter { it.maxTokens >= tokens }
    }

    /**
     * Get the best model recommendation for this text.
     */
    fun recommendModel(text: String): AIModel? {
        val compatibleModels = getCompatibleModels(text)
        return compatibleModels.minByOrNull { it.maxTokens }
    }

    /**
     * Format token count for display.
     */
    fun formatTokenCount(tokens: Int): String {
        return when {
            tokens < 1_000 -> "$tokens tokens"
            tokens < 1_000_000 -> String.format("%.1fK tokens", tokens / 1000.0)
            else -> String.format("%.1fM tokens", tokens / 1_000_000.0)
        }
    }

    /**
     * Get detailed context window report.
     */
    fun getContextWindowReport(text: String): String {
        val tokens = countTokens(text)
        val compatible = getCompatibleModels(text)

        return buildString {
            appendLine("Token Count: ${formatTokenCount(tokens)}")
            appendLine("Compatible Models:")
            compatible.forEach { model ->
                val percentage = (tokens.toDouble() / model.maxTokens * 100).toInt()
                appendLine("  - ${model.name}: $percentage% of context window")
            }
        }
    }

    /**
     * Estimate cost for OpenAI models (approximate pricing).
     */
    fun estimateCost(tokens: Int, model: AIModel): Double {
        return when (model) {
            AIModel.GPT_4 -> tokens / 1000.0 * 0.03 // $0.03/1K tokens
            AIModel.GPT_4_32K -> tokens / 1000.0 * 0.06
            AIModel.GPT_4_TURBO -> tokens / 1000.0 * 0.01
            AIModel.GPT_4O -> tokens / 1000.0 * 0.005
            AIModel.GPT_3_5_TURBO -> tokens / 1000.0 * 0.0015
            else -> 0.0 // Claude/Gemini have different pricing
        }
    }
}
