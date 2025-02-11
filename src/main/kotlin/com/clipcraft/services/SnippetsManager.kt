package com.clipcraft.services

import com.clipcraft.model.Snippet
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Example snippet manager at the project level. You can also unify this with
 * ClipCraftQueueService if you only need one snippet queue.
 */
@Service(Service.Level.PROJECT)
class SnippetsManager(val project: Project) : Disposable {

    private val snippets = CopyOnWriteArrayList<Snippet>()

    fun addSnippet(snippet: Snippet) {
        snippets.add(snippet)
    }

    fun getAllSnippets(): List<Snippet> = snippets.toList()

    fun removeSnippet(snippet: Snippet) {
        snippets.remove(snippet)
    }

    fun clearAll() {
        snippets.clear()
    }

    override fun dispose() {
        snippets.clear()
    }
}
