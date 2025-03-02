package com.clipcraft.services

import com.clipcraft.model.ClipCraftProfile
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class ClipCraftProjectProfileManager(private val project: Project) {
    private val profiles = mutableMapOf<String, ClipCraftProfile>()
    private var activeProfile: ClipCraftProfile? = null

    fun getProfile(name: String): ClipCraftProfile? = profiles[name]

    fun addOrUpdateProfile(profile: ClipCraftProfile) {
        profiles[profile.profileName] = profile
        if (activeProfile == null) activeProfile = profile
    }

    fun switchActiveProfile(name: String) {
        activeProfile = profiles[name]
    }

    fun getActiveProfile(): ClipCraftProfile? = activeProfile
    fun listProfiles(): List<ClipCraftProfile> = profiles.values.toList()
}
