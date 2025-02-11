package com.clipcraft.ui

import com.clipcraft.services.ClipCraftProjectProfileManager
import com.intellij.openapi.project.Project

class ClipCraftSetupWizardCore(private val project: Project) {

    fun applyWizardResults(wizardUI: ClipCraftSetupWizardUI) {
        // Get the project-level manager
        val manager = project.getService(ClipCraftProjectProfileManager::class.java) ?: return

        // Retrieve a profile named "Default"
        val currentProfile = manager.getProfile("Default") ?: return

        // Build new options based on wizard UI
        val updatedOptions = currentProfile.options.copy(
            includeMetadata = wizardUI.isIncludeMetadata(),
            useGitIgnore = wizardUI.isUseGitIgnore(),
            maxConcurrentTasks = wizardUI.getMaxConcurrentTasks(),
            concurrencyEnabled = true
        )

        // Create an updated profile
        val updatedProfile = currentProfile.copy(options = updatedOptions)

        // Update storage and switch to the updated profile
        manager.addOrUpdateProfile(updatedProfile)
        manager.switchActiveProfile(updatedProfile.profileName)
    }
}
