package com.clipcraft.services

object ClipCraftMacroManager {

    fun applyMacros(text: String, macros: Map<String, String>): String {
        var result = text
        macros.forEach { (macroKey, macroVal) ->
            result = result.replace("\${$macroKey}", macroVal)
        }
        return result
    }
}
