package com.clipcraft.ui

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.OutputFormat
import com.clipcraft.services.ClipCraftSettings
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import org.slf4j.LoggerFactory
import java.awt.GridLayout
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * Compact UI for quick changes to a subset of plugin settings,
 * with local logging for clarity.
 */
class ClipCraftQuickOptionsPanel(
    initialOptions: ClipCraftOptions,
    private val project: Project?
) : JPanel(GridLayout(0, 2, 10, 10)) {

    private val log = LoggerFactory.getLogger(ClipCraftQuickOptionsPanel::class.java)

    // Basic
    val ignoreFoldersField = JTextField(initialOptions.ignoreFolders.joinToString(", "))
    val ignoreFilesField = JTextField(initialOptions.ignoreFiles.joinToString(", "))
    val ignorePatternsField = JTextField(initialOptions.ignorePatterns.joinToString(", "))
    val removeCommentsCheckBox = JCheckBox("Remove Comments", initialOptions.removeComments)
    val trimLineWhitespaceCheckBox = JCheckBox("Trim Whitespace", initialOptions.trimLineWhitespace)

    private val outputFormatComboBox = JComboBox(OutputFormat.values()).apply {
        selectedItem = initialOptions.outputFormat
    }
    private val removeImportsCheckBox = JCheckBox("Remove Import Statements", initialOptions.removeImports)

    private val openSettingsButton = JButton("Open Full Settings").apply {
        toolTipText = "Open the complete ClipCraft settings dialog."
        addActionListener {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, "ClipCraft")
        }
    }

    private val changeListeners = mutableListOf<() -> Unit>()

    init {
        border = BorderFactory.createTitledBorder("Quick Options")

        add(JLabel("Ignore Folders:"))
        add(ignoreFoldersField)

        add(JLabel("Ignore Files:"))
        add(ignoreFilesField)

        add(JLabel("Ignore Patterns:"))
        add(ignorePatternsField)

        add(removeCommentsCheckBox)
        add(trimLineWhitespaceCheckBox)

        add(JLabel("Output Format:"))
        add(outputFormatComboBox)

        add(removeImportsCheckBox)
        add(openSettingsButton)

        val textFields = listOf(ignoreFoldersField, ignoreFilesField, ignorePatternsField)
        textFields.forEach { field ->
            field.document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent?) = notifyChange()
                override fun removeUpdate(e: DocumentEvent?) = notifyChange()
                override fun changedUpdate(e: DocumentEvent?) = notifyChange()
            })
        }
        val checkBoxes = listOf(removeCommentsCheckBox, trimLineWhitespaceCheckBox, removeImportsCheckBox)
        checkBoxes.forEach { cb -> cb.addActionListener { notifyChange() } }
        outputFormatComboBox.addActionListener { notifyChange() }
    }

    fun getOptions(): ClipCraftOptions {
        log.debug("Gathering options from QuickOptionsPanel")
        val currentOpts = ClipCraftSettings.getInstance().getActiveOptions()
        return currentOpts.copy(
            ignoreFolders = parseCommaList(ignoreFoldersField.text),
            ignoreFiles = parseCommaList(ignoreFilesField.text),
            ignorePatterns = parseCommaList(ignorePatternsField.text),
            removeComments = removeCommentsCheckBox.isSelected,
            trimLineWhitespace = trimLineWhitespaceCheckBox.isSelected,
            outputFormat = outputFormatComboBox.selectedItem as OutputFormat,
            removeImports = removeImportsCheckBox.isSelected
        )
    }

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

    fun isModified(currentOptions: ClipCraftOptions): Boolean {
        val panelOpts = getOptions()
        return panelOpts.ignoreFolders != currentOptions.ignoreFolders ||
                panelOpts.ignoreFiles != currentOptions.ignoreFiles ||
                panelOpts.ignorePatterns != currentOptions.ignorePatterns ||
                panelOpts.removeComments != currentOptions.removeComments ||
                panelOpts.trimLineWhitespace != currentOptions.trimLineWhitespace ||
                panelOpts.outputFormat != currentOptions.outputFormat ||
                panelOpts.removeImports != currentOptions.removeImports
    }

    fun addChangeListener(listener: () -> Unit) {
        changeListeners += listener
    }

    private fun notifyChange() {
        changeListeners.forEach { it() }
    }

    private fun parseCommaList(input: String): List<String> =
        input.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}
