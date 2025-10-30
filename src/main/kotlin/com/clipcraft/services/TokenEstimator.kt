package com.clipcraft.services

object TokenEstimator {
    /**
     * Estimates token count using a simple heuristic:
     * - Average 4 characters per token for code
     * - Accounts for whitespace and special characters
     */
    fun estimateTokens(text: String): Int {
        val charCount = text.length
        val wordCount = text.split(Regex("\\s+")).size
        val lineCount = text.lines().size

        // Weighted formula based on OpenAI's tokenization patterns
        val baseEstimate = charCount / 4.0
        val wordBonus = wordCount * 0.3
        val lineBonus = lineCount * 0.2

        return (baseEstimate + wordBonus + lineBonus).toInt()
    }

    fun estimateTokensForFiles(files: List<String>): Map<String, Int> {
        return files.associateWith { content -> estimateTokens(content) }
    }

    fun formatTokenCount(tokens: Int): String {
        return when {
            tokens < 1000 -> "$tokens tokens"
            tokens < 1_000_000 -> "${tokens / 1000}K tokens"
            else -> "${tokens / 1_000_000}M tokens"
        }
    }

    fun formatByteSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }

    /**
     * Checks if content fits within common LLM context windows
     */
    fun getContextWindowFit(tokens: Int): String {
        return when {
            tokens <= 8000 -> "Fits: GPT-3.5 (8K)"
            tokens <= 16000 -> "Fits: GPT-3.5-16K"
            tokens <= 32000 -> "Fits: GPT-4 (32K)"
            tokens <= 100000 -> "Fits: Claude 2/3 (100K)"
            tokens <= 200000 -> "Fits: Claude 3 Opus (200K)"
            else -> "Exceeds: Most context windows"
        }
    }
}
