package com.clipcraft.util

import com.clipcraft.model.*

object CodeFormatterTestHelper {
    fun createOptions(
        compressionMode: CompressionMode,
        selectiveCompression: Boolean = false
    ): ClipCraftOptions {
        return ClipCraftOptions(
            compressionMode = compressionMode,
            selectiveCompression = selectiveCompression,
            includeLineNumbers = false,
            outputFormat = OutputFormat.MARKDOWN,
            chunkSize = 4000,
            removeImports = false,
            removeComments = false,
            trimWhitespace = false,
            removeEmptyLines = false,
            useGitIgnore = false,
            themeMode = ThemeMode.LIGHT,
            ignorePatterns = mutableListOf(),
            maxConcurrentTasks = 4,
            concurrencyEnabled = false,
            enableChunkingForGPT = false,
            includeDirectorySummary = false,
            collapseBlankLines = false,
            removeLeadingBlankLines = false,
            singleLineOutput = false,
            includeMetadata = false,
            includeGitInfo = false,
            gptTemplates = mutableListOf(),
            autoDetectLanguage = false,
            overlapStrategy = OverlapStrategy.SINGLE_LINE,
            chunkStrategy = ChunkStrategy.NONE,
            concurrencyMode = ConcurrencyMode.DISABLED,
            gptHeaderText = null,
            gptFooterText = null,
            // New fields for testing selective compression and additional ignore options:
            additionalIgnorePatterns = "",
            invertIgnorePatterns = false,
            enableDirectoryPatternMatching = false
        )
    }
}
