package com.clipcraft.ui

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.OutputFormat
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.JCheckBox
import javax.swing.JComboBox

/**
 * A full options dialog for advanced configuration.
 * The UI is divided into Basic Options (most-used settings) and Advanced Options.
 */
class ClipCraftOptionsDialog(private val initialOptions: ClipCraftOptions) : DialogWrapper(true) {

    private val mainPanel: JPanel

    // Basic Options
    private val includeLineNumbersCheckBox = JCheckBox("Include Line Numbers", initialOptions.includeLineNumbers).apply {
        toolTipText = "When enabled, each line will be prefixed with its line number."
    }
    private val showPreviewCheckBox = JCheckBox("Show Preview", initialOptions.showPreview).apply {
        toolTipText = "Display a preview dialog before copying output to clipboard."
    }
    private val exportToFileCheckBox = JCheckBox("Export to File", initialOptions.exportToFile).apply {
        toolTipText = "When enabled, output is saved to a file instead of copying to clipboard."
    }
    private val exportFilePathField = JTextField(initialOptions.exportFilePath, 30).apply {
        toolTipText = "File path to export output (if left blank, defaults to project directory)."
    }
    private val includeMetadataCheckBox = JCheckBox("Include File Metadata", initialOptions.includeMetadata).apply {
        toolTipText = "Include file size and last modified date in the output header."
    }
    private val outputFormatComboBox = JComboBox(OutputFormat.values()).apply {
        selectedItem = initialOptions.outputFormat
        toolTipText = "Select the output format (Markdown, Plain, or HTML)."
    }
    private val removeImportsCheckBox = JCheckBox("Remove Import Statements", initialOptions.removeImports).apply {
        toolTipText = "Strips import statements from code to reduce clutter."
    }

    // Advanced Options
    private val ignoreFoldersField = JTextField(initialOptions.ignoreFolders.joinToString(","), 30).apply {
        toolTipText = "List folder names to ignore (comma-separated). Default: .git, build, out, node_modules"
    }
    private val ignoreFilesField = JTextField(initialOptions.ignoreFiles.joinToString(","), 30).apply {
        toolTipText = "List file names to ignore (comma-separated)."
    }
    private val ignorePatternsField = JTextField(initialOptions.ignorePatterns.joinToString(","), 30).apply {
        toolTipText = "Enter regex patterns to ignore files (comma-separated)."
    }
    private val removeCommentsCheckBox = JCheckBox("Remove Comments", initialOptions.removeComments).apply {
        toolTipText = "Strip out comments from the code output."
    }
    private val trimLineWhitespaceCheckBox = JCheckBox("Trim Trailing Whitespace", initialOptions.trimLineWhitespace).apply {
        toolTipText = "Remove extra spaces at the end of each line."
    }

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
            .panel.also { it.border = BorderFactory.createTitledBorder("Basic Options") }

        val advancedPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JLabel("Ignore Folders:"), ignoreFoldersField, 1, false)
            .addLabeledComponent(JLabel("Ignore Files:"), ignoreFilesField, 1, false)
            .addLabeledComponent(JLabel("Ignore Patterns:"), ignorePatternsField, 1, false)
            .addLabeledComponent(JLabel("Remove Comments:"), removeCommentsCheckBox, 1, false)
            .addLabeledComponent(JLabel("Trim Whitespace:"), trimLineWhitespaceCheckBox, 1, false)
            .panel.also { it.border = BorderFactory.createTitledBorder("Advanced Options") }

        mainPanel = FormBuilder.createFormBuilder()
            .addComponent(basicPanel)
            .addVerticalGap(10)
            .addComponent(advancedPanel)
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
            trimLineWhitespace = trimLineWhitespaceCheckBox.isSelected
        )
    }
}
