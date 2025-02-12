package com.clipcraft.model

/**
 * Main options for snippet extraction, formatting, concurrency, etc.
 * GPT-related fields were removed.
 */
data class ClipCraftOptions(
    var includeLineNumbers: Boolean = false,
    var outputFormat: OutputFormat = OutputFormat.MARKDOWN,
    var chunkSize: Int = 4000,
    var compressionMode: CompressionMode = CompressionMode.NONE,
    var removeImports: Boolean = false,
    var removeComments: Boolean = false,
    var trimWhitespace: Boolean = true,
    var removeEmptyLines: Boolean = false,
    var useGitIgnore: Boolean = false,
    var enableDirectoryPatternMatching: Boolean = false,
    var additionalIgnorePatterns: String? = null,
    var invertIgnorePatterns: Boolean = false,
    var themeMode: ThemeMode = ThemeMode.LIGHT,
    var ignorePatterns: MutableList<String> = mutableListOf(),
    var maxConcurrentTasks: Int = 4,
    var concurrencyMode: ConcurrencyMode = ConcurrencyMode.DISABLED,
    var includeDirectorySummary: Boolean = false,
    var collapseBlankLines: Boolean = false,
    var removeLeadingBlankLines: Boolean = false,
    var singleLineOutput: Boolean = false,
    var includeMetadata: Boolean = false,
    var includeGitInfo: Boolean = false,

    var autoDetectLanguage: Boolean = false,
    var overlapStrategy: OverlapStrategy = OverlapStrategy.SINGLE_LINE,
    var chunkStrategy: ChunkStrategy = ChunkStrategy.NONE,

    // Additional compression toggles
    var selectiveCompression: Boolean = false,

    // Binary detection
    var detectBinary: Boolean = false,
    var binaryCheckThreshold: Int = 2000,

    // Lint
    var showLint: Boolean = false,

    // Renamed from GPT-based naming:
    var snippetHeaderText: String? = null,
    var snippetFooterText: String? = null,
) {
    fun resolveConflicts() {
        // Adjust concurrency if needed
        if (concurrencyMode == ConcurrencyMode.DISABLED && maxConcurrentTasks > 1) {
            // It's valid to keep it at >1, but concurrency won't be used.
        }
        // If singleLineOutput is true, chunking is effectively disabled
        if (singleLineOutput) {
            chunkStrategy = ChunkStrategy.NONE
        }
        // Ensure chunkSize is valid
        if (chunkSize < 1) chunkSize = 4000
        if (binaryCheckThreshold < 1) binaryCheckThreshold = 1000
    }
}
