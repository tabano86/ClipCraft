package com.clipcraft.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.xmlb.XmlSerializerUtil

@Service(Service.Level.APP)
@State(name = "ClipCraftSettingsState", storages = [Storage("ClipCraftSettings.xml")])
class ClipCraftSettingsState : PersistentStateComponent<ClipCraftSettingsState> {

    private val logger = Logger.getInstance(ClipCraftSettingsState::class.java)

    var maxCopyCharacters: Int = 100_048_576

    override fun getState(): ClipCraftSettingsState = this

    override fun loadState(state: ClipCraftSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
        logger.info("Loaded ClipCraft settings: maxCopyCharacters=$maxCopyCharacters")
    }

    companion object {
        fun getInstance(): ClipCraftSettingsState =
            ApplicationManager.getApplication().getService(ClipCraftSettingsState::class.java)
    }
}
