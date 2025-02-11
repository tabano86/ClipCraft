package com.clipcraft.model

import java.util.UUID

/**
 * Represents a single code snippet. This unifies the older SnippetData with
 * additional fields for file path, Git info, etc.
 */
data class Snippet(
    val id: String = UUID.randomUUID().toString(),
    var filePath: String? = null,
    var relativePath: String? = null,
    var content: String,
    var fileName: String? = null,
    var language: String? = null,
    var fileSizeBytes: Long = 0,
    var lastModified: Long = System.currentTimeMillis(),
    var gitCommitHash: String? = null,
    var gitAuthor: String? = null,
    val createdTimestamp: Long = System.currentTimeMillis()
)
