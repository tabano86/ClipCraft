package com.clipcraft.services

import com.clipcraft.model.ClipCraftProfile
import com.intellij.openapi.components.Service

/**
 * Manages per-project ClipCraft profiles.
 */
@Service(Service.Level.PROJECT)
class ClipCraftProjectProfileManager {

    private val profiles: MutableMap<String, ClipCraftProfile> = mutableMapOf()
    private var activeProfile: ClipCraftProfile? = null

    /**
     * Retrieves a profile by name, or null if none is found.
     */
    fun getProfile(name: String): ClipCraftProfile? = profiles[name]

    /**
     * Adds or updates an existing profile.
     */
    fun addOrUpdateProfile(profile: ClipCraftProfile) {
        profiles[profile.profileName] = profile
        // If there's no active profile yet, set it to the newly added one
        if (activeProfile == null) {
            activeProfile = profile
        }
    }

    /**
     * Switches the active profile to the given name, if present.
     */
    fun switchActiveProfile(profileName: String) {
        activeProfile = profiles[profileName]
    }

    /**
     * Retrieves the currently active profile.
     */
    fun getActiveProfile(): ClipCraftProfile? = activeProfile

    /**
     * Returns a list of all available profiles.
     */
    fun listProfiles(): List<ClipCraftProfile> = profiles.values.toList()
}
