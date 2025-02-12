package com.clipcraft.ui

import com.clipcraft.lint.LintIssue
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

/**
 * Displays the lint results from the last run in a tool window.
 */
class ClipCraftLintToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder(JBUI.scale(10), JBUI.scale(10), JBUI.scale(10), JBUI.scale(10))
        }

        val resultsArea = JTextArea().apply {
            isEditable = false
            lineWrap = false
            wrapStyleWord = false
        }
        val scrollOutput = JBScrollPane(resultsArea)

        val topLabel = JLabel("ClipCraft Lint Results:")

        panel.add(topLabel, BorderLayout.NORTH)
        panel.add(scrollOutput, BorderLayout.CENTER)

        // Load any existing results
        val lintService = project.getService(LintResultsService::class.java)
        val existing = lintService?.getAllResults().orEmpty()
        if (existing.isNotEmpty()) {
            resultsArea.text = formatLintResults(existing)
        }

        val content = toolWindow.contentManager.factory.createContent(panel, "Lint Results", false)
        toolWindow.contentManager.addContent(content)
    }

    private fun formatLintResults(issues: List<LintIssue>): String {
        return issues.joinToString("\n") { it.formatMessage() }
    }
}
