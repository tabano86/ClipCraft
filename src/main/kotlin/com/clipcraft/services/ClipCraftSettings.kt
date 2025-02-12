package com.clipcraft.services

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.ClipCraftProfile

/**
 * Global, application-level settings for ClipCraft (non-persistent in this example).
 */
class ClipCraftSettings private constructor() {

    private val fallbackProfile = ClipCraftProfile("Global Default", ClipCraftOptions())
    private val allProfiles = mutableListOf<ClipCraftProfile>()
    private var currentProfileName: String

    companion object {
        private val instance = ClipCraftSettings()
        @JvmStatic
        fun getInstance(): ClipCraftSettings = instance
    }

    init {
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
        // If we were on fallback, switch to newly added profile
        if (getCurrentProfile() == fallbackProfile && currentProfileName == fallbackProfile.profileName) {
            currentProfileName = profile.profileName
        }
    }

    fun removeProfile(profileName: String) {
        if (profileName == fallbackProfile.profileName) return
        val removed = allProfiles.removeIf { it.profileName == profileName }
        if (removed && currentProfileName == profileName) {
            currentProfileName = fallbackProfile.profileName
        }
    }

    // Formerly "getHeader/getFooter" with GPT references, now generic:
    fun getSnippetPrefix(): String {
        return getCurrentProfile().options.snippetHeaderText ?: "/* Default Header */"
    }

    fun getSnippetSuffix(): String {
        return getCurrentProfile().options.snippetFooterText ?: "/* Default Footer */"
    }
}
