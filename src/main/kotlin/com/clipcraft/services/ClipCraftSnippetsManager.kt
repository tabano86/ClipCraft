package com.clipcraft.services

import com.clipcraft.model.Snippet
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import java.util.concurrent.CopyOnWriteArrayList

@Service(Service.Level.PROJECT)
class ClipCraftSnippetsManager(private val project: Project) {
    private val logger = Logger.getInstance(ClipCraftSnippetsManager::class.java)
    private val snippets = CopyOnWriteArrayList<Snippet>()

    fun addSnippet(snippet: Snippet) {
        snippets.add(snippet)
        logger.info("Snippet added: ${snippet.id} from ${snippet.fileName}")
    }

    fun removeSnippet(snippet: Snippet) {
        snippets.remove(snippet)
    }

    fun getAllSnippets(): List<Snippet> = snippets.toList()
    fun clearAll() {
        snippets.clear()
    }

    companion object {
        fun getInstance(project: Project): ClipCraftSnippetsManager = project.getService(ClipCraftSnippetsManager::class.java)
    }
}
