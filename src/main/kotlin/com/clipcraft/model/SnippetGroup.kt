package com.clipcraft.model

/**
 * A named group of related snippets.
 */
class SnippetGroup(
    val groupName: String,
    val snippets: MutableList<Snippet> = mutableListOf(),
)
