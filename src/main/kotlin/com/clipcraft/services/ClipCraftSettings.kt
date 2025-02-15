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
        loadFromService()
    }
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
    fun getSnippetPrefix(): String {
        return getCurrentProfile().options.snippetHeaderText ?: ""
    }
    fun getSnippetSuffix(): String {
        return getCurrentProfile().options.snippetFooterText ?: ""
    }
    private fun saveToService() {
        val svc = ClipCraftSettingsService.getInstance()
        val state = svc.getState()
        state.activeProfileName = currentProfileName
        val profilesJson = Json.encodeToString(allProfiles)
        state.profilesJson = profilesJson
        svc.loadState(state)
        svc.persist()
    }
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
