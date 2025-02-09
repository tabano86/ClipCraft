package com.clipcraft.services

object ClipCraftMacroManager {
    fun applyMacros(text: String, macros: Map<String, String>): String {
        var result = text
        macros.forEach { (key, value) ->
            result = result.replace("\${$key}", value)
        }
        return result
    }
}
