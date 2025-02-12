package com.clipcraft.model

import java.util.*

data class Snippet(
    val id: String = UUID.randomUUID().toString(),
    val filePath: String,
    val relativePath: String?,
    val fileName: String,
    val fileSizeBytes: Long,
    val lastModified: Long,
    val content: String,
    var language: String? = null,
    var gitCommitHash: String? = null
)
