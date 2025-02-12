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
            concurrencyMode = if (wizardUI.getMaxConcurrentTasks() > 1) {
                com.clipcraft.model.ConcurrencyMode.THREAD_POOL
            } else {
                com.clipcraft.model.ConcurrencyMode.DISABLED
            },
        )
        val updatedProfile = currentProfile.copy(options = updatedOptions)

        manager.addOrUpdateProfile(updatedProfile)
        manager.switchActiveProfile(updatedProfile.profileName)
    }
}
