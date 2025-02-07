package com.clipcraft.model

/**
 * Stores all user preferences for ClipCraft.
 *
 * Tooltips (shown in UI) describe each field.
 */
data class ClipCraftOptions(
    // Output formatting
    var includeLineNumbers: Boolean = false,
    var showPreview: Boolean = false,
    var exportToFile: Boolean = false,
    var exportFilePath: String = "",
    var includeMetadata: Boolean = false,
    var autoProcess: Boolean = true,
    var largeFileThreshold: Long = 1_048_576, // 1 MB threshold for background loading
    var singleCodeBlock: Boolean = false,     // Merge all output into a single code block
    var minimizeWhitespace: Boolean = false,    // Remove consecutive blank lines

    // Advanced options for excluding files/directories and cleaning code
    var ignoreFolders: List<String> = listOf(".git", "build", "out", "node_modules"),
    var ignoreFiles: List<String> = emptyList(),
    var ignorePatterns: List<String> = emptyList(),
    var removeComments: Boolean = false,       // Remove comment lines from source files
    var trimLineWhitespace: Boolean = false      // Remove trailing whitespace on each line
)
