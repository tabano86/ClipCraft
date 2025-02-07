package com.clipcraft

import com.clipcraft.model.ClipCraftOptions
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import javax.swing.*
import java.awt.GridLayout

class ClipCraftConfigurable : Configurable {

    private val includeLineNumbersCheckBox = JCheckBox("Include Line Numbers")
    private val showPreviewCheckBox = JCheckBox("Show Preview")
    private val exportToFileCheckBox = JCheckBox("Export to File")
    private val exportFilePathField = JTextField(30)
    private val includeMetadataCheckBox = JCheckBox("Include File Metadata")
    private val autoProcessCheckBox = JCheckBox("No Prompt (Auto Process)")
    private val largeFileThresholdField = JTextField(10)
    private val singleBlockCheckBox = JCheckBox("Single Code Block")
    private val minimizeWhitespaceCheckBox = JCheckBox("Minimize Whitespace")

    private var mainPanel: JPanel? = null

    override fun getDisplayName(): String = "ClipCraft"

    override fun createComponent(): JComponent {
        mainPanel = JPanel(GridLayout(0, 2, 10, 10)).apply {
            add(includeLineNumbersCheckBox)
            add(showPreviewCheckBox)
            add(exportToFileCheckBox)
            add(JLabel("Export File Path:"))
            add(exportFilePathField)
            add(includeMetadataCheckBox)
            add(autoProcessCheckBox)
            add(JLabel("Large File Threshold (bytes):"))
            add(largeFileThresholdField)
            add(singleBlockCheckBox)
            add(minimizeWhitespaceCheckBox)
        }
        reset()
        return mainPanel!!
    }

    override fun isModified(): Boolean {
        val saved = ClipCraftSettings.getInstance().state
        return includeLineNumbersCheckBox.isSelected != saved.includeLineNumbers ||
                showPreviewCheckBox.isSelected != saved.showPreview ||
                exportToFileCheckBox.isSelected != saved.exportToFile ||
                exportFilePathField.text != saved.exportFilePath ||
                includeMetadataCheckBox.isSelected != saved.includeMetadata ||
                autoProcessCheckBox.isSelected != saved.autoProcess ||
                largeFileThresholdField.text != saved.largeFileThreshold.toString() ||
                singleBlockCheckBox.isSelected != saved.singleCodeBlock ||
                minimizeWhitespaceCheckBox.isSelected != saved.minimizeWhitespace
    }

    @Throws(ConfigurationException::class)
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
        opts.singleCodeBlock = singleBlockCheckBox.isSelected
        opts.minimizeWhitespace = minimizeWhitespaceCheckBox.isSelected
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
        singleBlockCheckBox.isSelected = opts.singleCodeBlock
        minimizeWhitespaceCheckBox.isSelected = opts.minimizeWhitespace
    }

    override fun disposeUIResources() {
        mainPanel = null
    }
}
