package com.clipcraft.services

import com.clipcraft.model.ThemeMode
import com.intellij.ide.ui.LafManager
import com.intellij.openapi.application.ApplicationManager

object ClipCraftThemeManager {

    fun applyTheme(themeMode: ThemeMode) {
        val lafManager = LafManager.getInstance()
        when (themeMode) {
            ThemeMode.LIGHT -> {
                // Select a look and feel that contains "Light" in its name
                lafManager.currentLookAndFeel =
                    lafManager.installedLookAndFeels.find { it.name.contains("Light", ignoreCase = true) }
                        ?: selectFallbackLaf(lafManager)
            }
            ThemeMode.DARK -> {
                // Select a look and feel that contains "Dark" or "Darcula"
                lafManager.currentLookAndFeel =
                    lafManager.installedLookAndFeels.find {
                        it.name.contains("Dark", ignoreCase = true) || it.name.contains("Darcula", ignoreCase = true)
                    } ?: selectFallbackLaf(lafManager)
            }
            else -> {
                // For any other mode, fallback to a light theme.
                lafManager.currentLookAndFeel =
                    lafManager.installedLookAndFeels.find { it.name.contains("Light", ignoreCase = true) }
                        ?: selectFallbackLaf(lafManager)
                // Note: ExperimentalUI#setNewUI is an internal API and should not be used.
            }
        }
        ApplicationManager.getApplication().invokeLater { lafManager.updateUI() }
    }

    private fun selectFallbackLaf(lafManager: LafManager) =
        lafManager.installedLookAndFeels.firstOrNull() ?: error("No Look and Feel available")
}