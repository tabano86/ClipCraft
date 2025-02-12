package com.clipcraft.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class ClipCraftMacroManager(private val project: Project) {
    fun expandMacro(template: String, context: Map<String, String>): String {
        var result = template
        context.forEach { (k, v) -> result = result.replace("{$k}", v) }
        return result
    }

    companion object {
        fun getInstance(project: Project): ClipCraftMacroManager {
            return project.getService(ClipCraftMacroManager::class.java)
        }
    }
}
