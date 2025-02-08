package com.clipcraft.actions

import com.clipcraft.services.ClipCraftNotificationCenter
import com.clipcraft.services.ClipCraftSettings
import com.clipcraft.ui.ClipCraftWizard
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class ClipCraftWizardAction : AnAction("ClipCraft: Setup Wizard", "Open ClipCraft Setup Wizard", null) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val currentOpts = ClipCraftSettings.getInstance().state
        val wizard = ClipCraftWizard(currentOpts)
        if (wizard.showAndGet()) {
            val newOpts = wizard.getConfiguredOptions()
            ClipCraftSettings.getInstance().loadState(newOpts)
            ClipCraftNotificationCenter.notifyInfo("ClipCraft Wizard completed!", project)
        }
    }
}
