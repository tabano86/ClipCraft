package com.clipcraft.model

/**
 * The output formats supported by ClipCraft.
 */
enum class OutputFormat {
    MARKDOWN,
    PLAIN,
    HTML
}

/**
 * The main configuration for ClipCraft. (Theme settings have been removed; we always use the IDE’s theme.)
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

    // Removed themeMode (we use IntelliJ’s theme)
    // Additional production fields:
    var customTemplates: List<Any> = emptyList(), // placeholder for potential future use
    var enableDragAndDrop: Boolean = true,
    var filterRegex: String = "",
    var macros: Map<String, String> = emptyMap(), // user-defined macros for text substitution
    var simultaneousExports: Set<OutputFormat> = emptySet(),
    var displayGitMetadata: Boolean = false,
    var syntaxHighlightPreview: Boolean = true,
    var showProgressInStatusBar: Boolean = true,
    var shareToGistEnabled: Boolean = false,
    var exportToCloudServices: Boolean = false,
    var measurePerformance: Boolean = false,
    var perProjectConfig: Boolean = false,
    var locale: String = "en",
    var enableFeedbackButton: Boolean = true,
    var enableNotificationCenter: Boolean = true
)
