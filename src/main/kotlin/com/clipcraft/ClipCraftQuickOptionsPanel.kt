package com.clipcraft

import com.clipcraft.model.ClipCraftOptions
import javax.swing.*

class ClipCraftQuickOptionsPanel(initialOptions: ClipCraftOptions) : JPanel() {
    private val ignoreFoldersField = JTextField(initialOptions.ignoreFolders.joinToString(","))
    private val ignoreFilesField = JTextField(initialOptions.ignoreFiles.joinToString(","))
    private val ignorePatternsField = JTextField(initialOptions.ignorePatterns.joinToString(","))
    private val removeCommentsCheckBox = JCheckBox("Remove Comments", initialOptions.removeComments)
    private val trimLineWhitespaceCheckBox = JCheckBox("Trim Line Whitespace", initialOptions.trimLineWhitespace)

    init {
        layout = java.awt.GridLayout(0, 2, 10, 10)
        add(JLabel("Ignore Folders (comma separated):"))
        add(ignoreFoldersField)
        add(JLabel("Ignore Files (comma separated):"))
        add(ignoreFilesField)
        add(JLabel("Ignore Patterns (regex, comma separated):"))
        add(ignorePatternsField)
        add(removeCommentsCheckBox)
        add(trimLineWhitespaceCheckBox)
    }

    fun getOptions(): ClipCraftOptions {
        val currentOpts = ClipCraftSettings.getInstance().state
        return currentOpts.copy(
            ignoreFolders = ignoreFoldersField.text.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            ignoreFiles = ignoreFilesField.text.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            ignorePatterns = ignorePatternsField.text.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            removeComments = removeCommentsCheckBox.isSelected,
            trimLineWhitespace = trimLineWhitespaceCheckBox.isSelected
        )
    }
}
