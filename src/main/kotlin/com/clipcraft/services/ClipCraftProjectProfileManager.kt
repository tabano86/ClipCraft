package com.clipcraft.services

import com.clipcraft.model.ClipCraftProfile
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

/**
 * Manages per-project ClipCraft profiles (in-memory only).
 */
@Service(Service.Level.PROJECT)
class ClipCraftProjectProfileManager(val project: Project) {

    private val profiles: MutableMap<String, ClipCraftProfile> = mutableMapOf()
    private var activeProfile: ClipCraftProfile? = null

    fun getProfile(name: String): ClipCraftProfile? = profiles[name]

    fun addOrUpdateProfile(profile: ClipCraftProfile) {
        profiles[profile.profileName] = profile
        if (activeProfile == null) {
            activeProfile = profile
        }
    }

    fun switchActiveProfile(profileName: String) {
        activeProfile = profiles[profileName]
    }

    fun getActiveProfile(): ClipCraftProfile? = activeProfile

    fun listProfiles(): List<ClipCraftProfile> = profiles.values.toList()
}
