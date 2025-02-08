package com.clipcraft.services

/**
 * A simple engine to apply user-defined macros, e.g., replacing "${KEY}" with "VALUE."
 */
object ClipCraftMacroManager {

    fun applyMacros(text: String, macros: Map<String, String>): String {
        var result = text
        macros.forEach { (macroKey, macroVal) ->
            result = result.replace("\${$macroKey}", macroVal)
        }
        return result
    }
}
