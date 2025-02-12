package com.clipcraft.util

import com.clipcraft.model.ChunkStrategy
import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.CompressionMode
import com.clipcraft.model.ConcurrencyMode
import com.clipcraft.model.OutputFormat
import com.clipcraft.model.OverlapStrategy
import com.clipcraft.model.ThemeMode

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
