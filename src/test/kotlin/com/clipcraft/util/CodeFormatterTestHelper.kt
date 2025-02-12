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
        selectiveCompression: Boolean = false,
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
            concurrencyMode = ConcurrencyMode.DISABLED,
            includeDirectorySummary = false,
            collapseBlankLines = false,
            removeLeadingBlankLines = false,
            singleLineOutput = false,
            includeMetadata = false,
            includeGitInfo = false,
            autoDetectLanguage = false,
            overlapStrategy = OverlapStrategy.SINGLE_LINE,
            chunkStrategy = ChunkStrategy.NONE,
            additionalIgnorePatterns = "",
            invertIgnorePatterns = false,
            enableDirectoryPatternMatching = false,
            detectBinary = false,
            binaryCheckThreshold = 2000,
            showLint = false,
        )
    }
}
