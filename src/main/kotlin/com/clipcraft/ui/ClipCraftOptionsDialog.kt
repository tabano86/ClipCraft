package com.clipcraft.ui

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.OutputFormat
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import javax.swing.*

class ClipCraftOptionsDialog(private val initialOptions: ClipCraftOptions) : DialogWrapper(true) {

    private val mainPanel: JPanel

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

    // Some next-gen fields
    private val chunkingCheckBox = JCheckBox("Enable GPT Chunking", initialOptions.enableChunkingForGPT)
    private val chunkSizeField = JTextField(initialOptions.maxChunkSize.toString(), 5)
    private val directorySummaryCheckBox = JCheckBox("Include Directory Summary", initialOptions.includeDirectorySummary)
    private val collapseBlankLinesCheckBox = JCheckBox("Collapse Consecutive Blank Lines", initialOptions.collapseBlankLines)
    private val removeLeadingBlankCheckBox = JCheckBox("Remove Leading Blank Lines", initialOptions.removeLeadingBlankLines)
    private val singleLineCheckBox = JCheckBox("Single Line Output", initialOptions.singleLineOutput)

    init {
        title = "ClipCraft Options"
        val basicPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JLabel("Include Line Numbers:"), includeLineNumbersCheckBox, 1, false)
            .addLabeledComponent(JLabel("Show Preview:"), showPreviewCheckBox, 1, false)
            .addLabeledComponent(JLabel("Export to File:"), exportToFileCheckBox, 1, false)
            .addLabeledComponent(JLabel("Export File Path:"), exportFilePathField, 1, false)
            .addLabeledComponent(JLabel("Include Metadata:"), includeMetadataCheckBox, 1, false)
            .addLabeledComponent(JLabel("Output Format:"), outputFormatComboBox, 1, false)
            .addLabeledComponent(JLabel("Remove Imports:"), removeImportsCheckBox, 1, false)
            .panel.apply {
                border = BorderFactory.createTitledBorder("Basic Options")
            }

        val advancedPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JLabel("Ignore Folders:"), ignoreFoldersField, 1, false)
            .addLabeledComponent(JLabel("Ignore Files:"), ignoreFilesField, 1, false)
            .addLabeledComponent(JLabel("Ignore Patterns:"), ignorePatternsField, 1, false)
            .addLabeledComponent(JLabel("Remove Comments:"), removeCommentsCheckBox, 1, false)
            .addLabeledComponent(JLabel("Trim Whitespace:"), trimLineWhitespaceCheckBox, 1, false)
            .panel.apply {
                border = BorderFactory.createTitledBorder("Advanced Options")
            }

        val nextGenPanel = FormBuilder.createFormBuilder()
            .addComponent(JLabel("Next-Gen Settings:"))
            .addComponent(chunkingCheckBox)
            .addLabeledComponent(JLabel("Max Chunk Size:"), chunkSizeField)
            .addComponent(directorySummaryCheckBox)
            .addComponent(collapseBlankLinesCheckBox)
            .addComponent(removeLeadingBlankCheckBox)
            .addComponent(singleLineCheckBox)
            .panel.apply {
                border = BorderFactory.createTitledBorder("Next-Gen Features")
            }

        mainPanel = FormBuilder.createFormBuilder()
            .addComponent(basicPanel)
            .addVerticalGap(10)
            .addComponent(advancedPanel)
            .addVerticalGap(10)
            .addComponent(nextGenPanel)
            .panel

        init()
    }

    override fun createCenterPanel(): JComponent = mainPanel

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
            trimLineWhitespace = trimLineWhitespaceCheckBox.isSelected,

            enableChunkingForGPT = chunkingCheckBox.isSelected,
            maxChunkSize = chunkSizeField.text.toIntOrNull() ?: initialOptions.maxChunkSize,
            includeDirectorySummary = directorySummaryCheckBox.isSelected,
            collapseBlankLines = collapseBlankLinesCheckBox.isSelected,
            removeLeadingBlankLines = removeLeadingBlankCheckBox.isSelected,
            singleLineOutput = singleLineCheckBox.isSelected
        )
    }
}
