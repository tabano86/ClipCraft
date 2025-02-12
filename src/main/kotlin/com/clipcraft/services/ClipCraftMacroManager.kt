package com.clipcraft.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

/**
 * Manages macros for expanding placeholders in snippets.
 * For example, a snippet may contain {DATE} which is replaced with the current date.
 */
@Service(Service.Level.PROJECT)
class ClipCraftMacroManager(private val project: Project) {

    /**
     * Expands macros within the given template using the provided context.
     */
    fun expandMacro(template: String, context: Map<String, String>): String {
        var result = template
        context.forEach { (key, value) ->
            result = result.replace("{$key}", value)
        }
        return result
    }

    companion object {
        /**
         * Retrieve the instance from the given project.
         */
        fun getInstance(project: Project): ClipCraftMacroManager {
            return project.getService(ClipCraftMacroManager::class.java)
        }
    }
}
