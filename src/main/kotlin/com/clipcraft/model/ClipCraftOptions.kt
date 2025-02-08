package com.clipcraft.model

/**
 * Defines the available output formats for ClipCraft.
 */
enum class OutputFormat {
    MARKDOWN,
    PLAIN,
    HTML
}

/**
 * A next-gen configuration data class for ClipCraft.
 * You can save multiple profiles with different combos of these settings.
 */
data class ClipCraftOptions(
    var includeLineNumbers: Boolean = false,
    var showPreview: Boolean = false,
    var exportToFile: Boolean = false,
    var exportFilePath: String = "",
    var includeMetadata: Boolean = false,
    var autoProcess: Boolean = true,
    var largeFileThreshold: Long = 1_048_576, // 1 MB
    var singleCodeBlock: Boolean = false,     // (Unused in example, but left for future use)
    var minimizeWhitespace: Boolean = false,

    var ignoreFolders: List<String> = listOf(".git", "build", "out", "node_modules"),
    var ignoreFiles: List<String> = emptyList(),
    var ignorePatterns: List<String> = emptyList(),

    var removeComments: Boolean = false,
    var trimLineWhitespace: Boolean = false,
    var removeImports: Boolean = false,
    var outputFormat: OutputFormat = OutputFormat.MARKDOWN,
    var filterRegex: String = "",

    // New advanced macros, expansions
    var macros: Map<String, String> = emptyMap(),

    // Multi-format export
    var simultaneousExports: Set<OutputFormat> = emptySet(),

    // Git integration
    var displayGitMetadata: Boolean = false,

    // Editor preview integration
    var syntaxHighlightPreview: Boolean = true,
    var showProgressInStatusBar: Boolean = true,

    // Gist & cloud sharing
    var shareToGistEnabled: Boolean = false,
    var exportToCloudServices: Boolean = false,

    // Performance metrics
    var measurePerformance: Boolean = false,

    // Project config toggles
    var perProjectConfig: Boolean = false,

    // UI/Localization toggles
    var locale: String = "en",
    var enableFeedbackButton: Boolean = true,
    var enableNotificationCenter: Boolean = true,

    // Next-Gen features:
    var includeDirectorySummary: Boolean = false,
    var directorySummaryDepth: Int = 999,
    var collapseBlankLines: Boolean = false,
    var removeLeadingBlankLines: Boolean = false,
    var singleLineOutput: Boolean = false,

    // GPT chunking
    var enableChunkingForGPT: Boolean = false,
    var maxChunkSize: Int = 3000
)
