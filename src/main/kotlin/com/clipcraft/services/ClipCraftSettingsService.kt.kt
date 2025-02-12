package com.clipcraft.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/**
 * Persistent settings service for ClipCraft.
 * Stores minimal state such as the active profile name.
 */
@State(name = "ClipCraftSettingsService", storages = [Storage("ClipCraftSettings.xml")])
@Service(Service.Level.APP)
class ClipCraftSettingsService : PersistentStateComponent<ClipCraftSettingsService.State> {

    data class State(
        var activeProfileName: String = "Default"
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        fun getInstance(): ClipCraftSettingsService {
            return ApplicationManager.getApplication().getService(ClipCraftSettingsService::class.java)
        }
    }
}
