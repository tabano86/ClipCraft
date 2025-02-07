package com.clipcraft

import com.clipcraft.model.ClipCraftOptions
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import java.awt.GridLayout
import javax.swing.*

class ClipCraftConfigurable : Configurable {

    private val panel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }

    private val includeLineNumbersCheckBox = JCheckBox("Include Line Numbers")
    private val showPreviewCheckBox = JCheckBox("Show Preview")
    private val exportToFileCheckBox = JCheckBox("Export to File")
    private val exportFilePathField = JTextField(30)
    private val includeMetadataCheckBox = JCheckBox("Include File Metadata")
    private val autoProcessCheckBox = JCheckBox("Automatically Process (No Prompt)")
    private val largeFileThresholdField = JTextField(10)
    private val singleCodeBlockCheckBox = JCheckBox("Merge everything into one code block")
    private val minimizeWhitespaceCheckBox = JCheckBox("Minimize consecutive blank lines")

    // Advanced options components.
    private val ignoreFoldersField = JTextField(30)
    private val ignoreFilesField = JTextField(30)
    private val ignorePatternsField = JTextField(30)
    private val removeCommentsCheckBox = JCheckBox("Remove Comments")
    private val trimLineWhitespaceCheckBox = JCheckBox("Trim Line Whitespace")

    init {
        panel.add(includeLineNumbersCheckBox)
        panel.add(showPreviewCheckBox)
        panel.add(exportToFileCheckBox)
        panel.add(JLabel("Export File Path:"))
        panel.add(exportFilePathField)
        panel.add(includeMetadataCheckBox)
        panel.add(autoProcessCheckBox)
        panel.add(JLabel("Large File Threshold (bytes):"))
        panel.add(largeFileThresholdField)
        panel.add(singleCodeBlockCheckBox)
        panel.add(minimizeWhitespaceCheckBox)

        // Advanced options panel with a titled border.
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
    }

    override fun getDisplayName(): String = "ClipCraft"

    override fun createComponent(): JComponent {
        reset()
        return panel
    }

    override fun isModified(): Boolean {
        val opts = ClipCraftSettings.getInstance().state
        return includeLineNumbersCheckBox.isSelected != opts.includeLineNumbers ||
                showPreviewCheckBox.isSelected != opts.showPreview ||
                exportToFileCheckBox.isSelected != opts.exportToFile ||
                exportFilePathField.text != opts.exportFilePath ||
                includeMetadataCheckBox.isSelected != opts.includeMetadata ||
                autoProcessCheckBox.isSelected != opts.autoProcess ||
                largeFileThresholdField.text != opts.largeFileThreshold.toString() ||
                singleCodeBlockCheckBox.isSelected != opts.singleCodeBlock ||
                minimizeWhitespaceCheckBox.isSelected != opts.minimizeWhitespace ||
                ignoreFoldersField.text != opts.ignoreFolders.joinToString(",") ||
                ignoreFilesField.text != opts.ignoreFiles.joinToString(",") ||
                ignorePatternsField.text != opts.ignorePatterns.joinToString(",") ||
                removeCommentsCheckBox.isSelected != opts.removeComments ||
                trimLineWhitespaceCheckBox.isSelected != opts.trimLineWhitespace
    }

    @Throws(ConfigurationException::class)
    override fun apply() {
        val opts = ClipCraftSettings.getInstance().state
        opts.includeLineNumbers = includeLineNumbersCheckBox.isSelected
        opts.showPreview = showPreviewCheckBox.isSelected
        opts.exportToFile = exportToFileCheckBox.isSelected
        opts.exportFilePath = exportFilePathField.text
        opts.includeMetadata = includeMetadataCheckBox.isSelected
        opts.autoProcess = autoProcessCheckBox.isSelected
        opts.largeFileThreshold = largeFileThresholdField.text.toLongOrNull() ?: opts.largeFileThreshold
        opts.singleCodeBlock = singleCodeBlockCheckBox.isSelected
        opts.minimizeWhitespace = minimizeWhitespaceCheckBox.isSelected

        opts.ignoreFolders = ignoreFoldersField.text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        opts.ignoreFiles = ignoreFilesField.text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        opts.ignorePatterns = ignorePatternsField.text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        opts.removeComments = removeCommentsCheckBox.isSelected
        opts.trimLineWhitespace = trimLineWhitespaceCheckBox.isSelected

        ClipCraftSettings.getInstance().loadState(opts)
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
        singleCodeBlockCheckBox.isSelected = opts.singleCodeBlock
        minimizeWhitespaceCheckBox.isSelected = opts.minimizeWhitespace

        ignoreFoldersField.text = opts.ignoreFolders.joinToString(",")
        ignoreFilesField.text = opts.ignoreFiles.joinToString(",")
        ignorePatternsField.text = opts.ignorePatterns.joinToString(",")
        removeCommentsCheckBox.isSelected = opts.removeComments
        trimLineWhitespaceCheckBox.isSelected = opts.trimLineWhitespace
    }

    override fun disposeUIResources() {
        // nothing special to dispose
    }
}
