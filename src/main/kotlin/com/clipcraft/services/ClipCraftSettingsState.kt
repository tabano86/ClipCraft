package com.clipcraft.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * ClipCraftSettingsState is an application-level service that holds plugin settings
 * which are persisted across IDE restarts via XML serialization.
 */
@State(name = "ClipCraftSettingsState", storages = [Storage("ClipCraftSettings.xml")])
class ClipCraftSettingsState : PersistentStateComponent<ClipCraftSettingsState> {

    private val logger = Logger.getInstance(ClipCraftSettingsState::class.java)

    /**
     * Maximum number of characters that can be copied without confirmation.
     * Default = 100,000 characters.
     */
    var maxCopyCharacters: Int = 100_000

    /**
     * Additional settings can be added here...
     */

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
