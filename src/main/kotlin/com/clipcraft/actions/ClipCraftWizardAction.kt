package com.clipcraft.actions

import com.clipcraft.ui.ClipCraftSetupWizard
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * Launches a setup wizard dialog for basic configuration.
 */
class ClipCraftWizardAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val wizard = ClipCraftSetupWizard(project)
        wizard.show()
    }
}
