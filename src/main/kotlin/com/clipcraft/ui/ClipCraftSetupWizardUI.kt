package com.clipcraft.ui

import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTextField
import javax.swing.JLabel
import javax.swing.JPanel

class ClipCraftSetupWizardUI {

    private val mainPanel = JBPanel<JBPanel<*>>().apply {
        layout = null // For brevity; normally use a proper LayoutManager
    }

    private val includeMetadataCheck = JBCheckBox("Include metadata (file size, timestamp, etc.)", true)
    private val useGitIgnoreCheck = JBCheckBox("Use .gitignore", true)
    private val concurrencyText = JBTextField("4")

    init {
        val label = JLabel("ClipCraft Setup Wizard")
        label.setBounds(10, 10, 200, 25)
        mainPanel.add(label)

        includeMetadataCheck.setBounds(10, 50, 300, 25)
        mainPanel.add(includeMetadataCheck)

        useGitIgnoreCheck.setBounds(10, 80, 200, 25)
        mainPanel.add(useGitIgnoreCheck)

        val concurrencyLabel = JLabel("Max Concurrent Tasks:")
        concurrencyLabel.setBounds(10, 110, 150, 25)
        mainPanel.add(concurrencyLabel)

        concurrencyText.setBounds(165, 110, 50, 25)
        mainPanel.add(concurrencyText)
    }

    fun getMainPanel(): JPanel = mainPanel

    fun isIncludeMetadata(): Boolean = includeMetadataCheck.isSelected
    fun isUseGitIgnore(): Boolean = useGitIgnoreCheck.isSelected
    fun getMaxConcurrentTasks(): Int = concurrencyText.text.toIntOrNull() ?: 4
}
