package com.clipcraft.model

/**
 * A group of snippets with a group name for organizational purposes.
 */
class SnippetGroup(val groupName: String, val snippets: MutableList<Snippet> = mutableListOf())
