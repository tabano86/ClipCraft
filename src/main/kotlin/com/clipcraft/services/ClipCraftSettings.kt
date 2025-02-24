package com.clipcraft.services

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.ClipCraftProfile
import com.clipcraft.model.ConcurrencyMode
import java.util.logging.Logger
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface SettingsChangeListener {
    fun onSettingsChanged(settings: ClipCraftSettings)
}

class ClipCraftSettings private constructor() {

    private val lock = Any()
    private val logger = Logger.getLogger(ClipCraftSettings::class.java.name)

    private val fallbackProfile = ClipCraftProfile("Global Default", ClipCraftOptions())
    private val allProfiles = mutableListOf<ClipCraftProfile>()
    private var currentProfileName: String = fallbackProfile.profileName

    @Volatile
    private var loaded: Boolean = false
    private val listeners = mutableSetOf<SettingsChangeListener>()

    companion object {
        private val instance = ClipCraftSettings()

        @JvmStatic
        fun getInstance(): ClipCraftSettings = instance
    }

    private fun ensureLoaded() {
        if (!loaded) {
            synchronized(lock) {
                if (!loaded) {
                    loaded = true
                    val svc = ClipCraftSettingsService.getInstance()
                    val state = svc.getState()
                    currentProfileName = state.activeProfileName
                    if (state.profilesJson.isNotEmpty()) {
                        try {
                            val loadedProfiles = Json.decodeFromString<List<ClipCraftProfile>>(state.profilesJson)
                            allProfiles.clear()
                            allProfiles.addAll(loadedProfiles)
                        } catch (ex: Exception) {
                            logger.warning("Failed to decode profiles JSON: ${ex.message}")
                        }
                    }
                    if (allProfiles.isEmpty()) {
                        allProfiles.add(fallbackProfile)
                        currentProfileName = fallbackProfile.profileName
                    }
                }
            }
        }
    }

    private fun saveToService() {
        synchronized(lock) {
            val svc = ClipCraftSettingsService.getInstance()
            val state = svc.getState()
            state.activeProfileName = currentProfileName
            state.profilesJson = Json.encodeToString(allProfiles)
            svc.loadState(state)
            svc.persist()
        }
        notifyListeners()
    }

    private fun notifyListeners() {
        synchronized(lock) {
            listeners.forEach { listener ->
                try {
                    listener.onSettingsChanged(this)
                } catch (ex: Exception) {
                    logger.warning("Error notifying listener: ${ex.message}")
                }
            }
        }
    }

    // Forces a reload from the underlying service.
    fun refresh() {
        synchronized(lock) {
            loaded = false
            ensureLoaded()
        }
    }

    fun addSettingsChangeListener(listener: SettingsChangeListener) {
        synchronized(lock) {
            listeners.add(listener)
        }
    }

    fun removeSettingsChangeListener(listener: SettingsChangeListener) {
        synchronized(lock) {
            listeners.remove(listener)
        }
    }

    fun toggleLint(): Boolean {
        ensureLoaded()
        synchronized(lock) {
            val profile = getCurrentProfileInternal()
            profile.options.showLint = !profile.options.showLint
            saveToService()
            return profile.options.showLint
        }
    }

    fun toggleConcurrency() {
        ensureLoaded()
        synchronized(lock) {
            val profile = getCurrentProfileInternal()
            profile.options.concurrencyMode = when (profile.options.concurrencyMode) {
                ConcurrencyMode.DISABLED -> ConcurrencyMode.THREAD_POOL
                ConcurrencyMode.THREAD_POOL -> ConcurrencyMode.COROUTINES
                ConcurrencyMode.COROUTINES -> ConcurrencyMode.DISABLED
            }
            saveToService()
        }
    }

    // Allows atomic updates to the current profile's options.
    fun updateCurrentProfileOptions(update: (ClipCraftOptions) -> Unit) {
        ensureLoaded()
        synchronized(lock) {
            val profile = getCurrentProfileInternal()
            update(profile.options)
            saveToService()
        }
    }

    // Renames an existing profile if no duplicate exists.
    fun renameProfile(oldName: String, newName: String): Boolean {
        ensureLoaded()
        synchronized(lock) {
            val profile = allProfiles.find { it.profileName == oldName } ?: return false
            if (allProfiles.any { it.profileName == newName }) return false
            profile.profileName = newName // Assumes profileName is mutable.
            if (currentProfileName == oldName) {
                currentProfileName = newName
            }
            saveToService()
            return true
        }
    }

    // Returns a copy of the current profile.
    fun getCurrentProfile(): ClipCraftProfile {
        ensureLoaded()
        synchronized(lock) {
            return getCurrentProfileInternal().copy()
        }
    }

    private fun getCurrentProfileInternal(): ClipCraftProfile {
        return allProfiles.find { it.profileName == currentProfileName } ?: fallbackProfile
    }

    fun setCurrentProfile(profileName: String) {
        ensureLoaded()
        synchronized(lock) {
            if (allProfiles.any { it.profileName == profileName }) {
                currentProfileName = profileName
                saveToService()
            } else {
                logger.warning("Profile not found: $profileName")
            }
        }
    }

    // Returns copies of all profiles.
    fun getAllProfiles(): List<ClipCraftProfile> {
        ensureLoaded()
        synchronized(lock) {
            return allProfiles.map { it.copy() }
        }
    }

    fun addProfile(profile: ClipCraftProfile) {
        ensureLoaded()
        synchronized(lock) {
            val index = allProfiles.indexOfFirst { it.profileName == profile.profileName }
            if (index >= 0) {
                allProfiles[index] = profile
            } else {
                allProfiles.add(profile)
            }
            if (getCurrentProfileInternal() == fallbackProfile && currentProfileName == fallbackProfile.profileName) {
                currentProfileName = profile.profileName
            }
            saveToService()
        }
    }

    fun removeProfile(profileName: String) {
        ensureLoaded()
        synchronized(lock) {
            if (profileName == fallbackProfile.profileName) return
            val removed = allProfiles.removeIf { it.profileName == profileName }
            if (removed && currentProfileName == profileName) {
                currentProfileName = fallbackProfile.profileName
            }
            saveToService()
        }
    }

    fun updateSnippetHeaderText(newHeader: String) {
        updateCurrentProfileOptions { it.snippetHeaderText = newHeader }
    }

    fun updateSnippetFooterText(newFooter: String) {
        updateCurrentProfileOptions { it.snippetFooterText = newFooter }
    }

    fun getSnippetPrefix(): String {
        ensureLoaded()
        synchronized(lock) {
            return getCurrentProfileInternal().options.snippetHeaderText.orEmpty()
        }
    }

    fun getSnippetSuffix(): String {
        ensureLoaded()
        synchronized(lock) {
            return getCurrentProfileInternal().options.snippetFooterText.orEmpty()
        }
    }
}
