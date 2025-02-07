package com.clipcraft

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import javax.swing.*

/**
 * Settings page for ClipCraft.
 * Provides a full-featured UI with tooltips and clear labels.
 */
class ClipCraftConfigurable : Configurable {
    private val panel = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }

    // Basic Options
    private val includeLineNumbersCheckBox = JCheckBox("Include Line Numbers").apply {
        toolTipText = "Prefix each line with its line number."
    }
    private val showPreviewCheckBox = JCheckBox("Show Preview").apply {
        toolTipText = "Display a preview dialog of the copied code."
    }
    private val exportToFileCheckBox = JCheckBox("Export to File").apply {
        toolTipText = "Save the copied code to a file instead of the clipboard."
    }
    private val exportFilePathField = JTextField(30).apply {
        toolTipText = "File path to export the copied code (default: project directory/clipcraft_output.txt)."
    }
    private val includeMetadataCheckBox = JCheckBox("Include File Metadata").apply {
        toolTipText = "Include file size and last modified timestamp in the header."
    }
    private val autoProcessCheckBox = JCheckBox("Automatically Process (No Prompt)").apply {
        toolTipText = "Process files immediately without prompting for options."
    }
    private val largeFileThresholdField = JTextField(10).apply {
        toolTipText = "Threshold (in bytes) above which files are loaded with a progress indicator."
    }
    private val singleCodeBlockCheckBox = JCheckBox("Merge into One Code Block").apply {
        toolTipText = "Merge all file outputs into a single code block."
    }
    private val minimizeWhitespaceCheckBox = JCheckBox("Minimize Blank Lines").apply {
        toolTipText = "Collapse consecutive blank lines in the output."
    }

    // Advanced Options
    private val ignoreFoldersField = JTextField(30).apply {
        toolTipText = "Comma-separated folder names to ignore (e.g. .git, build)."
    }
    private val ignoreFilesField = JTextField(30).apply {
        toolTipText = "Comma-separated file names to ignore."
    }
    private val ignorePatternsField = JTextField(30).apply {
        toolTipText = "Comma-separated regex patterns; matching file names will be ignored."
    }
    private val removeCommentsCheckBox = JCheckBox("Remove Comments").apply {
        toolTipText = "Strip out comment lines from source code."
    }
    private val trimLineWhitespaceCheckBox = JCheckBox("Trim Trailing Whitespace").apply {
        toolTipText = "Remove trailing spaces from each line (preserves indentation)."
    }

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

        val advancedPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createTitledBorder("Advanced Options")
        }
        advancedPanel.add(JLabel("Ignore Folders (comma separated):"))
        advancedPanel.add(ignoreFoldersField)
        advancedPanel.add(JLabel("Ignore Files (comma separated):"))
        advancedPanel.add(ignoreFilesField)
        advancedPanel.add(JLabel("Ignore Patterns (regex, comma separated):"))
        advancedPanel.add(ignorePatternsField)
        advancedPanel.add(removeCommentsCheckBox)
        advancedPanel.add(trimLineWhitespaceCheckBox)

        panel.add(Box.createVerticalStrut(10))
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
        // Dispose of UI resources if necessary.
    }
}
