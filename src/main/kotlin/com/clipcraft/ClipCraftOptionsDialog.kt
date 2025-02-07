package com.clipcraft

import com.clipcraft.model.ClipCraftOptions
import com.intellij.openapi.ui.DialogWrapper
import java.awt.GridLayout
import javax.swing.*

class ClipCraftOptionsDialog(private val initialOptions: ClipCraftOptions) : DialogWrapper(true) {
    private val panel = JPanel(GridLayout(0, 2, 10, 10))
    private val includeLineNumbersCheckBox = JCheckBox("Include Line Numbers", initialOptions.includeLineNumbers)
    private val showPreviewCheckBox = JCheckBox("Show Preview", initialOptions.showPreview)
    private val exportToFileCheckBox = JCheckBox("Export to File", initialOptions.exportToFile)
    private val exportFilePathField = JTextField(initialOptions.exportFilePath, 30)
    private val includeMetadataCheckBox = JCheckBox("Include File Metadata", initialOptions.includeMetadata)

    // Advanced options components.
    private val ignoreFoldersField = JTextField(initialOptions.ignoreFolders.joinToString(","))
    private val ignoreFilesField = JTextField(initialOptions.ignoreFiles.joinToString(","))
    private val ignorePatternsField = JTextField(initialOptions.ignorePatterns.joinToString(","))
    private val removeCommentsCheckBox = JCheckBox("Remove Comments", initialOptions.removeComments)
    private val trimLineWhitespaceCheckBox = JCheckBox("Trim Line Whitespace", initialOptions.trimLineWhitespace)

    init {
        title = "ClipCraft Options"
        panel.add(includeLineNumbersCheckBox)
        panel.add(showPreviewCheckBox)
        panel.add(exportToFileCheckBox)
        panel.add(JLabel("Export File Path:"))
        panel.add(exportFilePathField)
        panel.add(includeMetadataCheckBox)

        // Advanced options panel.
        val advancedPanel = JPanel(GridLayout(0, 2, 10, 10))
        advancedPanel.border = BorderFactory.createTitledBorder("Advanced Options")
        advancedPanel.add(JLabel("Ignore Folders (comma separated):"))
        advancedPanel.add(ignoreFoldersField)
        advancedPanel.add(JLabel("Ignore Files (comma separated):"))
        advancedPanel.add(ignoreFilesField)
        advancedPanel.add(JLabel("Ignore Patterns (regex, comma separated):"))
        advancedPanel.add(ignorePatternsField)
        advancedPanel.add(removeCommentsCheckBox)
        advancedPanel.add(trimLineWhitespaceCheckBox)

        panel.add(advancedPanel)
        // Fill the remaining cell for layout symmetry.
        panel.add(JPanel())
        init()
    }

    override fun createCenterPanel(): JComponent = panel

    fun getOptions(): ClipCraftOptions {
        return ClipCraftOptions(
            includeLineNumbers = includeLineNumbersCheckBox.isSelected,
            showPreview = showPreviewCheckBox.isSelected,
            exportToFile = exportToFileCheckBox.isSelected,
            exportFilePath = exportFilePathField.text,
            includeMetadata = includeMetadataCheckBox.isSelected,
            autoProcess = initialOptions.autoProcess,
            largeFileThreshold = initialOptions.largeFileThreshold,
            singleCodeBlock = initialOptions.singleCodeBlock,
            minimizeWhitespace = initialOptions.minimizeWhitespace,
            ignoreFolders = ignoreFoldersField.text.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            ignoreFiles = ignoreFilesField.text.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            ignorePatterns = ignorePatternsField.text.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            removeComments = removeCommentsCheckBox.isSelected,
            trimLineWhitespace = trimLineWhitespaceCheckBox.isSelected
        )
    }
}
