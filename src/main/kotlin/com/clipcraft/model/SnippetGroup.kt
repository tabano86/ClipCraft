package com.clipcraft.model

/**
 * A collection of snippets grouped under a name.
 */
data class SnippetGroup(
    val name: String,
    val snippets: MutableList<Snippet> = mutableListOf()
)
