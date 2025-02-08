package com.clipcraft.actions

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.services.ClipCraftSettings
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class ClipCraftResetDefaultsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        // 1. Get the settings service instance
        val settings = ClipCraftSettings.getInstance()
        // 2. Retrieve the current state (which includes active profile name and all profiles)
        val currentState = settings.state  // currentState is of type ClipCraftSettings.State
        val currentProfileName = currentState.activeProfileName

        // 3. Copy existing profiles to avoid modifying the original state directly
        val updatedProfiles = HashMap(currentState.profiles)  // copy of profiles map

        // 4. Reset the active profile's settings to default values
        updatedProfiles[currentProfileName] = ClipCraftOptions()
        // (ClipCraftOptions() should initialize with default settings for a profile)

        // 5. Create a new state with the same active profile name and updated profiles map
        val newState = ClipCraftSettings.State().apply {
            activeProfileName = currentProfileName      // preserve active profile name
            profiles = updatedProfiles                  // preserve all profiles, with current one reset
        }

        // 6. Load the new state into the settings service (persisting the changes)
        settings.loadState(newState)
    }
}
