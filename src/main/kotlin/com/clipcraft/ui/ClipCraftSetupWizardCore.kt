package com.clipcraft.ui

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.ClipCraftProfile
import com.clipcraft.model.ConcurrencyMode
import com.intellij.openapi.project.Project

class ClipCraftSetupWizardCore(private val project: Project) {
    fun applyWizardResults(wizardUI: ClipCraftSetupWizardUI) {
        val settings = com.clipcraft.services.ClipCraftSettings.getInstance()
        val current: ClipCraftProfile = settings.getCurrentProfile()
        val updatedOptions: ClipCraftOptions = current.options.copy(
            includeMetadata = wizardUI.isIncludeMetadata(),
            useGitIgnore = wizardUI.isUseGitIgnore(),
            maxConcurrentTasks = wizardUI.getMaxConcurrentTasks(),
            concurrencyMode = if (wizardUI.getMaxConcurrentTasks() > 1) ConcurrencyMode.THREAD_POOL else ConcurrencyMode.DISABLED,
        )
        val updatedProfile = current.copy(options = updatedOptions)
        settings.addProfile(updatedProfile)
        settings.setCurrentProfile(updatedProfile.profileName)
    }
}
