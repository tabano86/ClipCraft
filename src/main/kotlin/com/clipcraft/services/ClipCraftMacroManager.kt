package com.clipcraft.services

object ClipCraftMacroManager {
    /**
     * Simple macro replacement engine.
     * If user has macros like {"MY_NAME":"John Doe"},
     * we replace occurrences of ${MY_NAME} in text with "John Doe".
     */
    fun applyMacros(text: String, macros: Map<String, String>): String {
        var result = text
        macros.forEach { (macroKey, macroVal) ->
            result = result.replace("\${$macroKey}", macroVal)
        }
        return result
    }
}
