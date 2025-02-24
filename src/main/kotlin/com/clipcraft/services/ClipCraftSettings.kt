package com.clipcraft.services

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.ClipCraftProfile
import com.intellij.openapi.diagnostic.Logger

/**
 * Higher-level settings facade.
 * Loads from ClipCraftSettingsState and may hold logic for multiple profiles, snippet prefix, suffix, etc.
 */
class ClipCraftSettings private constructor() {

    private val logger = Logger.getInstance(ClipCraftSettings::class.java)

    // The fallback or global default profile
    private val fallbackProfile = ClipCraftProfile("Global Default", ClipCraftOptions())
    private val allProfiles = mutableListOf<ClipCraftProfile>()
    private var currentProfileName: String = fallbackProfile.profileName

    // For watchers
    private val listeners = mutableSetOf<SettingsChangeListener>()

    companion object {
        @Volatile
        private var instance: ClipCraftSettings? = null
        fun getInstance(): ClipCraftSettings {
            return instance ?: synchronized(this) {
                instance ?: ClipCraftSettings().also { instance = it }
            }
        }
    }

    init {
        // Load at startup
        if (allProfiles.isEmpty()) {
            allProfiles.add(fallbackProfile)
            currentProfileName = fallbackProfile.profileName
        }
    }

    // Example usage if implementing multi-profile approach
    fun getCurrentProfile(): ClipCraftProfile {
        return allProfiles.find { it.profileName == currentProfileName } ?: fallbackProfile
    }

    fun setCurrentProfile(name: String) {
        if (allProfiles.any { it.profileName == name }) {
            currentProfileName = name
            notifyListeners()
        } else {
            logger.warn("Profile not found: $name")
        }
    }

    fun getSnippetPrefix(): String {
        return getCurrentProfile().options.snippetHeaderText.orEmpty()
    }

    fun getSnippetSuffix(): String {
        return getCurrentProfile().options.snippetFooterText.orEmpty()
    }

    // region Observer mechanism
    fun addSettingsChangeListener(listener: SettingsChangeListener) {
        listeners.add(listener)
    }

    fun removeSettingsChangeListener(listener: SettingsChangeListener) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        for (l in listeners) {
            try {
                l.onSettingsChanged(this)
            } catch (ex: Exception) {
                logger.warn("Error notifying listener: ${ex.message}", ex)
            }
        }
    }
    // endregion
}

interface SettingsChangeListener {
    fun onSettingsChanged(settings: ClipCraftSettings)
}
