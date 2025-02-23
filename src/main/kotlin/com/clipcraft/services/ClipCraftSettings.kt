package com.clipcraft.services

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.ClipCraftProfile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Manages user profiles/settings without calling services in the constructor or static init.
 * Instead, the [ensureLoaded] method will fetch from [ClipCraftSettingsService] when needed.
 */
class ClipCraftSettings private constructor() {

    private val fallbackProfile = ClipCraftProfile("Global Default", ClipCraftOptions())
    private val allProfiles = mutableListOf<ClipCraftProfile>()
    private var currentProfileName: String = fallbackProfile.profileName

    // Once this flag is set, we won't reload from service again.
    @Volatile
    private var loaded: Boolean = false

    companion object {
        // Our singleton instance
        private val instance = ClipCraftSettings()

        @JvmStatic
        fun getInstance(): ClipCraftSettings = instance
    }

    /**
     * Ensures data is loaded from [ClipCraftSettingsService] exactly once.
     */
    private fun ensureLoaded() {
        if (!loaded) {
            loaded = true
            val svc = ClipCraftSettingsService.getInstance()
            val state = svc.getState()

            currentProfileName = state.activeProfileName
            if (state.profilesJson.isNotEmpty()) {
                runCatching {
                    val loadedProfiles = Json.decodeFromString<List<ClipCraftProfile>>(state.profilesJson)
                    allProfiles.clear()
                    allProfiles.addAll(loadedProfiles)
                }
            }
            // Ensure fallback profile is present if none loaded
            if (allProfiles.isEmpty()) {
                allProfiles += fallbackProfile
                currentProfileName = fallbackProfile.profileName
            }
        }
    }

    /**
     * Saves current settings to [ClipCraftSettingsService].
     */
    private fun saveToService() {
        val svc = ClipCraftSettingsService.getInstance()
        val state = svc.getState()

        // Persist the current active profile
        state.activeProfileName = currentProfileName

        // Serialize all profiles
        val profilesJson = Json.encodeToString(allProfiles)
        state.profilesJson = profilesJson

        svc.loadState(state)
        svc.persist()
    }

    /**
     * Toggles 'showLint' in the current profile.
     */
    fun toggleLint(): Boolean {
        ensureLoaded()
        val profile = getCurrentProfile()
        profile.options.showLint = !profile.options.showLint
        saveToService()
        return profile.options.showLint
    }

    /**
     * Cycles through concurrency modes in the current profile.
     */
    fun toggleConcurrency() {
        ensureLoaded()
        val profile = getCurrentProfile()
        profile.options.concurrencyMode = when (profile.options.concurrencyMode) {
            com.clipcraft.model.ConcurrencyMode.DISABLED -> com.clipcraft.model.ConcurrencyMode.THREAD_POOL
            com.clipcraft.model.ConcurrencyMode.THREAD_POOL -> com.clipcraft.model.ConcurrencyMode.COROUTINES
            com.clipcraft.model.ConcurrencyMode.COROUTINES -> com.clipcraft.model.ConcurrencyMode.DISABLED
        }
        saveToService()
    }

    fun getCurrentProfile(): ClipCraftProfile {
        ensureLoaded()
        return allProfiles.find { it.profileName == currentProfileName } ?: fallbackProfile
    }

    fun setCurrentProfile(profileName: String) {
        ensureLoaded()
        val profile = allProfiles.find { it.profileName == profileName }
        if (profile != null) {
            currentProfileName = profileName
        }
        saveToService()
    }

    fun getAllProfiles(): List<ClipCraftProfile> {
        ensureLoaded()
        return allProfiles.toList()
    }

    fun addProfile(profile: ClipCraftProfile) {
        ensureLoaded()
        val index = allProfiles.indexOfFirst { it.profileName == profile.profileName }
        if (index >= 0) {
            allProfiles[index] = profile
        } else {
            allProfiles += profile
        }

        // If we're still on the fallback profile, and there's a new profile, switch
        if (getCurrentProfile() == fallbackProfile && currentProfileName == fallbackProfile.profileName) {
            currentProfileName = profile.profileName
        }
        saveToService()
    }

    fun removeProfile(profileName: String) {
        ensureLoaded()
        // Don't remove fallback
        if (profileName == fallbackProfile.profileName) return
        val removed = allProfiles.removeIf { it.profileName == profileName }
        if (removed && currentProfileName == profileName) {
            currentProfileName = fallbackProfile.profileName
        }
        saveToService()
    }

    fun getSnippetPrefix(): String {
        ensureLoaded()
        return getCurrentProfile().options.snippetHeaderText.orEmpty()
    }

    fun getSnippetSuffix(): String {
        ensureLoaded()
        return getCurrentProfile().options.snippetFooterText.orEmpty()
    }
}
