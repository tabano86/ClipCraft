package com.clipcraft.services

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.ClipCraftProfile

/**
 * Global, application-level settings for ClipCraft.
 *
 * In this example, profiles are stored in memory only (non-persistent).
 */
class ClipCraftSettings private constructor() {

    // Fallback profile if no project-specific profile exists.
    private val fallbackProfile = ClipCraftProfile(
        profileName = "Global Default",
        options = ClipCraftOptions(),
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
        currentProfileName = fallbackProfile.profileName
    }

    fun getCurrentProfile(): ClipCraftProfile {
        return allProfiles.find { it.profileName == currentProfileName } ?: fallbackProfile
    }

    fun setCurrentProfile(profileName: String) {
        val profile = allProfiles.find { it.profileName == profileName }
        if (profile != null) {
            currentProfileName = profileName
        }
    }

    fun getAllProfiles(): List<ClipCraftProfile> = allProfiles.toList()

    fun addProfile(profile: ClipCraftProfile) {
        val index = allProfiles.indexOfFirst { it.profileName == profile.profileName }
        if (index >= 0) {
            allProfiles[index] = profile
        } else {
            allProfiles += profile
        }
        if (getCurrentProfile() == fallbackProfile && currentProfileName == fallbackProfile.profileName) {
            currentProfileName = profile.profileName
        }
    }

    fun removeProfile(profileName: String) {
        // Don't remove fallback
        if (profileName == fallbackProfile.profileName) return
        val removed = allProfiles.removeIf { it.profileName == profileName }
        if (removed && currentProfileName == profileName) {
            currentProfileName = fallbackProfile.profileName
        }
    }

    fun getHeader(): String {
        return getCurrentProfile().options.gptHeaderText ?: "/* Default Header */"
    }

    fun getFooter(): String {
        return getCurrentProfile().options.gptFooterText ?: "/* Default Footer */"
    }
}
