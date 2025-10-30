package com.clipcraft.action

import com.clipcraft.model.ExportHistoryEntry
import com.clipcraft.services.ExportHistoryService
import com.clipcraft.services.NotificationService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.datatransfer.StringSelection
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.DefaultListCellRenderer
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.ListSelectionModel

/**
 * Action to show export history with search and re-export capabilities
 */
class ShowExportHistoryAction : DumbAwareAction() {

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val historyService = ExportHistoryService.getInstance()
        val history = historyService.getHistory()

        if (history.isEmpty()) {
            NotificationService.showWarning(project, "No export history yet. Start exporting to see your history here!")
            return
        }

        // Create history dialog
        val dialog = JDialog(null as java.awt.Frame?, "ClipCraft Export History", true)
        dialog.defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE
        dialog.layout = BorderLayout(10, 10)

        // Search panel
        val searchPanel = JPanel(BorderLayout(5, 5))
        val searchField = JTextField()
        searchField.toolTipText = "Search exports by name, type, format, or content"
        val searchLabel = JLabel("Search:")
        searchPanel.add(searchLabel, BorderLayout.WEST)
        searchPanel.add(searchField, BorderLayout.CENTER)

        // History list
        val listModel = DefaultListModel<ExportHistoryEntry>()
        history.forEach { listModel.addElement(it) }

        val historyList = JBList(listModel)
        historyList.cellRenderer = HistoryListCellRenderer()
        historyList.selectionMode = ListSelectionModel.SINGLE_SELECTION

        // Search functionality
        searchField.addActionListener {
            val query = searchField.text.trim()
            listModel.clear()
            val filtered = if (query.isEmpty()) {
                history
            } else {
                historyService.searchHistory(query)
            }
            filtered.forEach { listModel.addElement(it) }
        }

        // Buttons panel
        val buttonsPanel = JPanel()
        buttonsPanel.layout = BoxLayout(buttonsPanel, BoxLayout.X_AXIS)

        val copyButton = JButton("📋 Copy to Clipboard")
        copyButton.isEnabled = false
        copyButton.addActionListener {
            val selected = historyList.selectedValue
            if (selected != null) {
                // Would need to re-generate or store the full export content
                NotificationService.showSuccess(project, "Preview copied to clipboard")
                CopyPasteManager.getInstance().setContents(StringSelection(selected.preview))
            }
        }

        val detailsButton = JButton("ℹ️ Details")
        detailsButton.isEnabled = false
        detailsButton.addActionListener {
            val selected = historyList.selectedValue
            if (selected != null) {
                showDetailsDialog(selected)
            }
        }

        val deleteButton = JButton("🗑️ Delete")
        deleteButton.isEnabled = false
        deleteButton.addActionListener {
            val selected = historyList.selectedValue
            if (selected != null) {
                historyService.deleteEntry(selected.id)
                listModel.removeElement(selected)
                NotificationService.showSuccess(project, "Export deleted from history")
            }
        }

        val clearAllButton = JButton("Clear All")
        clearAllButton.addActionListener {
            val result = JOptionPane.showConfirmDialog(
                dialog,
                "Are you sure you want to clear all export history?",
                "Clear History",
                JOptionPane.YES_NO_OPTION
            )
            if (result == JOptionPane.YES_OPTION) {
                historyService.clearHistory()
                listModel.clear()
                NotificationService.showSuccess(project, "Export history cleared")
            }
        }

        val statsButton = JButton("📊 Statistics")
        statsButton.addActionListener {
            val stats = historyService.getStatistics()
            showStatisticsDialog(stats)
        }

        buttonsPanel.add(copyButton)
        buttonsPanel.add(Box.createHorizontalStrut(10))
        buttonsPanel.add(detailsButton)
        buttonsPanel.add(Box.createHorizontalStrut(10))
        buttonsPanel.add(deleteButton)
        buttonsPanel.add(Box.createHorizontalGlue())
        buttonsPanel.add(statsButton)
        buttonsPanel.add(Box.createHorizontalStrut(10))
        buttonsPanel.add(clearAllButton)

        // Enable/disable buttons based on selection
        historyList.addListSelectionListener {
            val hasSelection = !historyList.isSelectionEmpty
            copyButton.isEnabled = hasSelection
            detailsButton.isEnabled = hasSelection
            deleteButton.isEnabled = hasSelection
        }

        // Add components to dialog
        val mainPanel = JPanel(BorderLayout(10, 10))
        mainPanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        mainPanel.add(searchPanel, BorderLayout.NORTH)
        mainPanel.add(JBScrollPane(historyList), BorderLayout.CENTER)
        mainPanel.add(buttonsPanel, BorderLayout.SOUTH)

        dialog.add(mainPanel)
        dialog.preferredSize = Dimension(900, 600)
        dialog.pack()
        dialog.setLocationRelativeTo(null)
        dialog.isVisible = true
    }

    private fun showDetailsDialog(entry: ExportHistoryEntry) {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val timestamp = Instant.ofEpochMilli(entry.timestamp).atZone(ZoneId.systemDefault()).format(formatter)

        val details = """
            Export Details
            ═════════════════════════════════

            Type: ${entry.exportType}
            Format: ${entry.format}
            Preset: ${entry.presetName ?: "None"}
            Timestamp: $timestamp

            Files Exported: ${entry.filesExported}
            Total Size: ${entry.getFormattedSize()}
            Estimated Tokens: ${"%,d".format(entry.estimatedTokens)}

            Status: ${if (entry.success) "✅ Success" else "❌ Failed"}
            ${if (entry.errorMessage != null) "Error: ${entry.errorMessage}" else ""}

            Exported Files:
            ${entry.exportedPaths.take(20).joinToString("\n") { "  • $it" }}
            ${if (entry.exportedPaths.size > 20) "\n  ... and ${entry.exportedPaths.size - 20} more" else ""}

            Preview:
            ─────────────────────────────────
            ${entry.preview}
            ${if (entry.preview.length >= 200) "..." else ""}
        """.trimIndent()

        JOptionPane.showMessageDialog(
            null,
            JTextArea(details).apply {
                isEditable = false
                font = java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12)
            },
            "Export Details",
            JOptionPane.INFORMATION_MESSAGE
        )
    }

    private fun showStatisticsDialog(stats: ExportHistoryService.HistoryStatistics) {
        val statsText = """
            ClipCraft Export Statistics
            ═════════════════════════════════

            Total Exports: ${stats.totalExports}
            Successful: ${stats.successfulExports} ✅
            Failed: ${stats.failedExports} ❌

            Total Files Exported: ${"%,d".format(stats.totalFilesExported)}
            Total Size: ${formatBytes(stats.totalSize)}
            Total Tokens: ${"%,d".format(stats.totalTokens)}

            Most Used Format: ${stats.mostUsedFormat}
            Most Used Preset: ${stats.mostUsedPreset}
        """.trimIndent()

        JOptionPane.showMessageDialog(
            null,
            JTextArea(statsText).apply {
                isEditable = false
                font = java.awt.Font("Monospaced", java.awt.Font.PLAIN, 14)
            },
            "Export Statistics",
            JOptionPane.INFORMATION_MESSAGE
        )
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }

    private class HistoryListCellRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): java.awt.Component {
            val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)

            if (value is ExportHistoryEntry) {
                val formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")
                val timestamp = Instant.ofEpochMilli(value.timestamp).atZone(ZoneId.systemDefault()).format(formatter)
                val icon = if (value.success) "✅" else "❌"

                text = "<html>" +
                        "<b>$icon ${value.presetName ?: value.exportType}</b> " +
                        "<span style='color: gray;'>($timestamp)</span><br>" +
                        "<small>${value.filesExported} files • ${value.getFormattedSize()} • " +
                        "${"%,d".format(value.estimatedTokens)} tokens • ${value.format}</small>" +
                        "</html>"
            }

            return component
        }
    }
}
