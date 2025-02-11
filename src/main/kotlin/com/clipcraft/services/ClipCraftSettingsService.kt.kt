package com.clipcraft.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/**
 * Example persistent service for advanced usage.
 * Currently not deeply integrated with the rest of the code, but can be extended.
 */
@State(name = "ClipCraftSettingsService", storages = [Storage("ClipCraftSettings.xml")])
@Service(Service.Level.APP)
class ClipCraftSettingsService : PersistentStateComponent<ClipCraftSettingsService.State> {

    class State {
        var activeProfileName: String = "Default"
        var gptApiKey: String? = null
    }

    private var myState = State()

    override fun getState(): State = myState
    override fun loadState(state: State) {
        myState = state
    }

    companion object {
        fun getInstance(): ClipCraftSettingsService {
            return ApplicationManager.getApplication().getService(ClipCraftSettingsService::class.java)
        }
    }
}
