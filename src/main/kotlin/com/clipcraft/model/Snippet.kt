package com.clipcraft.model

import java.util.*

/**
 * Represents a single code snippet, with multiple constructors for convenience.
 */
data class Snippet(
    val id: String = UUID.randomUUID().toString(),
    val filePath: String,
    val relativePath: String?,
    val fileName: String,
    val fileSizeBytes: Long,
    val lastModified: Long,
    val content: String,

    var language: String? = null,
    var gitCommitHash: String? = null,
) {
    constructor(content: String, fileName: String) : this(
        id = UUID.randomUUID().toString(),
        filePath = fileName,
        relativePath = null,
        fileName = fileName,
        fileSizeBytes = content.length.toLong(),
        lastModified = System.currentTimeMillis(),
        content = content,
        language = null,
        gitCommitHash = null,
    )

    constructor(content: String, fileName: String, relativePath: String) : this(
        id = UUID.randomUUID().toString(),
        filePath = fileName,
        relativePath = relativePath,
        fileName = fileName,
        fileSizeBytes = content.length.toLong(),
        lastModified = System.currentTimeMillis(),
        content = content,
        language = null,
        gitCommitHash = null,
    )
}
