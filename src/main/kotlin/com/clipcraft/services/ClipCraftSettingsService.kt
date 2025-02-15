package com.clipcraft.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(name = "ClipCraftSettingsService", storages = [Storage("ClipCraftSettings.xml")])
@Service(Service.Level.APP)
class ClipCraftSettingsService : PersistentStateComponent<ClipCraftSettingsService.State> {
    data class State(
        var activeProfileName: String = "Default",
        // NEW: store JSON for all profiles
        var profilesJson: String = "",
    )

    private var state = State()

    override fun getState() = state

    override fun loadState(state: State) {
        this.state = state
        // The actual parsing is done inside ClipCraftSettings.loadFromService()
    }

    companion object {
        fun getInstance(): ClipCraftSettingsService =
            ApplicationManager.getApplication().getService(ClipCraftSettingsService::class.java)
    }

    // NEW: convenience method to force-save
    fun persist() {
        ApplicationManager.getApplication().saveSettings()
    }
}
