package com.clipcraft.settings

import com.clipcraft.model.SettingsState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "com.clipcraft.settings.SettingsState",
    storages = [Storage("clipCraftSettings.xml")],
)
class SettingsStateProvider : PersistentStateComponent<SettingsState> {

    var internalState = SettingsState()

    override fun getState(): SettingsState = internalState

    override fun loadState(state: SettingsState) {
        XmlSerializerUtil.copyBean(state, this.internalState)
    }

    companion object {
        fun getInstance(): SettingsStateProvider =
            ApplicationManager.getApplication().getService(SettingsStateProvider::class.java)
    }
}
