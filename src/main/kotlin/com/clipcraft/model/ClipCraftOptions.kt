package com.clipcraft.model

enum class OutputFormat {
    MARKDOWN, PLAIN, HTML
}

enum class ThemeMode {
    SYSTEM_DEFAULT, LIGHT, DARK
}

data class CustomTemplate(
    val templateName: String,
    val templateContent: String
)

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
    var enableDragAndDrop: Boolean = true, // allow drag-and-drop files
    var filterRegex: String = "",
    var macros: Map<String, String> = emptyMap(), // user-defined macros
    var simultaneousExports: Set<OutputFormat> = emptySet(), // e.g., export multiple formats
    var displayGitMetadata: Boolean = false,
    var syntaxHighlightPreview: Boolean = true,
    var showProgressInStatusBar: Boolean = true,
    var shareToGistEnabled: Boolean = false,
    var exportToCloudServices: Boolean = false, // e.g., Drive, Dropbox
    var measurePerformance: Boolean = false,
    var perProjectConfig: Boolean = false,
    var locale: String = "en", // for localization
    var enableFeedbackButton: Boolean = true,
    var enableNotificationCenter: Boolean = true
)
