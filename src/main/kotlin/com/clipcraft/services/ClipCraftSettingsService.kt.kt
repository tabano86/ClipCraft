package com.clipcraft.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/**
 * Defines a single profile of ClipCraft settings (formatting, chunking, etc.).
 */
data class ClipCraftProfile(
    val name: String = "Default", // Added property to uniquely identify the profile
    val language: String = "",    // e.g. "JAVA", "KOTLIN", etc.
    val removeImports: Boolean = false,
    val removePackageDecl: Boolean = false,
    val removeComments: Boolean = false,
    val removeEmptyLines: Boolean = false,
    val customRegex: String = "",
    val includeMetadata: Boolean = true,
    val chunkSize: Int = 4000,
    val chunkMinMerge: Int = 3000,
    val outputFormat: String = "MARKDOWN"
)

/**
 * Application-level service: globally stores profiles, active profile, GPT API key, etc.
 */
@State(name = "ClipCraftSettingsService", storages = [Storage("ClipCraftSettings.xml")])
@Service(Service.Level.APP)
class ClipCraftSettingsService : PersistentStateComponent<ClipCraftSettingsService.State> {

    class State {
        var activeProfileName: String = "Default"
        var profiles: MutableList<ClipCraftProfile> = mutableListOf(ClipCraftProfile("Default"))
        var gptApiKey: String? = null
    }

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    // Retrieves the currently active profile
    fun getActiveProfile(): ClipCraftProfile {
        return myState.profiles.find { it.name == myState.activeProfileName }
            ?: myState.profiles.first() // fallback if active profile is not found
    }

    // Change the active profile
    fun setActiveProfile(name: String) {
        if (myState.profiles.any { it.name == name }) {
            myState.activeProfileName = name
        }
    }

    // List available profile names
    fun listProfileNames(): List<String> = myState.profiles.map { it.name }

    // Add or override a profile
    fun addProfile(profile: ClipCraftProfile) {
        myState.profiles.removeIf { it.name == profile.name }
        myState.profiles.add(profile)
    }

    // Stub for validating GPT key
    fun testGptConnection(): Boolean {
        // In a real plugin, you'd make an API call to verify the key
        return myState.gptApiKey?.isNotEmpty() == true
    }

    companion object {
        fun getInstance(): ClipCraftSettingsService {
            return ApplicationManager.getApplication().getService(ClipCraftSettingsService::class.java)
        }
    }
}
