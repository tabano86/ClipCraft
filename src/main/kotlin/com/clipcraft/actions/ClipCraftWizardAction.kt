package com.clipcraft.actions

import com.clipcraft.services.ClipCraftNotificationCenter
import com.clipcraft.services.ClipCraftSettings
import com.clipcraft.ui.ClipCraftSetupWizard
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * Opens the ClipCraft Setup Wizard.
 */
class ClipCraftWizardAction : AnAction("ClipCraft: Setup Wizard", "Open ClipCraft Setup Wizard", null) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val currentOpts = ClipCraftSettings.getInstance().state
        val wizard = ClipCraftSetupWizard(currentOpts)
        if (wizard.showAndGet()) {
            val newOpts = wizard.getConfiguredOptions()
            ClipCraftSettings.getInstance().loadState(newOpts)
            ClipCraftNotificationCenter.notifyInfo("ClipCraft Wizard completed!", project)
        }
    }
}
