package com.clipcraft.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.xmlb.XmlSerializerUtil

@Service(Service.Level.APP)
@State(name = "ClipCraftSettings", storages = [Storage("ClipCraftSettings.xml")])
class ClipCraftSettingsService : PersistentStateComponent<ClipCraftSettingsState> {

    private val logger = Logger.getInstance(ClipCraftSettingsService::class.java)

    private var state = ClipCraftSettingsState()

    override fun getState(): ClipCraftSettingsState = state

    override fun loadState(newState: ClipCraftSettingsState) {
        XmlSerializerUtil.copyBean(newState, state)
        logger.info(
            "Loaded settings: maxCopyCharacters=${state.maxCopyCharacters}, " +
                "concurrency=${state.advancedOptions.concurrencyMode}",
        )
    }

    companion object {
        fun getInstance(): ClipCraftSettingsService =
            ApplicationManager.getApplication().getService(ClipCraftSettingsService::class.java)
                ?: error("ClipCraftSettingsService not found!")
    }
}
