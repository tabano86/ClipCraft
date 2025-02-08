package com.clipcraft.services

import com.clipcraft.model.ClipCraftOptions
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import com.intellij.openapi.components.ServiceManager

@State(name = "ClipCraftProjectSettings", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class ClipCraftProjectProfileManager(private val project: Project) : PersistentStateComponent<ClipCraftOptions> {

    private var projectOptions = ClipCraftOptions()

    override fun getState(): ClipCraftOptions = projectOptions

    override fun loadState(state: ClipCraftOptions) {
        projectOptions = state
    }

    companion object {
        fun getInstance(project: Project): ClipCraftProjectProfileManager {
            return ServiceManager.getService(project, ClipCraftProjectProfileManager::class.java)
        }
    }
}
