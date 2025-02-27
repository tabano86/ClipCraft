package com.clipcraft.ui

import com.intellij.ui.dsl.builder.panel
import javax.swing.JCheckBox
import javax.swing.JPanel
import javax.swing.JTextField

class ClipCraftSetupWizardUI {
    private val includeMetadataCheck = JCheckBox("Include metadata (file size, timestamp, etc.)", true)
    private val useGitIgnoreCheck = JCheckBox("Use .gitignore", true)
    private val concurrencyField = JTextField("4", 5)
    private val mainPanel: JPanel = panel {
        row("Include Metadata:") {
            cell(includeMetadataCheck)
        }
        row("Use .gitignore:") {
            cell(useGitIgnoreCheck)
        }
        row("Max Concurrent Tasks:") {
            cell(concurrencyField)
        }
    }

    fun getMainPanel(): JPanel = mainPanel
    fun isIncludeMetadata() = includeMetadataCheck.isSelected
    fun isUseGitIgnore() = useGitIgnoreCheck.isSelected
    fun getMaxConcurrentTasks() = concurrencyField.text.toIntOrNull() ?: 4
}
