package com.clipcraft.ui

import com.clipcraft.services.ClipCraftSettings
import com.intellij.openapi.project.Project

class ClipCraftSetupWizardCore(private val project: Project) {
    fun applyWizardResults(wizardUI: ClipCraftSetupWizardUI) {
        val settings = ClipCraftSettings.getInstance()
        val currentProfile = settings.getCurrentProfile()
        currentProfile.options.copy(
            includeMetadata = wizardUI.isIncludeMetadata(),
            useGitIgnore = wizardUI.isUseGitIgnore(),
            maxConcurrentTasks = wizardUI.getMaxConcurrentTasks()
        )
        // Typically you’d reassign the profile with updated options or store them:
        // currentProfile.options = newOptions
        // Or do it more explicitly if you have an active manager:
        // ...
    }
}
