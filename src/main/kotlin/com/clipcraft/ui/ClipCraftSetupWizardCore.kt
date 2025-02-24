package com.clipcraft.ui

import com.clipcraft.services.ClipCraftSettings
import com.intellij.openapi.project.Project

class ClipCraftSetupWizardCore(private val project: Project) {
    fun applyWizardResults(wizardUI: ClipCraftSetupWizardUI) {
        // Example: apply wizard settings to a default or current profile
        val settings = ClipCraftSettings.getInstance()
        val currentProfile = settings.getCurrentProfile()
        val newOptions = currentProfile.options.copy(
            includeMetadata = wizardUI.isIncludeMetadata(),
            useGitIgnore = wizardUI.isUseGitIgnore(),
            maxConcurrentTasks = wizardUI.getMaxConcurrentTasks(),
        )
        currentProfile.copy(options = newOptions)
        // store updatedProfile if using multiple profiles
        // or just apply it
    }
}
