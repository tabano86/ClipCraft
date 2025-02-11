package com.clipcraft.services

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.ClipCraftProfile

/**
 * Global, application-level settings for ClipCraft.
 *
 * In this example, profiles are stored in memory only.
 * Adjust for persistence if needed.
 */
class ClipCraftSettings private constructor() {

    // Fallback profile if no project-specific profile exists.
    // We'll include it by default in our allProfiles list.
    private val fallbackProfile = ClipCraftProfile(
        profileName = "Global Default",
        options = ClipCraftOptions()
    )

    // Holds all known profiles in memory.
    private val allProfiles = mutableListOf<ClipCraftProfile>()

    // Tracks the currently selected profile name.
    private var currentProfileName: String

    companion object {
        private val instance = ClipCraftSettings()
        @JvmStatic
        fun getInstance(): ClipCraftSettings = instance
    }

    init {
        // Initialize with the fallback profile in the list
        allProfiles += fallbackProfile
        // Set default active profile name to fallbackProfile
        currentProfileName = fallbackProfile.profileName
    }

    /**
     * Returns the currently active global profile.
     */
    fun getCurrentProfile(): ClipCraftProfile {
        return allProfiles.find { it.profileName == currentProfileName } ?: fallbackProfile
    }

    /**
     * Switches the current profile to the one with the given name.
     * If not found, remains on the existing profile.
     */
    fun setCurrentProfile(profileName: String) {
        val profile = allProfiles.find { it.profileName == profileName }
        if (profile != null) {
            currentProfileName = profileName
        }
    }

    /**
     * Returns a snapshot list of all known profiles.
     */
    fun getAllProfiles(): List<ClipCraftProfile> = allProfiles.toList()

    /**
     * Adds a new profile or updates an existing one (by matching profileName).
     */
    fun addProfile(profile: ClipCraftProfile) {
        // If there's already a profile with the same name, replace it.
        val index = allProfiles.indexOfFirst { it.profileName == profile.profileName }
        if (index >= 0) {
            allProfiles[index] = profile
        } else {
            allProfiles += profile
        }
        // Optionally auto-switch to the newly added profile if none is active
        if (getCurrentProfile() == fallbackProfile && currentProfileName == fallbackProfile.profileName) {
            currentProfileName = profile.profileName
        }
    }

    /**
     * Removes a profile by name (if it exists).
     * If the removed profile was active, reverts to the fallback profile.
     */
    fun removeProfile(profileName: String) {
        // Only remove if it's not the fallback.
        if (profileName == fallbackProfile.profileName) return

        val removed = allProfiles.removeIf { it.profileName == profileName }
        if (removed && currentProfileName == profileName) {
            currentProfileName = fallbackProfile.profileName
        }
    }

    /**
     * Returns the user-defined header text (example usage).
     */
    fun getHeader(): String {
        return getCurrentProfile().options.gptHeaderText ?: "/* Default Header */"
    }

    /**
     * Returns the user-defined footer text (example usage).
     */
    fun getFooter(): String {
        return getCurrentProfile().options.gptFooterText ?: "/* Default Footer */"
    }
}
