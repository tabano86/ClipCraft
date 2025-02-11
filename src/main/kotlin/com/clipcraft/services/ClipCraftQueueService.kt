package com.clipcraft.services

import com.clipcraft.model.Snippet
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class ClipCraftQueueService(val project: Project) {
    private val snippetQueue: MutableList<Snippet> = mutableListOf()

    fun addSnippet(snippet: Snippet) {
        snippetQueue.add(snippet)
    }

    fun removeSnippet(id: String) {
        snippetQueue.removeIf { it.id == id }
    }

    fun clearQueue() {
        snippetQueue.clear()
    }

    fun getAllSnippets(): List<Snippet> = snippetQueue.toList()
}
