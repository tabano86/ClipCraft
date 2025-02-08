package com.clipcraft.services

import com.clipcraft.model.ThemeMode
import com.intellij.ide.ui.LafManager
import com.intellij.openapi.application.ApplicationManager

object ClipCraftThemeManager {

    fun applyTheme(themeMode: ThemeMode) {
        val lafManager = LafManager.getInstance()
        val chosenLookAndFeel = when (themeMode) {
            ThemeMode.LIGHT -> lafManager.installedLookAndFeels.find { it.name.contains("Light", ignoreCase = true) }
            ThemeMode.DARK -> lafManager.installedLookAndFeels.find {
                it.name.contains("Dark", ignoreCase = true) || it.name.contains("Darcula", ignoreCase = true)
            }

            else -> null
        }
        lafManager.currentLookAndFeel = chosenLookAndFeel ?: lafManager.installedLookAndFeels.firstOrNull()
                ?: error("No Look and Feel available")
        ApplicationManager.getApplication().invokeLater { lafManager.updateUI() }
    }
}
