package com.clipcraft

import com.clipcraft.model.ClipCraftOptions
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

/**
 * A compact panel for quick adjustment of advanced options.
 * This panel is shown when the user holds Alt while invoking the action.
 */
class ClipCraftQuickOptionsPanel(initialOptions: ClipCraftOptions) : JPanel() {
    private val ignoreFoldersField = JTextField(initialOptions.ignoreFolders.joinToString(",")).apply {
        toolTipText = "Comma-separated folder names to ignore."
    }
    private val ignoreFilesField = JTextField(initialOptions.ignoreFiles.joinToString(",")).apply {
        toolTipText = "Comma-separated file names to ignore."
    }
    private val ignorePatternsField = JTextField(initialOptions.ignorePatterns.joinToString(",")).apply {
        toolTipText = "Comma-separated regex patterns to ignore files."
    }
    private val removeCommentsCheckBox = JCheckBox("Remove Comments", initialOptions.removeComments).apply {
        toolTipText = "Strip out comment lines from code."
    }
    private val trimLineWhitespaceCheckBox =
        JCheckBox("Trim Trailing Whitespace", initialOptions.trimLineWhitespace).apply {
            toolTipText = "Remove trailing spaces (but preserve indentation)."
        }

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
