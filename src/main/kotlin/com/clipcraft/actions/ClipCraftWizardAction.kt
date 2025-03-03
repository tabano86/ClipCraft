package com.clipcraft.actions

import com.clipcraft.ui.ClipCraftSetupWizard
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class ClipCraftWizardAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ClipCraftSetupWizard(project).show()
    }
}
