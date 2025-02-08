package com.clipcraft.ui

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.OutputFormat
import com.clipcraft.services.ClipCraftSettings
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import java.awt.GridLayout
import javax.swing.*

/**
 * A simplified, quick options panel. Displayed if user ALT-clicks the main ClipCraft action.
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
        addActionListener {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, "ClipCraft")
        }
    }

    init {
        layout = GridLayout(0, 2, 10, 10)
        add(JLabel("Ignore Folders (comma separated):"))
        add(ignoreFoldersField)
        add(JLabel("Ignore Files (comma separated):"))
        add(ignoreFilesField)
        add(JLabel("Ignore Patterns (regex, comma separated):"))
        add(ignorePatternsField)
        add(removeCommentsCheckBox)
        add(trimLineWhitespaceCheckBox)
        add(JLabel("Output Format:"))
        add(outputFormatComboBox)
        add(removeImportsCheckBox)
        add(openSettingsButton)
    }

    fun getOptions(): ClipCraftOptions {
        val currentOpts = ClipCraftSettings.getInstance().state
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
