package com.clipcraft.actions

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.services.ClipCraftNotificationCenter
import com.clipcraft.services.ClipCraftProjectProfileManager
import com.clipcraft.services.ClipCraftSettings
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class ClipCraftResetDefaultsAction : AnAction("ClipCraft: Reset to Defaults", "Restore ClipCraft default settings", null) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val defaultOptions = ClipCraftOptions() // new instance = defaults
        ClipCraftSettings.getInstance().loadState(defaultOptions)

        if (defaultOptions.perProjectConfig && project != null) {
            ClipCraftProjectProfileManager.getInstance(project).loadState(defaultOptions)
        }

        ClipCraftNotificationCenter.notifyInfo("ClipCraft settings reset to defaults.", project)
    }
}
