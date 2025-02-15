package com.clipcraft.services

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.ClipCraftProfile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
        // NEW: After load, we also read from ClipCraftSettingsService
        loadFromService()
    }

    // NEW: Convenience toggles (to reduce code for the user)
    fun toggleLint(): Boolean {
        val profile = getCurrentProfile()
        profile.options.showLint = !profile.options.showLint
        saveToService()
        return profile.options.showLint
    }

    fun toggleConcurrency() {
        val profile = getCurrentProfile()
        profile.options.concurrencyMode = when (profile.options.concurrencyMode) {
            com.clipcraft.model.ConcurrencyMode.DISABLED -> com.clipcraft.model.ConcurrencyMode.THREAD_POOL
            com.clipcraft.model.ConcurrencyMode.THREAD_POOL -> com.clipcraft.model.ConcurrencyMode.COROUTINES
            com.clipcraft.model.ConcurrencyMode.COROUTINES -> com.clipcraft.model.ConcurrencyMode.DISABLED
        }
        saveToService()
    }

    // For retrieving the current profile
    fun getCurrentProfile(): ClipCraftProfile {
        return allProfiles.find { it.profileName == currentProfileName } ?: fallbackProfile
    }

    fun setCurrentProfile(profileName: String) {
        val profile = allProfiles.find { it.profileName == profileName }
        if (profile != null) {
            currentProfileName = profileName
        }
        saveToService()
    }

    fun getAllProfiles(): List<ClipCraftProfile> = allProfiles.toList()

    fun addProfile(profile: ClipCraftProfile) {
        val index = allProfiles.indexOfFirst { it.profileName == profile.profileName }
        if (index >= 0) {
            allProfiles[index] = profile
        } else {
            allProfiles += profile
        }
        // If we were still using fallback, switch to the new one
        if (getCurrentProfile() == fallbackProfile && currentProfileName == fallbackProfile.profileName) {
            currentProfileName = profile.profileName
        }
        saveToService()
    }

    fun removeProfile(profileName: String) {
        if (profileName == fallbackProfile.profileName) return
        val removed = allProfiles.removeIf { it.profileName == profileName }
        if (removed && currentProfileName == profileName) {
            currentProfileName = fallbackProfile.profileName
        }
        saveToService()
    }

    // These provide snippet prefix/suffix
    fun getSnippetPrefix(): String {
        return getCurrentProfile().options.snippetHeaderText ?: "/* Default Header */"
    }

    fun getSnippetSuffix(): String {
        return getCurrentProfile().options.snippetFooterText ?: "/* Default Footer */"
    }

    // NEW: Save to persistent service
    private fun saveToService() {
        val svc = ClipCraftSettingsService.getInstance()
        val state = svc.getState()
        state.activeProfileName = currentProfileName
        // We'll convert all profiles to JSON
        val profilesJson = Json.encodeToString(allProfiles)
        state.profilesJson = profilesJson
        svc.loadState(state) // update in memory
        // trigger store
        svc.persist()
    }

    // NEW: Load from persistent service
    private fun loadFromService() {
        val svc = ClipCraftSettingsService.getInstance()
        val state = svc.getState()
        currentProfileName = state.activeProfileName
        if (state.profilesJson.isNotEmpty()) {
            runCatching {
                val loaded = Json.decodeFromString<List<ClipCraftProfile>>(state.profilesJson)
                allProfiles.clear()
                allProfiles.addAll(loaded)
            }
        }
    }
}
