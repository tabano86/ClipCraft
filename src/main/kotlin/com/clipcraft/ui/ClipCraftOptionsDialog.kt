package com.clipcraft.ui

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.OutputFormat
import com.intellij.openapi.ui.DialogWrapper
import org.slf4j.LoggerFactory
import com.intellij.util.ui.FormBuilder
import javax.swing.*

class ClipCraftOptionsDialog(private val initialOptions: ClipCraftOptions) : DialogWrapper(true) {

    private val log = LoggerFactory.getLogger(ClipCraftOptionsDialog::class.java)
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

    // Next-gen options
    private val ignoreFoldersField = JTextField(initialOptions.ignoreFolders.joinToString(", "), 30)
    private val ignoreFilesField = JTextField(initialOptions.ignoreFiles.joinToString(", "), 30)
    private val ignorePatternsField = JTextField(initialOptions.ignorePatterns.joinToString(", "), 30)
    private val removeCommentsCheckBox = JCheckBox("Remove Comments", initialOptions.removeComments)
    private val trimLineWhitespaceCheckBox = JCheckBox("Trim Trailing Whitespace", initialOptions.trimLineWhitespace)

    private val chunkingCheckBox = JCheckBox("Enable GPT Chunking", initialOptions.enableChunkingForGPT)
    private val chunkSizeField = JTextField(initialOptions.maxChunkSize.toString(), 5)
    private val directorySummaryCheckBox = JCheckBox("Include Directory Summary", initialOptions.includeDirectorySummary)
    private val collapseBlankLinesCheckBox = JCheckBox("Collapse Consecutive Blank Lines", initialOptions.collapseBlankLines)
    private val removeLeadingBlankCheckBox = JCheckBox("Remove Leading Blank Lines", initialOptions.removeLeadingBlankLines)
    private val singleLineCheckBox = JCheckBox("Single Line Output", initialOptions.singleLineOutput)

    init {
        title = "ClipCraft Options"
        val basicPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JLabel("Include Line Numbers:"), includeLineNumbersCheckBox)
            .addLabeledComponent(JLabel("Show Preview:"), showPreviewCheckBox)
            .addLabeledComponent(JLabel("Export to File:"), exportToFileCheckBox)
            .addLabeledComponent(JLabel("Export File Path:"), exportFilePathField)
            .addLabeledComponent(JLabel("Include Metadata:"), includeMetadataCheckBox)
            .addLabeledComponent(JLabel("Output Format:"), outputFormatComboBox)
            .addLabeledComponent(JLabel("Remove Imports:"), removeImportsCheckBox)
            .panel.apply { border = BorderFactory.createTitledBorder("Basic Options") }

        val advancedPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JLabel("Ignore Folders:"), ignoreFoldersField)
            .addLabeledComponent(JLabel("Ignore Files:"), ignoreFilesField)
            .addLabeledComponent(JLabel("Ignore Patterns:"), ignorePatternsField)
            .addLabeledComponent(JLabel("Remove Comments:"), removeCommentsCheckBox)
            .addLabeledComponent(JLabel("Trim Whitespace:"), trimLineWhitespaceCheckBox)
            .panel.apply { border = BorderFactory.createTitledBorder("Advanced Options") }

        val nextGenPanel = FormBuilder.createFormBuilder()
            .addComponent(JLabel("Next-Gen Settings:"))
            .addComponent(chunkingCheckBox)
            .addLabeledComponent(JLabel("Max Chunk Size:"), chunkSizeField)
            .addComponent(directorySummaryCheckBox)
            .addComponent(collapseBlankLinesCheckBox)
            .addComponent(removeLeadingBlankCheckBox)
            .addComponent(singleLineCheckBox)
            .panel.apply { border = BorderFactory.createTitledBorder("Next-Gen Features") }

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
        val chunk = chunkSizeField.text.toIntOrNull() ?: initialOptions.maxChunkSize
        log.info("User selected chunk size: $chunk")
        return initialOptions.copy(
            includeLineNumbers = includeLineNumbersCheckBox.isSelected,
            showPreview = showPreviewCheckBox.isSelected,
            exportToFile = exportToFileCheckBox.isSelected,
            exportFilePath = exportFilePathField.text,
            includeMetadata = includeMetadataCheckBox.isSelected,
            outputFormat = outputFormatComboBox.selectedItem as OutputFormat,
            removeImports = removeImportsCheckBox.isSelected,
            ignoreFolders = parseCommaList(ignoreFoldersField.text),
            ignoreFiles = parseCommaList(ignoreFilesField.text),
            ignorePatterns = parseCommaList(ignorePatternsField.text),
            removeComments = removeCommentsCheckBox.isSelected,
            trimLineWhitespace = trimLineWhitespaceCheckBox.isSelected,
            enableChunkingForGPT = chunkingCheckBox.isSelected,
            maxChunkSize = if (chunk <= 0) 3000 else chunk,
            includeDirectorySummary = directorySummaryCheckBox.isSelected,
            collapseBlankLines = collapseBlankLinesCheckBox.isSelected,
            removeLeadingBlankLines = removeLeadingBlankCheckBox.isSelected,
            singleLineOutput = singleLineCheckBox.isSelected
        )
    }

    private fun parseCommaList(input: String): List<String> =
        input.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}
