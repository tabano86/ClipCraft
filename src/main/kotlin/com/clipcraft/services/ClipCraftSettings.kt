package com.clipcraft.services

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.ClipCraftProfile
import com.intellij.openapi.components.*
import com.intellij.openapi.diagnostic.Logger

@State(name = "ClipCraftSettings", storages = [Storage("clipcraft.xml")])
@Service(Service.Level.APP)
class ClipCraftSettings : PersistentStateComponent<ClipCraftSettings.State> {

    companion object {
        fun getInstance(): ClipCraftSettings = service()
        private val logger = Logger.getInstance(ClipCraftSettings::class.java)
    }

    class State {
        var currentProfileName: String = "Default"
        var allProfiles: MutableList<ClipCraftProfile> = mutableListOf(
            ClipCraftProfile("Default", ClipCraftOptions())
        )
    }

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    fun getCurrentProfile(): ClipCraftProfile {
        return myState.allProfiles.find { it.profileName == myState.currentProfileName }
            ?: myState.allProfiles.first()
    }

    fun setCurrentProfile(name: String) {
        myState.currentProfileName = name
    }

    fun getAllProfiles(): List<ClipCraftProfile> = myState.allProfiles

    fun addProfile(profile: ClipCraftProfile) {
        removeProfile(profile.profileName)
        myState.allProfiles.add(profile)
    }

    fun removeProfile(name: String) {
        myState.allProfiles.removeIf { it.profileName == name }
    }
}
