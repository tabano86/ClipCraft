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

    init {
        title = "ClipCraft Options"
        panel.add(includeLineNumbersCheckBox)
        panel.add(showPreviewCheckBox)
        panel.add(exportToFileCheckBox)
        panel.add(JLabel("Export File Path:"))
        panel.add(exportFilePathField)
        panel.add(includeMetadataCheckBox)
        init()
    }

    override fun createCenterPanel(): JComponent = panel

    fun getOptions(): ClipCraftOptions {
        // Return a new ClipCraftOptions retaining other fields from initialOptions
        return ClipCraftOptions(
            includeLineNumbers = includeLineNumbersCheckBox.isSelected,
            showPreview = showPreviewCheckBox.isSelected,
            exportToFile = exportToFileCheckBox.isSelected,
            exportFilePath = exportFilePathField.text,
            includeMetadata = includeMetadataCheckBox.isSelected,
            autoProcess = initialOptions.autoProcess,
            largeFileThreshold = initialOptions.largeFileThreshold,
            singleCodeBlock = initialOptions.singleCodeBlock,
            minimizeWhitespace = initialOptions.minimizeWhitespace
        )
    }
}
