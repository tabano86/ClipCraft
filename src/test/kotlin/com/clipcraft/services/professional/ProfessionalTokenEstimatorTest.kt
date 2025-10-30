package com.clipcraft.services.professional

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("Professional Token Estimator Tests")
class ProfessionalTokenEstimatorTest {

    @Test
    @DisplayName("Should count tokens accurately for simple text")
    fun `test simple token counting`() {
        val text = "Hello, world!"
        val tokens = ProfessionalTokenEstimator.countTokens(text)

        // "Hello, world!" should be approximately 4 tokens
        assertThat(tokens).isBetween(3, 5)
    }

    @Test
    @DisplayName("Should count tokens for code accurately")
    fun `test code token counting`() {
        val code = """
            fun main() {
                println("Hello, world!")
            }
        """.trimIndent()

        val tokens = ProfessionalTokenEstimator.countTokens(code)

        // Code typically has more tokens due to symbols
        assertThat(tokens).isGreaterThan(10)
    }

    @ParameterizedTest
    @CsvSource(
        "'Hello', 1, 3",
        "'Hello world', 2, 4",
        "'The quick brown fox', 4, 6",
    )
    @DisplayName("Should count tokens within expected range")
    fun `test token range for common phrases`(text: String, minTokens: Int, maxTokens: Int) {
        val tokens = ProfessionalTokenEstimator.countTokens(text)
        assertThat(tokens).isBetween(minTokens, maxTokens)
    }

    @Test
    @DisplayName("Should identify GPT-4 as compatible model for small text")
    fun `test model compatibility for small text`() {
        val smallText = "A".repeat(1000) // ~250 tokens
        val compatible = ProfessionalTokenEstimator.getCompatibleModels(smallText)

        assertThat(compatible).contains(
            ProfessionalTokenEstimator.AIModel.GPT_4,
            ProfessionalTokenEstimator.AIModel.GPT_4_32K,
            ProfessionalTokenEstimator.AIModel.CLAUDE_3_OPUS,
        )
    }

    @Test
    @DisplayName("Should identify only large context models for big text")
    fun `test model compatibility for large text`() {
        val largeText = "A".repeat(50000) // ~12,500 tokens
        val compatible = ProfessionalTokenEstimator.getCompatibleModels(largeText)

        assertThat(compatible).containsAnyOf(
            ProfessionalTokenEstimator.AIModel.GPT_4_32K,
            ProfessionalTokenEstimator.AIModel.CLAUDE_3_OPUS,
        )

        assertThat(compatible).doesNotContain(
            ProfessionalTokenEstimator.AIModel.GPT_4,
        )
    }

    @Test
    @DisplayName("Should recommend smallest compatible model")
    fun `test model recommendation`() {
        val text = "A".repeat(2000) // ~500 tokens
        val recommended = ProfessionalTokenEstimator.recommendModel(text)

        assertThat(recommended).isEqualTo(ProfessionalTokenEstimator.AIModel.GPT_4)
    }

    @Test
    @DisplayName("Should format token counts correctly")
    fun `test token count formatting`() {
        assertThat(ProfessionalTokenEstimator.formatTokenCount(500)).isEqualTo("500 tokens")
        assertThat(ProfessionalTokenEstimator.formatTokenCount(1500)).contains("K tokens")
        assertThat(ProfessionalTokenEstimator.formatTokenCount(1500000)).contains("M tokens")
    }

    @Test
    @DisplayName("Should check context window fit correctly")
    fun `test context window fit`() {
        val smallText = "Hello world"
        val fitsGPT4 = ProfessionalTokenEstimator.fitsInContextWindow(
            smallText,
            ProfessionalTokenEstimator.AIModel.GPT_4,
        )

        assertThat(fitsGPT4).isTrue()
    }

    @Test
    @DisplayName("Should generate context window report")
    fun `test context window report generation`() {
        val text = "A".repeat(1000)
        val report = ProfessionalTokenEstimator.getContextWindowReport(text)

        assertThat(report).contains("Token Count")
        assertThat(report).contains("Compatible Models")
        assertThat(report).contains("GPT")
    }

    @Test
    @DisplayName("Should handle empty string")
    fun `test empty string handling`() {
        val tokens = ProfessionalTokenEstimator.countTokens("")
        assertThat(tokens).isEqualTo(0)
    }

    @Test
    @DisplayName("Should handle very long text")
    fun `test very long text handling`() {
        val longText = "A".repeat(1000000) // ~250K tokens
        val tokens = ProfessionalTokenEstimator.countTokens(longText)

        assertThat(tokens).isGreaterThan(200000)
        assertThat(tokens).isLessThan(300000)
    }

    @Test
    @DisplayName("Should count tokens consistently")
    fun `test token counting consistency`() {
        val text = "Consistent token counting test"

        val count1 = ProfessionalTokenEstimator.countTokens(text)
        val count2 = ProfessionalTokenEstimator.countTokens(text)
        val count3 = ProfessionalTokenEstimator.countTokens(text)

        assertThat(count1).isEqualTo(count2)
        assertThat(count2).isEqualTo(count3)
    }
}
