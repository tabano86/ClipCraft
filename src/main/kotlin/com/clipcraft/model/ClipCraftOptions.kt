package com.clipcraft.model

data class ClipCraftOptions(
    var includeLineNumbers: Boolean = false,
    var outputFormat: OutputFormat = OutputFormat.MARKDOWN,
    var chunkSize: Int = 4000,
    var compressionMode: CompressionMode = CompressionMode.NONE,
    var removeImports: Boolean = false,
    var removeComments: Boolean = false,
    var trimWhitespace: Boolean = true,
    var removeEmptyLines: Boolean = false,         // Remove empty lines
    var useGitIgnore: Boolean = false,
    var enableDirectoryPatternMatching: Boolean = false,  // Directory pattern matching
    var additionalIgnorePatterns: String? = null,    // Additional glob patterns (comma-separated)
    var invertIgnorePatterns: Boolean = false,       // Negative match option
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

    // New fields for GPT prompt templates, or other stored info
    var gptTemplates: MutableList<GPTPromptTemplate> = mutableListOf(
        GPTPromptTemplate("ExplainThisCode", "Explain this code"),
        GPTPromptTemplate("OptimizeSnippet", "Optimize this snippet")
    ),
    var autoDetectLanguage: Boolean = false,
    var overlapStrategy: OverlapStrategy = OverlapStrategy.SINGLE_LINE,
    var chunkStrategy: ChunkStrategy = ChunkStrategy.NONE,
    var concurrencyMode: ConcurrencyMode = ConcurrencyMode.DISABLED,

    /**
     * User-defined text that will be prepended to snippet output.
     */
    var gptHeaderText: String? = null,

    /**
     * User-defined text that will be appended to snippet output.
     */
    var gptFooterText: String? = null,

    // NEW: Lists for explicit file/folder ignoring
    var ignoreFiles: List<String>? = null,
    var ignoreFolders: List<String>? = null,
    var selectiveCompression: Boolean = false,
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

        // Overlap: if singleLineOutput is on, chunking should be off
        if (singleLineOutput && chunkStrategy != ChunkStrategy.NONE) {
            when (overlapStrategy) {
                OverlapStrategy.SINGLE_LINE -> chunkStrategy = ChunkStrategy.NONE
                OverlapStrategy.CHUNKING, OverlapStrategy.ASK -> singleLineOutput = false
            }
        }
    }
}
