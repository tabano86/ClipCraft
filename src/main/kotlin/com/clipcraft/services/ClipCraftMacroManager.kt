package com.clipcraft.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class ClipCraftMacroManager(private val project: Project) {
    // Placeholder for macros or expansions that the user can define,
    // e.g., placeholders in GPT prompts or snippet expansions.

    fun expandMacro(template: String, context: Map<String, String>): String {
        var result = template
        context.forEach { (key, value) ->
            result = result.replace("{{$key}}", value)
        }
        return result
    }
}
