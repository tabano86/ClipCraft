package com.clipcraft.ui

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.OutputFormat
import com.intellij.openapi.ui.DialogWrapper
import java.awt.GridLayout
import javax.swing.*

class ClipCraftOptionsDialog(private val initialOptions: ClipCraftOptions) : DialogWrapper(true) {
    private val panel = JPanel(GridLayout(0, 2, 10, 10))

    private val includeLineNumbersCheckBox = JCheckBox("Include Line Numbers", initialOptions.includeLineNumbers).apply {
        toolTipText = "Prefix each line with its number."
    }
    private val showPreviewCheckBox = JCheckBox("Show Preview", initialOptions.showPreview).apply {
        toolTipText = "Display a preview dialog before copying."
    }
    private val exportToFileCheckBox = JCheckBox("Export to File", initialOptions.exportToFile).apply {
        toolTipText = "Save the output to a file."
    }
    private val exportFilePathField = JTextField(initialOptions.exportFilePath, 30).apply {
        toolTipText = "File path for exporting the output."
    }
    private val includeMetadataCheckBox = JCheckBox("Include File Metadata", initialOptions.includeMetadata).apply {
        toolTipText = "Display file size and last modified info."
    }
    private val outputFormatComboBox = JComboBox(OutputFormat.values()).apply {
        selectedItem = initialOptions.outputFormat
        toolTipText = "Select output format: Markdown, Plain, or HTML."
    }
    private val removeImportsCheckBox = JCheckBox("Remove Import Statements", initialOptions.removeImports).apply {
        toolTipText = "Remove import statements from the code."
    }
    private val ignoreFoldersField = JTextField(initialOptions.ignoreFolders.joinToString(","), 30).apply {
        toolTipText = "Comma-separated folder names to ignore."
    }
    private val ignoreFilesField = JTextField(initialOptions.ignoreFiles.joinToString(","), 30).apply {
        toolTipText = "Comma-separated file names to ignore."
    }
    private val ignorePatternsField = JTextField(initialOptions.ignorePatterns.joinToString(","), 30).apply {
        toolTipText = "Comma-separated regex patterns to ignore files."
    }
    private val removeCommentsCheckBox = JCheckBox("Remove Comments", initialOptions.removeComments).apply {
        toolTipText = "Strip out comment lines from the code."
    }
    private val trimLineWhitespaceCheckBox = JCheckBox("Trim Trailing Whitespace", initialOptions.trimLineWhitespace).apply {
        toolTipText = "Remove trailing spaces from each line."
    }

    init {
        title = "ClipCraft Options"
        panel.add(includeLineNumbersCheckBox)
        panel.add(showPreviewCheckBox)
        panel.add(exportToFileCheckBox)
        panel.add(JLabel("Export File Path:"))
        panel.add(exportFilePathField)
        panel.add(includeMetadataCheckBox)
        panel.add(JLabel("Output Format:"))
        panel.add(outputFormatComboBox)
        panel.add(removeImportsCheckBox)

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
            trimLineWhitespace = trimLineWhitespaceCheckBox.isSelected,
            removeImports = removeImportsCheckBox.isSelected,
            outputFormat = outputFormatComboBox.selectedItem as? OutputFormat ?: initialOptions.outputFormat,
            // Unchanged fields for options not handled in this dialog:
            themeMode = initialOptions.themeMode,
            customTemplates = initialOptions.customTemplates,
            enableDragAndDrop = initialOptions.enableDragAndDrop,
            filterRegex = initialOptions.filterRegex,
            macros = initialOptions.macros,
            simultaneousExports = initialOptions.simultaneousExports,
            displayGitMetadata = initialOptions.displayGitMetadata,
            syntaxHighlightPreview = initialOptions.syntaxHighlightPreview,
            showProgressInStatusBar = initialOptions.showProgressInStatusBar,
            shareToGistEnabled = initialOptions.shareToGistEnabled,
            exportToCloudServices = initialOptions.exportToCloudServices,
            measurePerformance = initialOptions.measurePerformance,
            perProjectConfig = initialOptions.perProjectConfig,
            locale = initialOptions.locale,
            enableFeedbackButton = initialOptions.enableFeedbackButton,
            enableNotificationCenter = initialOptions.enableNotificationCenter
        )
    }
}
