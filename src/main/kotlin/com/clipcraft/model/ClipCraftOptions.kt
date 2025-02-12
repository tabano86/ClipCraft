package com.clipcraft.model

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
    var ignoreFiles: List<String>? = null,
    var ignoreFolders: List<String>? = null,
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
    var selectiveCompression: Boolean = false,
    var detectBinary: Boolean = false,
    var binaryCheckThreshold: Int = 2000,
    var showLint: Boolean = false,
    var snippetHeaderText: String? = "/* ClipCraft Snippet */",
    var snippetFooterText: String? = "/* End of Snippet */",
    // New options:
    var includeImageFiles: Boolean = false, // if false, image files are replaced with a placeholder
    var lintErrorsOnly: Boolean = false, // if true, only errors are reported in lint results
    var lintWarningsOnly: Boolean = false, // if true, only warnings are reported in lint results
    var addSnippetToQueue: Boolean = false, // if true, snippets from caret are added to a queue
) {
    fun resolveConflicts() {
        if (singleLineOutput) chunkStrategy = ChunkStrategy.NONE
        if (chunkSize < 1) chunkSize = 4000
        if (binaryCheckThreshold < 1) binaryCheckThreshold = 1000
    }
}
