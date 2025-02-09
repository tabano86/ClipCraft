package com.clipcraft.services

/**
 * Simple macro substitution manager.
 * Now purely operates on strings (pure function).
 */
object ClipCraftMacroManager {
    fun applyMacros(text: String, macros: Map<String, String>): String {
        var result = text
        macros.forEach { (key, value) ->
            // We treat pattern as literal, not a regex
            result = result.replace("\${$key}", value)
        }
        return result
    }
}
