package com.clipcraft.ui

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.OutputFormat
import com.intellij.openapi.ui.DialogWrapper
import java.awt.GridLayout
import javax.swing.*

/**
 * A dialog that manually sets various options. Shown when autoProcess == false or triggered manually.
 */
class ClipCraftOptionsDialog(private val initialOptions: ClipCraftOptions) : DialogWrapper(true) {

    private val panel = JPanel(GridLayout(0, 2, 10, 10))

    private val includeLineNumbersCheckBox = JCheckBox("Include Line Numbers", initialOptions.includeLineNumbers)
    private val showPreviewCheckBox = JCheckBox("Show Preview", initialOptions.showPreview)
    private val exportToFileCheckBox = JCheckBox("Export to File", initialOptions.exportToFile)
    private val exportFilePathField = JTextField(initialOptions.exportFilePath, 30)
    private val includeMetadataCheckBox = JCheckBox("Include File Metadata", initialOptions.includeMetadata)
    private val outputFormatComboBox = JComboBox(OutputFormat.values()).apply {
        selectedItem = initialOptions.outputFormat
    }
    private val removeImportsCheckBox = JCheckBox("Remove Import Statements", initialOptions.removeImports)

    private val ignoreFoldersField = JTextField(initialOptions.ignoreFolders.joinToString(","), 30)
    private val ignoreFilesField = JTextField(initialOptions.ignoreFiles.joinToString(","), 30)
    private val ignorePatternsField = JTextField(initialOptions.ignorePatterns.joinToString(","), 30)
    private val removeCommentsCheckBox = JCheckBox("Remove Comments", initialOptions.removeComments)
    private val trimLineWhitespaceCheckBox = JCheckBox("Trim Trailing Whitespace", initialOptions.trimLineWhitespace)

    init {
        title = "ClipCraft Options"
        // Main
        panel.add(includeLineNumbersCheckBox)
        panel.add(showPreviewCheckBox)
        panel.add(exportToFileCheckBox)
        panel.add(JLabel("Export File Path:"))
        panel.add(exportFilePathField)
        panel.add(includeMetadataCheckBox)
        panel.add(JLabel("Output Format:"))
        panel.add(outputFormatComboBox)
        panel.add(removeImportsCheckBox)

        // Advanced
        val advancedPanel = JPanel(GridLayout(0, 2, 10, 10)).apply {
            border = BorderFactory.createTitledBorder("Advanced Options")
            add(JLabel("Ignore Folders (comma separated):"))
            add(ignoreFoldersField)
            add(JLabel("Ignore Files (comma separated):"))
            add(ignoreFilesField)
            add(JLabel("Ignore Patterns (regex, comma separated):"))
            add(ignorePatternsField)
            add(removeCommentsCheckBox)
            add(trimLineWhitespaceCheckBox)
        }

        panel.add(advancedPanel)
        init()
    }

    override fun createCenterPanel(): JComponent = panel

    /**
     * Gather the new user-defined options from this dialog.
     */
    fun getOptions(): ClipCraftOptions {
        return initialOptions.copy(
            includeLineNumbers = includeLineNumbersCheckBox.isSelected,
            showPreview = showPreviewCheckBox.isSelected,
            exportToFile = exportToFileCheckBox.isSelected,
            exportFilePath = exportFilePathField.text,
            includeMetadata = includeMetadataCheckBox.isSelected,
            outputFormat = outputFormatComboBox.selectedItem as OutputFormat,
            removeImports = removeImportsCheckBox.isSelected,
            ignoreFolders = ignoreFoldersField.text.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            ignoreFiles = ignoreFilesField.text.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            ignorePatterns = ignorePatternsField.text.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            removeComments = removeCommentsCheckBox.isSelected,
            trimLineWhitespace = trimLineWhitespaceCheckBox.isSelected
        )
    }
}
