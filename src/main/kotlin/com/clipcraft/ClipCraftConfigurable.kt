package com.clipcraft

import com.clipcraft.model.ClipCraftOptions
import com.intellij.openapi.options.Configurable
import java.awt.GridLayout
import javax.swing.*

class ClipCraftConfigurable : Configurable {

    private val includeLineNumbersCheckBox = JCheckBox("Include Line Numbers")
    private val showPreviewCheckBox = JCheckBox("Show Preview")
    private val exportToFileCheckBox = JCheckBox("Export to File")
    private val exportFilePathField = JTextField(30)
    private val includeMetadataCheckBox = JCheckBox("Include File Metadata")
    private val autoProcessCheckBox = JCheckBox("No Prompt (Auto Process)")
    private val largeFileThresholdField = JTextField(10)

    private var mainPanel: JPanel? = null

    override fun getDisplayName(): String = "ClipCraft"

    override fun createComponent(): JComponent? {
        mainPanel = JPanel(GridLayout(0, 2, 10, 10))
        mainPanel?.apply {
            add(includeLineNumbersCheckBox)
            add(showPreviewCheckBox)
            add(exportToFileCheckBox)
            add(JLabel("Export File Path:"))
            add(exportFilePathField)
            add(includeMetadataCheckBox)
            add(autoProcessCheckBox)
            add(JLabel("Large File Threshold (bytes):"))
            add(largeFileThresholdField)
        }
        reset()
        return mainPanel
    }

    override fun isModified(): Boolean {
        val saved = ClipCraftSettings.getInstance().state
        return saved.includeLineNumbers != includeLineNumbersCheckBox.isSelected ||
                saved.showPreview != showPreviewCheckBox.isSelected ||
                saved.exportToFile != exportToFileCheckBox.isSelected ||
                saved.exportFilePath != exportFilePathField.text ||
                saved.includeMetadata != includeMetadataCheckBox.isSelected ||
                saved.autoProcess != autoProcessCheckBox.isSelected ||
                saved.largeFileThreshold.toString() != largeFileThresholdField.text
    }

    override fun apply() {
        val settings = ClipCraftSettings.getInstance()
        val opts = settings.state
        opts.includeLineNumbers = includeLineNumbersCheckBox.isSelected
        opts.showPreview = showPreviewCheckBox.isSelected
        opts.exportToFile = exportToFileCheckBox.isSelected
        opts.exportFilePath = exportFilePathField.text
        opts.includeMetadata = includeMetadataCheckBox.isSelected
        opts.autoProcess = autoProcessCheckBox.isSelected
        opts.largeFileThreshold = largeFileThresholdField.text.toLongOrNull() ?: opts.largeFileThreshold
        // Save back
        settings.loadState(opts)
    }

    override fun reset() {
        val opts = ClipCraftSettings.getInstance().state
        includeLineNumbersCheckBox.isSelected = opts.includeLineNumbers
        showPreviewCheckBox.isSelected = opts.showPreview
        exportToFileCheckBox.isSelected = opts.exportToFile
        exportFilePathField.text = opts.exportFilePath
        includeMetadataCheckBox.isSelected = opts.includeMetadata
        autoProcessCheckBox.isSelected = opts.autoProcess
        largeFileThresholdField.text = opts.largeFileThreshold.toString()
    }

    override fun disposeUIResources() {
        mainPanel = null
    }
}
