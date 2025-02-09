package com.clipcraft.services

import com.clipcraft.model.ClipCraftOptions
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*

@State(name = "ClipCraftSettings", storages = [Storage("ClipCraftSettings.xml")])
@Service(Service.Level.APP)
class ClipCraftSettings : PersistentStateComponent<ClipCraftSettings.State> {

    data class State(
        var activeProfileName: String = "Default",
        var profiles: MutableMap<String, ClipCraftOptions> = mutableMapOf("Default" to ClipCraftOptions())
    )

    private var myState: State = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    fun getActiveOptions(): ClipCraftOptions =
        myState.profiles[myState.activeProfileName] ?: ClipCraftOptions()

    fun saveProfile(name: String, options: ClipCraftOptions) {
        myState.profiles[name] = options
    }

    fun deleteProfile(name: String) {
        if (name == myState.activeProfileName) return
        myState.profiles.remove(name)
    }

    fun setActiveProfile(name: String) {
        if (myState.profiles.containsKey(name)) {
            myState.activeProfileName = name
        }
    }

    fun listProfileNames(): List<String> = myState.profiles.keys.toList()

    companion object {
        fun getInstance(): ClipCraftSettings =
            ApplicationManager.getApplication().getService(ClipCraftSettings::class.java)
    }
}
