package com.clipcraft.services

object ClipCraftMacroManager {
    /**
     * Simple macro replacement: replaces occurrences of ${KEY} with value from macros.
     */
    fun applyMacros(text: String, macros: Map<String, String>): String {
        var result = text
        macros.forEach { (macroKey, macroVal) ->
            result = result.replace("\${$macroKey}", macroVal)
        }
        return result
    }
}
