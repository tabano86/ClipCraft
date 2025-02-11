package com.clipcraft.model

data class ClipCraftOptions(
    var includeLineNumbers: Boolean = false,
    var outputFormat: OutputFormat = OutputFormat.MARKDOWN,
    var chunkSize: Int = 4000,
    var compressionMode: CompressionMode = CompressionMode.NONE,
    var removeImports: Boolean = false,
    var removeComments: Boolean = false,
    var trimWhitespace: Boolean = true,
    var useGitIgnore: Boolean = false,
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
    var gptTemplates: MutableList<GPTPromptTemplate> = mutableListOf(
        GPTPromptTemplate("ExplainThisCode", "Explain this code"),
        GPTPromptTemplate("OptimizeSnippet", "Optimize this snippet")
    ),
    var autoDetectLanguage: Boolean = false,
    var overlapStrategy: OverlapStrategy = OverlapStrategy.SINGLE_LINE,
    var chunkStrategy: ChunkStrategy = ChunkStrategy.NONE,
    var concurrencyMode: ConcurrencyMode = ConcurrencyMode.DISABLED
) {
    /**
     * Called to reconcile contradictory or legacy settings fields.
     */
    fun resolveConflicts() {
        // Upgrade legacy concurrency
        if (concurrencyEnabled && concurrencyMode == ConcurrencyMode.DISABLED) {
            concurrencyMode = ConcurrencyMode.THREAD_POOL
        } else if (!concurrencyEnabled && concurrencyMode != ConcurrencyMode.DISABLED) {
            // If concurrency is not actually enabled but user sets COROUTINES or THREAD_POOL
            concurrencyMode = ConcurrencyMode.DISABLED
        }

        // Upgrade legacy chunking
        if (enableChunkingForGPT && chunkStrategy == ChunkStrategy.NONE) {
            chunkStrategy = ChunkStrategy.BY_SIZE
        } else if (!enableChunkingForGPT && chunkStrategy != ChunkStrategy.NONE) {
            chunkStrategy = ChunkStrategy.NONE
        }

        // Overlap: singleLine vs chunking
        if (singleLineOutput && chunkStrategy != ChunkStrategy.NONE) {
            when (overlapStrategy) {
                OverlapStrategy.SINGLE_LINE -> {
                    chunkStrategy = ChunkStrategy.NONE
                }
                OverlapStrategy.CHUNKING -> {
                    singleLineOutput = false
                }
                OverlapStrategy.ASK -> {
                    // Defaults to chunking for now
                    singleLineOutput = false
                }
            }
        }
    }
}