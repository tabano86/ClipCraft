package com.clipcraft.model

import java.nio.file.Path

data class ExportOptions(
    // Basic filtering
    val includeGlobs: String,
    val excludeGlobs: String,
    val maxFileSizeKb: Int,

    // Output format
    val outputFormat: OutputFormat = OutputFormat.MARKDOWN,
    val exportToFile: Boolean = false,
    val exportFilePath: Path? = null,

    // Content options
    val includeLineNumbers: Boolean = false,
    val stripComments: Boolean = false,
    val stripWhitespace: Boolean = false,
    val includeMetadata: Boolean = true,
    val includeGitInfo: Boolean = false,
    val includeTimestamp: Boolean = true,
    val includeTableOfContents: Boolean = false,
    val includeStatistics: Boolean = true,

    // Size management
    val enableChunking: Boolean = false,
    val maxTokens: Int = 100000,
    val chunkStrategy: ChunkStrategy = ChunkStrategy.BY_SIZE,

    // Advanced filtering
    val useRegexFiltering: Boolean = false,
    val regexPattern: String = "",
    val respectGitignore: Boolean = true,
    val minFileSizeBytes: Long = 0,
    val maxFileSizeBytes: Long = Long.MAX_VALUE,
    val includeOnlyModifiedAfter: Long? = null,
    val minLineCount: Int = 0,
    val maxLineCount: Int = Int.MAX_VALUE,

    // Security
    val detectSecrets: Boolean = true,
    val maskSecrets: Boolean = true,
    val warnPII: Boolean = true,

    // Path format
    val pathFormat: PathFormat = PathFormat.RELATIVE,
    val customPathPrefix: String = "",

    // Code processing
    val validateSyntax: Boolean = false,
    val extractTodos: Boolean = false,
    val extractAnnotations: Boolean = false,
    val extractDocumentation: Boolean = false,

    // File grouping
    val groupByDirectory: Boolean = true,
    val sortFiles: FileSortOrder = FileSortOrder.PATH_ALPHABETICAL,
)

enum class ChunkStrategy {
    BY_SIZE,
    BY_FILE_COUNT,
    BY_DIRECTORY,
    BY_FILE_TYPE,
    SMART,
}

enum class PathFormat {
    RELATIVE,
    ABSOLUTE,
    PROJECT_RELATIVE,
    CUSTOM,
}

enum class FileSortOrder {
    PATH_ALPHABETICAL,
    NAME_ALPHABETICAL,
    SIZE_ASCENDING,
    SIZE_DESCENDING,
    MODIFIED_DATE,
    EXTENSION,
}
