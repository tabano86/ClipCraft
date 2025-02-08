package com.clipcraft.services

import com.clipcraft.model.ClipCraftOptions
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

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
