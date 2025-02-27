package com.clipcraft.services

import com.intellij.openapi.diagnostic.Logger

class ClipCraftSettings private constructor() {
    private val logger = Logger.getInstance(ClipCraftSettings::class.java)
    private val fallbackProfile =
        com.clipcraft.model.ClipCraftProfile("Global Default", com.clipcraft.model.ClipCraftOptions())
    private val allProfiles = mutableListOf<com.clipcraft.model.ClipCraftProfile>()
    private var currentProfileName: String = fallbackProfile.profileName
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
        if (allProfiles.isEmpty()) {
            allProfiles.add(fallbackProfile)
            currentProfileName = fallbackProfile.profileName
        }
    }

    fun getCurrentProfile(): com.clipcraft.model.ClipCraftProfile {
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

    fun getSnippetPrefix(): String = getCurrentProfile().options.snippetHeaderText.orEmpty()
    fun getSnippetSuffix(): String = getCurrentProfile().options.snippetFooterText.orEmpty()
    fun addSettingsChangeListener(listener: SettingsChangeListener) {
        listeners.add(listener)
    }

    fun removeSettingsChangeListener(listener: SettingsChangeListener) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        listeners.forEach {
            try {
                it.onSettingsChanged(this)
            } catch (ex: Exception) {
                logger.warn("Error notifying listener: ${ex.message}", ex)
            }
        }
    }

    interface SettingsChangeListener {
        fun onSettingsChanged(settings: ClipCraftSettings)
    }
}
