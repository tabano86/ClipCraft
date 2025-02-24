package com.clipcraft.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/**
 * An alternative or legacy persistent service.
 * If not needed, you can remove or merge with ClipCraftSettingsState.
 */
@State(name = "ClipCraftSettingsService", storages = [Storage("ClipCraftSettingsService.xml")])
class ClipCraftSettingsService : PersistentStateComponent<ClipCraftSettingsService.State> {

    data class State(
        var someOldSetting: Boolean = false,
        var legacyValue: String = "",
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        fun getInstance(): ClipCraftSettingsService =
            ApplicationManager.getApplication().getService(ClipCraftSettingsService::class.java)
    }

    fun persist() {
        ApplicationManager.getApplication().saveSettings()
    }
}
