package com.clipcraft.actions

import com.clipcraft.services.ClipCraftNotificationCenter
import com.clipcraft.services.ClipCraftSettings
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.JOptionPane

/**
 * Allows switching among multiple named profiles quickly from the Tools menu.
 */
class ClipCraftSwitchProfileAction : AnAction(
    "ClipCraft: Switch Profile",
    "Switch among multiple named ClipCraft profiles",
    null
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val settings = ClipCraftSettings.getInstance()
        val profileNames = settings.listProfileNames()
        if (profileNames.isEmpty()) {
            ClipCraftNotificationCenter.notifyInfo("No profiles found.", project)
            return
        }
        val selected = JOptionPane.showInputDialog(
            null,
            "Select a ClipCraft profile:",
            "Switch Profile",
            JOptionPane.PLAIN_MESSAGE,
            null,
            profileNames.toTypedArray(),
            settings.state.activeProfileName
        ) as String?

        if (!selected.isNullOrEmpty()) {
            settings.setActiveProfile(selected)
            ClipCraftNotificationCenter.notifyInfo("Switched to profile: $selected", project)
        }
    }
}
