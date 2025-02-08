package com.clipcraft.services

import com.clipcraft.model.ThemeMode
import com.intellij.ide.ui.LafManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.ExperimentalUI

object ClipCraftThemeManager {

    fun applyTheme(themeMode: ThemeMode) {
        // This is a simplified approach.
        // In practice, you might apply specific light/dark UI themes or rely on the IDE's system default.
        val lafManager = LafManager.getInstance()
        when (themeMode) {
            ThemeMode.LIGHT -> {
                // Force a known Light LaF
                // e.g., IntelliJ Light
                lafManager.currentLookAndFeel =
                    lafManager.installedLookAndFeels.find { it.name.contains("Light") } ?: lafManager.defaultLightLaf
            }
            ThemeMode.DARK -> {
                // Force a known Dark LaF
                // e.g., Darcula
                lafManager.currentLookAndFeel =
                    lafManager.installedLookAndFeels.find { it.name.contains("Dark") || it.name.contains("Darcula") }
            }
            else -> {
                // Use system default or IntelliJ default
                lafManager.setCurrentLookAndFeel(lafManager.defaultLightLaf)
                ExperimentalUI.setNewUI(false) // Example toggling new UI
            }
        }
        ApplicationManager.getApplication().invokeLater { lafManager.updateUI() }
    }
}
