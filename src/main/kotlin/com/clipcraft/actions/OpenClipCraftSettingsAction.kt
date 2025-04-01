package com.clipcraft.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil

class OpenClipCraftSettingsAction : AnAction("Settings", "Opens ClipCraft settings", null) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        // Opens the settings dialog to our configurable with id "clipcraft.settings"
        ShowSettingsUtil.getInstance().showSettingsDialog(project, "clipcraft.settings")
    }
}
