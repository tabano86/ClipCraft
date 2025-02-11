package com.clipcraft.services

import com.intellij.openapi.diagnostic.Logger

/**
 * Manages snippet storage or a snippet queue.
 */
class ClipCraftSnippetManager private constructor() {

    private val logger = Logger.getInstance(ClipCraftSnippetManager::class.java)

    companion object {
        private var instance: ClipCraftSnippetManager? = null

        @JvmStatic
        fun getInstance(): ClipCraftSnippetManager {
            if (instance == null) {
                instance = ClipCraftSnippetManager()
            }
            return instance!!
        }
    }

    /**
     * Adds a snippet (in this stub, the snippet content is simply logged).
     */
    fun addSnippet(content: String) {
        logger.info("Snippet added:\n$content")
    }
}
