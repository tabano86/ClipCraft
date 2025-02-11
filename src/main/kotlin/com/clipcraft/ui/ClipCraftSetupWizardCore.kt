package com.clipcraft.ui

import com.clipcraft.services.ClipCraftProjectProfileManager
import com.intellij.openapi.project.Project

class ClipCraftSetupWizardCore(private val project: Project) {

    fun applyWizardResults(wizardUI: ClipCraftSetupWizardUI) {
        val manager = project.getService(ClipCraftProjectProfileManager::class.java) ?: return

        val currentProfile = manager.getProfile("Default") ?: return
        val updatedOptions = currentProfile.options.copy(
            includeMetadata = wizardUI.isIncludeMetadata(),
            useGitIgnore = wizardUI.isUseGitIgnore(),
            maxConcurrentTasks = wizardUI.getMaxConcurrentTasks(),
            concurrencyEnabled = true
        )

        val updatedProfile = currentProfile.copy(options = updatedOptions)
        manager.addOrUpdateProfile(updatedProfile)
        manager.switchActiveProfile(updatedProfile.profileName)
    }
}
