package com.clipcraft.actions

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.services.ClipCraftSettings
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class ClipCraftResetDefaultsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val settings = ClipCraftSettings.getInstance()
        val currentState = settings.state
        val currentProfileName = currentState.activeProfileName
        val updatedProfiles = HashMap(currentState.profiles)
        updatedProfiles[currentProfileName] = ClipCraftOptions()
        val newState = ClipCraftSettings.State().apply {
            activeProfileName = currentProfileName
            profiles = updatedProfiles
        }
        settings.loadState(newState)
    }
}
