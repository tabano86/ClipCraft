package com.clipcraft.model

/**
 * Holds user-facing settings for snippet processing, concurrency, ignoring files, etc.
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
    var concurrencyEnabled: Boolean = false, // legacy
    var enableChunkingForGPT: Boolean = false, // legacy
    var includeDirectorySummary: Boolean = false,
    var collapseBlankLines: Boolean = false,
    var removeLeadingBlankLines: Boolean = false,
    var singleLineOutput: Boolean = false,
    var includeMetadata: Boolean = false,
    var includeGitInfo: Boolean = false,

    // GPT-related expansions
    var gptTemplates: MutableList<GPTPromptTemplate> = mutableListOf(
        GPTPromptTemplate("ExplainThisCode", "Explain this code"),
        GPTPromptTemplate("OptimizeSnippet", "Optimize this snippet")
    ),
    var autoDetectLanguage: Boolean = false,
    var overlapStrategy: OverlapStrategy = OverlapStrategy.SINGLE_LINE,
    var chunkStrategy: ChunkStrategy = ChunkStrategy.NONE,
    var concurrencyMode: ConcurrencyMode = ConcurrencyMode.DISABLED,

    var gptHeaderText: String? = null,
    var gptFooterText: String? = null,

    // Additional ignoring
    var ignoreFiles: List<String>? = null,
    var ignoreFolders: List<String>? = null,
    var selectiveCompression: Boolean = false
) {
    fun resolveConflicts() {
        // Upgrade legacy concurrency
        if (concurrencyEnabled && concurrencyMode == ConcurrencyMode.DISABLED) {
            concurrencyMode = ConcurrencyMode.THREAD_POOL
        } else if (!concurrencyEnabled && concurrencyMode != ConcurrencyMode.DISABLED) {
            concurrencyMode = ConcurrencyMode.DISABLED
        }

        // Upgrade legacy chunking
        if (enableChunkingForGPT && chunkStrategy == ChunkStrategy.NONE) {
            chunkStrategy = ChunkStrategy.BY_SIZE
        } else if (!enableChunkingForGPT && chunkStrategy != ChunkStrategy.NONE) {
            chunkStrategy = ChunkStrategy.NONE
        }

        // If singleLineOutput is on, chunking is effectively meaningless.
        // We disable chunking and set chunkStrategy = NONE for safety.
        if (singleLineOutput) {
            chunkStrategy = ChunkStrategy.NONE
        }
    }
}
