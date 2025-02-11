package com.clipcraft.model

data class SnippetGroup(
    val name: String,
    val snippets: MutableList<Snippet> = mutableListOf()
)
