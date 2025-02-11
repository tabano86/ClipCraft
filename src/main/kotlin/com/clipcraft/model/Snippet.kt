package com.clipcraft.model

import java.util.UUID

data class Snippet(
    val id: String = UUID.randomUUID().toString(),
    var content: String,
    var fileName: String? = null,
    var language: String? = null,
    var metadata: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
