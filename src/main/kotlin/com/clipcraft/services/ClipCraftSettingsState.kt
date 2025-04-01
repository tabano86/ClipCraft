package com.clipcraft.services

import com.clipcraft.model.ClipCraftOptions
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@Service(Service.Level.APP)
@State(name = "ClipCraftSettingsState", storages = [Storage("ClipCraftSettings.xml")])
data class ClipCraftSettingsState(
    var maxCopyCharacters: Int = 100_048_576,
    var advancedOptions: ClipCraftOptions = ClipCraftOptions(),
) : PersistentStateComponent<ClipCraftSettingsState> {

    override fun getState(): ClipCraftSettingsState = this

    override fun loadState(state: ClipCraftSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(): ClipCraftSettingsState =
            ApplicationManager.getApplication().getService(ClipCraftSettingsState::class.java)
    }
}
