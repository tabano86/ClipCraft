package com.clipcraft.services

import com.intellij.openapi.diagnostic.Logger

/**
 * Manages snippet storage or queue (in memory).
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

    fun addSnippet(content: String) {
        logger.info("Snippet added:\n$content")
    }
}
