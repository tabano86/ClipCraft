package com.clipcraft.model

/**
 * A simple data class for storing plugin preferences.
 */
data class ClipCraftOptions(
    var includeLineNumbers: Boolean = false,
    var showPreview: Boolean = false,
    var exportToFile: Boolean = false,
    var exportFilePath: String = "",
    var includeMetadata: Boolean = false,
    var autoProcess: Boolean = true,
    var largeFileThreshold: Long = 1_048_576, // 1 MB
    var singleCodeBlock: Boolean = false,     // Merge all output into one code block
    var minimizeWhitespace: Boolean = false   // Collapses consecutive blank lines
)
