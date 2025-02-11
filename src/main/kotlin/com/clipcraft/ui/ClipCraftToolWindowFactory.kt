package com.clipcraft.ui

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

class ClipCraftToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = ClipCraftSnippetQueuePanel(project)
        val content = toolWindow.contentManager.factory.createContent(panel.rootPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
