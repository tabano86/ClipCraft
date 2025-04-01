package com.clipcraft.services

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.ClipCraftProfile
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger

@Service
@State(name = "ClipCraftSettings", storages = [Storage("ClipCraftSettings.xml")])
class ClipCraftSettings : PersistentStateComponent<ClipCraftSettings.State> {
    private val logger = Logger.getInstance(ClipCraftSettings::class.java)

    // Rename property to avoid JVM signature clash.
    var myState = State()

    data class State(
        var currentProfileName: String = "Global Default",
        var allProfiles: MutableList<ClipCraftProfile> = mutableListOf(
            ClipCraftProfile("Global Default", ClipCraftOptions()),
            ),
    )

    fun getCurrentProfile(): ClipCraftProfile {
        return myState.allProfiles.find { it.profileName == myState.currentProfileName }
            ?: myState.allProfiles.first()
    }

    fun setCurrentProfile(name: String) {
        if (myState.allProfiles.any { it.profileName == name }) {
            myState.currentProfileName = name
        } else {
            logger.warn("Profile not found: $name")
        }
    }

    fun getSnippetPrefix(): String = getCurrentProfile().options.snippetHeaderText.orEmpty()
    fun getSnippetSuffix(): String = getCurrentProfile().options.snippetFooterText.orEmpty()

    override fun getState(): State = myState

    override fun loadState(newState: State) {
        myState = newState
    }

    companion object {
        fun getInstance(): ClipCraftSettings {
            return ApplicationManager.getApplication().getService(ClipCraftSettings::class.java)
        }
    }
}
