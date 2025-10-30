package com.clipcraft.services.professional

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

@DisplayName("Professional Formatter Tests")
class ProfessionalFormatterTest {

    private lateinit var sampleData: ProfessionalFormatter.ExportData

    @BeforeEach
    fun setup() {
        sampleData = ProfessionalFormatter.ExportData(
            metadata = ProfessionalFormatter.MetadataInfo(
                exportTime = "2025-01-15T10:30:00",
                projectName = "TestProject",
                totalFiles = 2,
                totalBytes = 1024L,
                estimatedTokens = 500,
                gitBranch = "main",
                gitCommit = "abc123"
            ),
            files = listOf(
                ProfessionalFormatter.FileInfo(
                    path = "src/Main.kt",
                    language = "kotlin",
                    content = "fun main() { println(\"Hello\") }",
                    lineCount = 1,
                    byteSize = 34L,
                    tokens = 10
                ),
                ProfessionalFormatter.FileInfo(
                    path = "src/Utils.kt",
                    language = "kotlin",
                    content = "fun util() { }",
                    lineCount = 1,
                    byteSize = 14L,
                    tokens = 5
                )
            )
        )
    }

    @Test
    @DisplayName("Should format to JSON correctly")
    fun `test JSON formatting`() {
        val json = ProfessionalFormatter.toJson(sampleData)

        assertThat(json).isNotEmpty()
        assertThat(json).contains("TestProject")
        assertThat(json).contains("src/Main.kt")
        assertThat(json).contains("kotlin")

        // Verify it's valid JSON
        assertDoesNotThrow {
            ObjectMapper().readTree(json)
        }
    }

    @Test
    @DisplayName("Should format to XML correctly")
    fun `test XML formatting`() {
        val xml = ProfessionalFormatter.toXml(sampleData)

        assertThat(xml).isNotEmpty()
        assertThat(xml).contains("<metadata>")
        assertThat(xml).contains("<files>")
        assertThat(xml).contains("TestProject")
        assertThat(xml).contains("src/Main.kt")
    }

    @Test
    @DisplayName("Should format to YAML correctly")
    fun `test YAML formatting`() {
        val yaml = ProfessionalFormatter.toYaml(sampleData)

        assertThat(yaml).isNotEmpty()
        assertThat(yaml).contains("metadata:")
        assertThat(yaml).contains("files:")
        assertThat(yaml).contains("TestProject")
    }

    @Test
    @DisplayName("Should format to Markdown correctly")
    fun `test Markdown formatting`() {
        val markdown = ProfessionalFormatter.toMarkdown(sampleData, includeMetadata = true)

        assertThat(markdown).isNotEmpty()
        assertThat(markdown).contains("# TestProject")
        assertThat(markdown).contains("## Export Information")
        assertThat(markdown).contains("```kotlin")
        assertThat(markdown).contains("src/Main.kt")
    }

    @Test
    @DisplayName("Should format Markdown without metadata")
    fun `test Markdown without metadata`() {
        val markdown = ProfessionalFormatter.toMarkdown(sampleData, includeMetadata = false)

        assertThat(markdown).isNotEmpty()
        assertThat(markdown).doesNotContain("## Export Information")
        assertThat(markdown).contains("src/Main.kt")
    }

    @Test
    @DisplayName("Should format Markdown with TOC")
    fun `test Markdown with table of contents`() {
        val markdown = ProfessionalFormatter.toMarkdown(
            sampleData,
            includeMetadata = true,
            includeToc = true
        )

        assertThat(markdown).contains("## Table of Contents")
        assertThat(markdown).contains("1. [src/Main.kt]")
        assertThat(markdown).contains("2. [src/Utils.kt]")
    }

    @Test
    @DisplayName("Should format to HTML correctly")
    fun `test HTML formatting`() {
        val html = ProfessionalFormatter.toHtml(sampleData, includeMetadata = true)

        assertThat(html).isNotEmpty()
        assertThat(html).contains("<!DOCTYPE html>")
        assertThat(html).contains("<head>")
        assertThat(html).contains("<body>")
        assertThat(html).contains("TestProject")
        assertThat(html).contains("src/Main.kt")
        assertThat(html).contains("copyToClipboard") // Copy functionality
    }

    @Test
    @DisplayName("Should format HTML with proper styling")
    fun `test HTML styling`() {
        val html = ProfessionalFormatter.toHtml(sampleData)

        assertThat(html).contains("<style>")
        assertThat(html).contains("gradient")
        assertThat(html).contains("@media (prefers-color-scheme: dark)") // Theme support
        assertThat(html).contains(".copy-btn") // Copy button
    }

    @Test
    @DisplayName("Should format to plain text correctly")
    fun `test plain text formatting`() {
        val plainText = ProfessionalFormatter.toPlainText(sampleData, includeMetadata = true)

        assertThat(plainText).isNotEmpty()
        assertThat(plainText).contains("CODE EXPORT")
        assertThat(plainText).contains("TestProject")
        assertThat(plainText).contains("FILE: src/Main.kt")
        assertThat(plainText).contains("=" .repeat(80)) // Separator
    }

    @Test
    @DisplayName("Should format timestamps correctly")
    fun `test timestamp formatting`() {
        val timestamp = ProfessionalFormatter.getCurrentTimestamp()

        assertThat(timestamp).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*")
    }

    @Test
    @DisplayName("Should handle empty file list")
    fun `test empty file list handling`() {
        val emptyData = sampleData.copy(files = emptyList())

        assertDoesNotThrow {
            ProfessionalFormatter.toJson(emptyData)
            ProfessionalFormatter.toXml(emptyData)
            ProfessionalFormatter.toMarkdown(emptyData)
            ProfessionalFormatter.toHtml(emptyData)
            ProfessionalFormatter.toPlainText(emptyData)
        }
    }

    @Test
    @DisplayName("Should handle special characters in content")
    fun `test special character handling`() {
        val specialData = sampleData.copy(
            files = listOf(
                ProfessionalFormatter.FileInfo(
                    path = "src/Special.kt",
                    language = "kotlin",
                    content = "val quote = \"Hello & goodbye <tag>\"",
                    lineCount = 1,
                    byteSize = 36L,
                    tokens = 10
                )
            )
        )

        val json = ProfessionalFormatter.toJson(specialData)
        val xml = ProfessionalFormatter.toXml(specialData)
        val html = ProfessionalFormatter.toHtml(specialData)

        assertThat(json).contains("\\\"") // JSON escaping
        assertThat(xml).doesNotContain("<tag>") // Should be escaped or in CDATA
        assertThat(html).contains("&") // HTML should handle it
    }

    @Test
    @DisplayName("Should include all metadata fields")
    fun `test complete metadata inclusion`() {
        val markdown = ProfessionalFormatter.toMarkdown(sampleData, includeMetadata = true)

        assertThat(markdown).contains("Export Time")
        assertThat(markdown).contains("Total Files")
        assertThat(markdown).contains("Total Size")
        assertThat(markdown).contains("Estimated Tokens")
        assertThat(markdown).contains("Git Branch")
        assertThat(markdown).contains("Git Commit")
    }
}
