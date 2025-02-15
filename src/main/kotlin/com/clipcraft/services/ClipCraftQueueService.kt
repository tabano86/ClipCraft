package com.clipcraft.services

import com.clipcraft.model.Snippet
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class ClipCraftQueueService(val project: Project) {
    private val queue = mutableListOf<Snippet>()
    fun addSnippet(snippet: Snippet) {
        queue.add(snippet)
    }

    fun removeSnippet(id: String) {
        queue.removeIf { it.id == id }
    }

    fun clearQueue() {
        queue.clear()
    }

    fun getAllSnippets(): List<Snippet> = queue.toList()

    companion object {
        fun getInstance(project: Project): ClipCraftQueueService =
            project.getService(ClipCraftQueueService::class.java)
    }
}
