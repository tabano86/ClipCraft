package com.clipcraft.services

import com.clipcraft.model.ClipCraftProfile
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

/**
 * Similar to ClipCraftProfileManager but at the project level if desired.
 * By default, delegates to the global settings. You can store project-specific
 * overrides if your design requires it.
 */
@Service(Service.Level.PROJECT)
class ClipCraftProjectProfileManager(private val project: Project) {

    fun getProfile(name: String): ClipCraftProfile? {
        return ClipCraftProfileManager().getProfile(name)
    }

    fun addOrUpdateProfile(profile: ClipCraftProfile) {
        ClipCraftProfileManager().addProfile(profile)
    }

    fun switchActiveProfile(name: String) {
        ClipCraftProfileManager().switchProfile(name)
    }

    fun getProfiles(): List<ClipCraftProfile> {
        return ClipCraftProfileManager().listProfiles()
    }

    fun getActiveProfile(): ClipCraftProfile {
        return ClipCraftProfileManager().currentProfile()
    }
}
