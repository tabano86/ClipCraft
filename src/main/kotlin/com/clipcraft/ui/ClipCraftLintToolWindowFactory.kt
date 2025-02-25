package com.clipcraft.ui

import com.clipcraft.services.LintResultsService
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea

class ClipCraftLintToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder(JBUI.scale(10), JBUI.scale(10), JBUI.scale(10), JBUI.scale(10))
        }
        val resultsArea = JTextArea().apply { isEditable = false }
        val scroll = JBScrollPane(resultsArea)
        panel.add(JLabel("ClipCraft Lint Results:"), BorderLayout.NORTH)
        panel.add(scroll, BorderLayout.CENTER)
        val results = project.getService(LintResultsService::class.java)?.getAllResults().orEmpty()
        if (results.isNotEmpty()) {
            resultsArea.text = results.joinToString("\n") { it.formatMessage() }
        }
        toolWindow.contentManager.addContent(
            toolWindow.contentManager.factory.createContent(panel, "Lint Results", false),
        )
    }
}
