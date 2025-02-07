package com.clipcraft

import com.clipcraft.model.ClipCraftOptions
import com.intellij.openapi.ui.DialogWrapper
import java.awt.BorderLayout
import java.awt.GridLayout
import javax.swing.*

class ClipCraftOptionsDialog(initialOptions: ClipCraftOptions) : DialogWrapper(true) {
    private val includeLineNumbersCheckBox = JCheckBox("Include Line Numbers", initialOptions.includeLineNumbers)
    private val showPreviewCheckBox = JCheckBox("Show Preview", initialOptions.showPreview)
    private val exportToFileCheckBox = JCheckBox("Export to File", initialOptions.exportToFile)
    private val exportFilePathField = JTextField(initialOptions.exportFilePath, 30)
    private val includeMetadataCheckBox = JCheckBox("Include File Metadata", initialOptions.includeMetadata)
    private val singleBlockCheckBox = JCheckBox("Single Code Block", initialOptions.singleCodeBlock)
    private val minimizeWhitespaceCheckBox = JCheckBox("Minimize Whitespace", initialOptions.minimizeWhitespace)

    init {
        title = "ClipCraft Options"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        val grid = JPanel(GridLayout(0, 2, 10, 10))
        grid.add(includeLineNumbersCheckBox)
        grid.add(showPreviewCheckBox)
        grid.add(exportToFileCheckBox)
        grid.add(JLabel("Export File Path:"))
        grid.add(exportFilePathField)
        grid.add(includeMetadataCheckBox)
        grid.add(singleBlockCheckBox)
        grid.add(minimizeWhitespaceCheckBox)
        panel.add(grid, BorderLayout.CENTER)
        return panel
    }

    fun getOptions(): ClipCraftOptions {
        return ClipCraftOptions(
            includeLineNumbers = includeLineNumbersCheckBox.isSelected,
            showPreview = showPreviewCheckBox.isSelected,
            exportToFile = exportToFileCheckBox.isSelected,
            exportFilePath = exportFilePathField.text,
            includeMetadata = includeMetadataCheckBox.isSelected,
            singleCodeBlock = singleBlockCheckBox.isSelected,
            minimizeWhitespace = minimizeWhitespaceCheckBox.isSelected
        )
    }
}
