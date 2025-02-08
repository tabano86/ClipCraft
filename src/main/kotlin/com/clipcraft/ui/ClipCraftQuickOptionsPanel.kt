package com.clipcraft.ui

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.OutputFormat
import com.clipcraft.services.ClipCraftSettings
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import java.awt.GridLayout
import javax.swing.*

/**
 * A "quick" panel that can appear in a pop-up for adjusting common settings.
 * Triggered, e.g., if the user ALT-clicks the main action.
 */
class ClipCraftQuickOptionsPanel(initialOptions: ClipCraftOptions, private val project: Project?) : JPanel() {

    private val ignoreFoldersField = JTextField(initialOptions.ignoreFolders.joinToString(","), 30)
    private val ignoreFilesField = JTextField(initialOptions.ignoreFiles.joinToString(","), 30)
    private val ignorePatternsField = JTextField(initialOptions.ignorePatterns.joinToString(","), 30)
    private val removeCommentsCheckBox = JCheckBox("Remove Comments", initialOptions.removeComments)
    private val trimLineWhitespaceCheckBox = JCheckBox("Trim Trailing Whitespace", initialOptions.trimLineWhitespace)
    private val outputFormatComboBox = JComboBox(OutputFormat.values()).apply {
        selectedItem = initialOptions.outputFormat
    }
    private val removeImportsCheckBox = JCheckBox("Remove Import Statements", initialOptions.removeImports)
    private val openSettingsButton = JButton("Open Full Settings").apply {
        toolTipText = "Open the complete ClipCraft settings dialog."
        addActionListener {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, "ClipCraft")
        }
    }

    init {
        layout = GridLayout(0, 2, 10, 10)
        border = BorderFactory.createTitledBorder("Quick Options")
        add(JLabel("Ignore Folders:"))
        add(ignoreFoldersField)
        add(JLabel("Ignore Files:"))
        add(ignoreFilesField)
        add(JLabel("Ignore Patterns:"))
        add(ignorePatternsField)
        add(removeCommentsCheckBox)
        add(trimLineWhitespaceCheckBox)
        add(JLabel("Output Format:"))
        add(outputFormatComboBox)
        add(removeImportsCheckBox)
        add(openSettingsButton)
    }

    fun getOptions(): ClipCraftOptions {
        val currentOpts = ClipCraftSettings.getInstance().getActiveOptions()
        return currentOpts.copy(
            ignoreFolders = ignoreFoldersField.text.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            ignoreFiles = ignoreFilesField.text.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            ignorePatterns = ignorePatternsField.text.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            removeComments = removeCommentsCheckBox.isSelected,
            trimLineWhitespace = trimLineWhitespaceCheckBox.isSelected,
            outputFormat = outputFormatComboBox.selectedItem as OutputFormat,
            removeImports = removeImportsCheckBox.isSelected
        )
    }
}
