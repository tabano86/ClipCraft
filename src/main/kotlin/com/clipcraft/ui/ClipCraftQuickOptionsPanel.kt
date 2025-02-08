package com.clipcraft.ui

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.OutputFormat
import com.clipcraft.services.ClipCraftSettings
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import java.awt.GridLayout
import javax.swing.*

class ClipCraftQuickOptionsPanel(initialOptions: ClipCraftOptions, private val project: Project?) : JPanel() {
    private val ignoreFoldersField = JTextField(initialOptions.ignoreFolders.joinToString(","), 30).apply {
        toolTipText = "Comma-separated folder names to ignore."
    }
    private val ignoreFilesField = JTextField(initialOptions.ignoreFiles.joinToString(","), 30).apply {
        toolTipText = "Comma-separated file names to ignore."
    }
    private val ignorePatternsField = JTextField(initialOptions.ignorePatterns.joinToString(","), 30).apply {
        toolTipText = "Comma-separated regex patterns to ignore files."
    }
    private val removeCommentsCheckBox = JCheckBox("Remove Comments", initialOptions.removeComments).apply {
        toolTipText = "Strip out comment lines from code."
    }
    private val trimLineWhitespaceCheckBox = JCheckBox("Trim Trailing Whitespace", initialOptions.trimLineWhitespace).apply {
        toolTipText = "Remove trailing whitespace from each line."
    }
    private val outputFormatComboBox = JComboBox(OutputFormat.values()).apply {
        selectedItem = initialOptions.outputFormat
        toolTipText = "Select output format: Markdown, Plain, or HTML."
    }
    private val removeImportsCheckBox = JCheckBox("Remove Import Statements", initialOptions.removeImports).apply {
        toolTipText = "Remove import statements from the code."
    }
    private val openSettingsButton = JButton("Open Full Settings").apply {
        toolTipText = "Open the full ClipCraft settings panel."
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

    /**
     * Returns a new ClipCraftOptions instance based on the current quick panel settings.
     * This uses a copy of the persisted state and applies the quick changes.
     */
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
