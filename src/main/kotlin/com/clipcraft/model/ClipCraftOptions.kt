package com.clipcraft.model

import kotlinx.serialization.Serializable

@Serializable
data class ClipCraftOptions(
    var concurrencyMode: ConcurrencyMode = ConcurrencyMode.DISABLED,
    var maxConcurrentTasks: Int = 4,
    var chunkStrategy: ChunkStrategy = ChunkStrategy.NONE,
    var chunkSize: Int = 4000,
    var overlapStrategy: OverlapStrategy = OverlapStrategy.SINGLE_LINE,
    var compressionMode: CompressionMode = CompressionMode.NONE,
    var selectiveCompression: Boolean = false,
    var outputFormat: OutputFormat = OutputFormat.MARKDOWN,
    var includeLineNumbers: Boolean = false,
    var removeImports: Boolean = false,
    var removeComments: Boolean = false,
    var trimWhitespace: Boolean = true,
    var removeEmptyLines: Boolean = false,
    var singleLineOutput: Boolean = false,
    var includeDirectorySummary: Boolean = false,
    var hierarchicalDirectorySummary: Boolean = false,
    var includeMetadata: Boolean = false,
    var metadataTemplate: String? = null,
    var snippetHeaderText: String? = null,
    var snippetFooterText: String? = null,
    var showLint: Boolean = false,
    var lintErrorsOnly: Boolean = false,
    var lintWarningsOnly: Boolean = false,
    var includeLintInOutput: Boolean = false,
    var includeGitInfo: Boolean = false,
    var useGitIgnore: Boolean = false,
    var enableDirectoryPatternMatching: Boolean = false,
    var additionalIgnorePatterns: String? = null,
    var invertIgnorePatterns: Boolean = false,
    var ignorePatterns: MutableList<String> = mutableListOf(),
    var ignoreFiles: List<String>? = null,
    var ignoreFolders: List<String>? = null,
    var detectBinary: Boolean = false,
    var binaryCheckThreshold: Int = 2000,
    var addSnippetToQueue: Boolean = false,
    var includeImageFiles: Boolean = false,
    var autoDetectLanguage: Boolean = false,
    var themeMode: ThemeMode = ThemeMode.LIGHT,
    var collapseBlankLines: Boolean = false,
    var removeLeadingBlankLines: Boolean = false,
    var outputMacroTemplate: String? = null,
    var outputTarget: OutputTarget = OutputTarget.CLIPBOARD,
    var includeIdeProblems: Boolean = false,
) {
    fun resolveConflicts() {
        if (singleLineOutput) chunkStrategy = ChunkStrategy.NONE
        if (chunkSize < 1) chunkSize = 4000
        if (binaryCheckThreshold < 1) binaryCheckThreshold = 1000
    }
}
