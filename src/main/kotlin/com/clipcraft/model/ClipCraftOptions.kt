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
 * Next-gen configuration data class for ClipCraft,
 * including .gitignore usage, advanced compression modes,
 * and toggles for auto-apply, etc.
 */
data class ClipCraftOptions(
    var includeLineNumbers: Boolean = false,
    var showPreview: Boolean = false,
    var exportToFile: Boolean = false,
    var exportFilePath: String = "",
    var includeMetadata: Boolean = false,
    var autoProcess: Boolean = true,
    var largeFileThreshold: Long = 1_048_576, // 1 MB
    var singleCodeBlock: Boolean = false,
    var minimizeWhitespace: Boolean = false,

    var ignoreFolders: List<String> = listOf(".git", "build", "out", "node_modules"),
    var ignoreFiles: List<String> = emptyList(),
    var ignorePatterns: List<String> = emptyList(),

    var removeComments: Boolean = false,
    var trimLineWhitespace: Boolean = false,
    var removeImports: Boolean = false,
    var outputFormat: OutputFormat = OutputFormat.MARKDOWN,
    var filterRegex: String = "",

    // Macros
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

    // Next-Gen features
    var includeDirectorySummary: Boolean = false,
    var directorySummaryDepth: Int = 999,
    var collapseBlankLines: Boolean = false,
    var removeLeadingBlankLines: Boolean = false,
    var singleLineOutput: Boolean = false,

    // GPT chunking
    var enableChunkingForGPT: Boolean = false,
    var maxChunkSize: Int = 3000,

    // Theme mode
    var themeMode: ThemeMode = ThemeMode.SYSTEM_DEFAULT,

    // .gitignore usage
    var useGitIgnore: Boolean = false,

    // Advanced compression
    var compressionMode: CompressionMode = CompressionMode.NONE,
    var selectiveCompression: Boolean = false,

    // If true, the UI automatically applies changes on each toggle without needing "Apply"
    var autoApplyOnChange: Boolean = false
)
