package com.clipcraft

import com.clipcraft.model.ClipCraftOptions
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(name = "ClipCraftSettings", storages = [Storage("ClipCraftSettings.xml")])
class ClipCraftSettings : PersistentStateComponent<ClipCraftOptions> {
    private var myOptions = ClipCraftOptions()

    override fun getState(): ClipCraftOptions = myOptions

    override fun loadState(state: ClipCraftOptions) {
        myOptions = state
    }

    companion object {
        fun getInstance(): ClipCraftSettings {
            return ApplicationManager.getApplication().getService(ClipCraftSettings::class.java)
        }
    }
}
