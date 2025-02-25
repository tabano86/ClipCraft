package com.clipcraft.actions

import ClipCraftNotificationCenter
import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.ClipCraftProfile
import com.clipcraft.services.ClipCraftProjectProfileManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class ClipCraftResetDefaultsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val manager = project.getService(ClipCraftProjectProfileManager::class.java) ?: return
        val defaultProfile = ClipCraftProfile("Default", ClipCraftOptions())
        manager.addOrUpdateProfile(defaultProfile)
        manager.switchActiveProfile("Default")
        ClipCraftNotificationCenter.info("ClipCraft settings reset to defaults.")
    }
}
