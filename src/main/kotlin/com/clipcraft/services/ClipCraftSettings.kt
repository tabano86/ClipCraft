package com.clipcraft.services

import com.clipcraft.model.ClipCraftOptions
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/**
 * Global app-level settings for ClipCraft.
 * Now supports multiple named profiles via an internal map.
 */

@State(name = "ClipCraftSettings", storages = [Storage("ClipCraftSettings.xml")])
@Service(Service.Level.APP)
class ClipCraftSettings : PersistentStateComponent<ClipCraftSettings.State> {

    data class State(
        var activeProfileName: String = "Default",
        var profiles: MutableMap<String, ClipCraftOptions> = mutableMapOf()
    )


    private var myState: State = State(
        activeProfileName = "Default",
        profiles = mutableMapOf("Default" to ClipCraftOptions())  // initialize default profile
    )

    override fun getState(): State {
        return myState
    }

    override fun loadState(state: State) {
        myState = state
    }

    /**
     * Returns the currently active profile's ClipCraftOptions.
     */
    fun getActiveOptions(): ClipCraftOptions {
        return myState.profiles[myState.activeProfileName] ?: ClipCraftOptions()
    }

    /**
     * Saves (adds or updates) a profile by name.
     */
    fun saveProfile(name: String, options: ClipCraftOptions) {
        myState.profiles[name] = options
    }

    /**
     * Deletes a profile by name, if it exists (cannot delete the active profile).
     */
    fun deleteProfile(name: String) {
        if (name == myState.activeProfileName) return
        myState.profiles.remove(name)
    }

    /**
     * Sets the active profile. Does nothing if profile name doesn't exist.
     */
    fun setActiveProfile(name: String) {
        if (myState.profiles.containsKey(name)) {
            myState.activeProfileName = name
        }
    }

    /**
     * Returns all known profile names.
     */
    fun listProfileNames(): List<String> {
        return myState.profiles.keys.toList()
    }

    companion object {
        /** Gets the singleton instance of ClipCraftSettings */
        fun getInstance(): ClipCraftSettings =
            ApplicationManager
                .getApplication().getService(ClipCraftSettings::class.java)
    }
}
