package com.clipcraft.ui

import com.clipcraft.model.Snippet
import com.clipcraft.services.ClipCraftNotificationCenter
import com.clipcraft.services.ClipCraftSharingService
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea

/**
 * Creates a tool window containing a simple GPT chat interface (stub).
 */
class ClipCraftToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JPanel(BorderLayout())

        val inputArea = JTextArea(5, 50).apply {
            lineWrap = true
            wrapStyleWord = true
        }
        val outputArea = JTextArea(15, 50).apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
        }

        val scrollInput = JScrollPane(inputArea)
        val scrollOutput = JScrollPane(outputArea)

        val button = JButton("Send to GPT").apply {
            addActionListener {
                val text = inputArea.text.trim()
                if (text.isEmpty()) {
                    ClipCraftNotificationCenter.warn("No text provided to GPT.")
                    return@addActionListener
                }
                val sharingService = project.getService(ClipCraftSharingService::class.java) ?: return@addActionListener

                runBlocking {
                    val snippet = Snippet(content = text, fileName = "UserChatInput")
                    val response = withContext(Dispatchers.IO) {
                        sharingService.sendToGpt(snippet, "Please analyze or transform this code:")
                    }
                    outputArea.text = response
                }
            }
        }

        val topPanel = JPanel(BorderLayout())
        topPanel.add(JLabel("Enter text/snippet to discuss with GPT:"), BorderLayout.NORTH)
        topPanel.add(scrollInput, BorderLayout.CENTER)
        topPanel.add(button, BorderLayout.EAST)

        panel.add(topPanel, BorderLayout.NORTH)
        panel.add(scrollOutput, BorderLayout.CENTER)

        val content = toolWindow.contentManager.factory.createContent(panel, "GPT Chat", false)
        toolWindow.contentManager.addContent(content)
    }
}
