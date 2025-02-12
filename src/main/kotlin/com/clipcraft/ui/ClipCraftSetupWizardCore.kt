package com.clipcraft.ui

import com.clipcraft.model.ConcurrencyMode
import com.clipcraft.services.ClipCraftProjectProfileManager
import com.intellij.openapi.project.Project

class ClipCraftSetupWizardCore(private val project: Project) {
    fun applyWizardResults(wizardUI: ClipCraftSetupWizardUI) {
        val mgr = project.getService(ClipCraftProjectProfileManager::class.java) ?: return
        val current = mgr.getProfile("Default") ?: return
        val updatedOptions = current.options.copy(
            includeMetadata = wizardUI.isIncludeMetadata(),
            useGitIgnore = wizardUI.isUseGitIgnore(),
            maxConcurrentTasks = wizardUI.getMaxConcurrentTasks(),
            concurrencyMode = if (wizardUI.getMaxConcurrentTasks() > 1) ConcurrencyMode.THREAD_POOL else ConcurrencyMode.DISABLED
        )
        val updatedProfile = current.copy(options = updatedOptions)
        mgr.addOrUpdateProfile(updatedProfile)
        mgr.switchActiveProfile(updatedProfile.profileName)
    }
}
