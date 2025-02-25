package com.clipcraft.actions

import ClipCraftNotificationCenter
import com.clipcraft.services.ClipCraftProjectProfileManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

class ClipCraftSwitchProfileAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val mgr = project.getService(ClipCraftProjectProfileManager::class.java) ?: return
        val names = mgr.listProfiles().map { it.profileName }.toTypedArray()
        if (names.isEmpty()) {
            ClipCraftNotificationCenter.warn("No profiles available.")
            return
        }
        val chosen = Messages.showEditableChooseDialog(
            "Select a ClipCraft Profile:",
            "Switch ClipCraft Profile",
            Messages.getQuestionIcon(),
            names,
            names[0],
            null,
        )
        if (chosen != null) {
            mgr.switchActiveProfile(chosen)
            ClipCraftNotificationCenter.info("Switched to profile: $chosen")
        }
    }
}
