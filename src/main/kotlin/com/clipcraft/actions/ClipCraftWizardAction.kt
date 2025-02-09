package com.clipcraft.actions

import com.clipcraft.services.ClipCraftNotificationCenter
import com.clipcraft.services.ClipCraftSettings
import com.clipcraft.ui.ClipCraftSetupWizard
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.slf4j.LoggerFactory

class ClipCraftWizardAction : AnAction(
    "ClipCraft: Setup Wizard",
    "Open ClipCraft Setup Wizard",
    null
) {
    private val log = LoggerFactory.getLogger(ClipCraftWizardAction::class.java)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val settings = ClipCraftSettings.getInstance()
        val currentOpts = settings.getActiveOptions()
        log.debug("Invoking the Setup Wizard with current options.")
        val wizard = ClipCraftSetupWizard(currentOpts)
        if (wizard.showAndGet()) {
            val newOpts = wizard.getConfiguredOptions()
            settings.saveProfile(settings.state.activeProfileName, newOpts)
            ClipCraftNotificationCenter.notifyInfo("ClipCraft Wizard completed!", project)
            log.info("Wizard completed and new options saved.")
        }
    }
}
