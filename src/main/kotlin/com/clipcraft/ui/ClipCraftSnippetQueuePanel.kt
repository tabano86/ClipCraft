package com.clipcraft.ui

import com.clipcraft.model.Snippet
import com.clipcraft.services.ClipCraftQueueService
import com.intellij.openapi.project.Project
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea

class ClipCraftSnippetQueuePanel(project: Project) {
    val rootPanel: JPanel = JPanel(BorderLayout())
    private val snippetTextArea = JTextArea()
    private val queueService = project.getService(ClipCraftQueueService::class.java)

    init {
        val refreshBtn = JButton("Refresh Queue").apply {
            addActionListener { refreshQueue() }
        }
        val clearBtn = JButton("Clear Queue").apply {
            addActionListener {
                queueService.clearQueue()
                refreshQueue()
            }
        }

        val topPanel = JPanel().apply {
            add(refreshBtn)
            add(clearBtn)
        }
        rootPanel.add(topPanel, BorderLayout.NORTH)
        rootPanel.add(JScrollPane(snippetTextArea), BorderLayout.CENTER)

        refreshQueue()
    }

    private fun refreshQueue() {
        val snippets: List<Snippet> = queueService.getAllSnippets()
        val sb = StringBuilder()
        snippets.forEach { snippet ->
            sb.append("File: ${snippet.fileName ?: "Untitled"}\n")
            sb.append(snippet.content)
            sb.append("\n------------------------\n")
        }
        snippetTextArea.text = sb.toString()
    }
}
