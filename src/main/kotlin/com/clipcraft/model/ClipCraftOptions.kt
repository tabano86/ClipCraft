package com.clipcraft.model

/**
 * Possible output formats for code snippet.
 */
enum class OutputFormat {
    MARKDOWN,
    PLAIN,
    HTML
}

/**
 * UI theme modes the user may select.
 */
enum class ThemeMode {
    SYSTEM_DEFAULT,
    LIGHT,
    DARK
}

/**
 * A user-defined template object (for advanced formatting).
 */
data class CustomTemplate(
    val templateName: String,
    val templateContent: String
)

/**
 * The main configuration for ClipCraft, stored persistently and modifiable via UI.
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

    var themeMode: ThemeMode = ThemeMode.SYSTEM_DEFAULT,
    var customTemplates: List<CustomTemplate> = emptyList(),
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
