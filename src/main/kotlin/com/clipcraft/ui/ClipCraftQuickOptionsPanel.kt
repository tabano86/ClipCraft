package com.clipcraft.ui

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.OutputFormat
import com.clipcraft.services.ClipCraftSettings
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import java.awt.GridLayout
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * ClipCraftQuickOptionsPanel provides a compact UI that lets users rapidly
 * adjust a subset of plugin settings (like ignore lists, comment removal,
 * import removal, and output format) without opening the full settings.
 *
 * This unified version merges the old "preview" panel version too.
 */
class ClipCraftQuickOptionsPanel(
    initialOptions: ClipCraftOptions,
    private val project: Project?
) : JPanel(GridLayout(0, 2, 10, 10)) {

    // Basic fields
    private val ignoreFoldersField = JTextField(initialOptions.ignoreFolders.joinToString(", "))
    private val ignoreFilesField = JTextField(initialOptions.ignoreFiles.joinToString(", "))
    private val ignorePatternsField = JTextField(initialOptions.ignorePatterns.joinToString(", "))
    private val removeCommentsCheckBox = JCheckBox("Remove Comments", initialOptions.removeComments)
    private val trimLineWhitespaceCheckBox = JCheckBox("Trim Whitespace", initialOptions.trimLineWhitespace)

    // Additional quick toggles
    private val outputFormatComboBox = JComboBox(OutputFormat.values()).apply {
        selectedItem = initialOptions.outputFormat
    }
    private val removeImportsCheckBox = JCheckBox("Remove Import Statements", initialOptions.removeImports)

    // Button to open full settings
    private val openSettingsButton = JButton("Open Full Settings").apply {
        toolTipText = "Open the complete ClipCraft settings dialog."
        addActionListener {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, "ClipCraft")
        }
    }

    // Track changes
    private val changeListeners = mutableListOf<() -> Unit>()

    init {
        border = BorderFactory.createTitledBorder("Quick Options")

        add(JLabel("Ignore Folders:").apply { toolTipText = "Comma-separated folder names to ignore." })
        add(ignoreFoldersField)

        add(JLabel("Ignore Files:").apply { toolTipText = "Comma-separated file names to ignore." })
        add(ignoreFilesField)

        add(JLabel("Ignore Patterns:").apply { toolTipText = "Comma-separated glob patterns to ignore." })
        add(ignorePatternsField)

        add(removeCommentsCheckBox.apply { toolTipText = "Enable to remove comments from output." })
        add(trimLineWhitespaceCheckBox.apply { toolTipText = "Enable to trim trailing whitespace from lines." })

        add(JLabel("Output Format:"))
        add(outputFormatComboBox)

        add(removeImportsCheckBox)
        add(openSettingsButton)

        // Listen for changes
        val textFields = listOf(ignoreFoldersField, ignoreFilesField, ignorePatternsField)
        val checkBoxes = listOf(removeCommentsCheckBox, trimLineWhitespaceCheckBox, removeImportsCheckBox)
        textFields.forEach { field ->
            field.document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent?) = notifyChange()
                override fun removeUpdate(e: DocumentEvent?) = notifyChange()
                override fun changedUpdate(e: DocumentEvent?) = notifyChange()
            })
        }
        checkBoxes.forEach { cb ->
            cb.addActionListener { notifyChange() }
        }
        outputFormatComboBox.addActionListener { notifyChange() }
    }

    /**
     * Collect the current field values into an updated [ClipCraftOptions],
     * based on the active profile's baseline.
     */
    fun getOptions(): ClipCraftOptions {
        val currentOpts = ClipCraftSettings.getInstance().getActiveOptions()
        return currentOpts.copy(
            ignoreFolders = ignoreFoldersField.text.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            ignoreFiles = ignoreFilesField.text.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            ignorePatterns = ignorePatternsField.text.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            removeComments = removeCommentsCheckBox.isSelected,
            trimLineWhitespace = trimLineWhitespaceCheckBox.isSelected,
            outputFormat = outputFormatComboBox.selectedItem as OutputFormat,
            removeImports = removeImportsCheckBox.isSelected
        )
    }

    /**
     * Reset fields to the given [options].
     */
    fun resetFields(options: ClipCraftOptions) {
        ignoreFoldersField.text = options.ignoreFolders.joinToString(", ")
        ignoreFilesField.text = options.ignoreFiles.joinToString(", ")
        ignorePatternsField.text = options.ignorePatterns.joinToString(", ")
        removeCommentsCheckBox.isSelected = options.removeComments
        trimLineWhitespaceCheckBox.isSelected = options.trimLineWhitespace
        outputFormatComboBox.selectedItem = options.outputFormat
        removeImportsCheckBox.isSelected = options.removeImports
        notifyChange()
    }

    fun addChangeListener(listener: () -> Unit) {
        changeListeners += listener
    }

    private fun notifyChange() {
        changeListeners.forEach { it.invoke() }
    }

    /**
     * Checks if this panel’s fields differ from the given [currentOptions].
     */
    fun isModified(currentOptions: ClipCraftOptions): Boolean {
        val panelOpts = getOptions()
        // Compare the relevant fields
        if (panelOpts.ignoreFolders != currentOptions.ignoreFolders) return true
        if (panelOpts.ignoreFiles != currentOptions.ignoreFiles) return true
        if (panelOpts.ignorePatterns != currentOptions.ignorePatterns) return true
        if (panelOpts.removeComments != currentOptions.removeComments) return true
        if (panelOpts.trimLineWhitespace != currentOptions.trimLineWhitespace) return true
        if (panelOpts.outputFormat != currentOptions.outputFormat) return true
        if (panelOpts.removeImports != currentOptions.removeImports) return true
        return false
    }
}
