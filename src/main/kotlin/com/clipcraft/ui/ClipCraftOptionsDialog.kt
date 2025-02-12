package com.clipcraft.ui

import com.clipcraft.model.ChunkStrategy
import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.ConcurrencyMode
import com.clipcraft.model.OutputFormat
import com.clipcraft.model.OverlapStrategy
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField
import org.slf4j.LoggerFactory

/**
 * A simple example of a dialog to edit ClipCraftOptions, not necessarily used in production.
 */
class ClipCraftOptionsDialog(private val initialOptions: ClipCraftOptions) : DialogWrapper(true) {

    private val log = LoggerFactory.getLogger(ClipCraftOptionsDialog::class.java)
    private val mainPanel: JPanel

    private val includeLineNumbersCheckBox = JCheckBox("Include Line Numbers", initialOptions.includeLineNumbers)
    private val autoDetectLanguageCheckBox = JCheckBox("Auto-Detect Language?", initialOptions.autoDetectLanguage)
    private val showPreviewCheckBox = JCheckBox("Show Preview", false)
    private val exportToFileCheckBox = JCheckBox("Export to File", false)
    private val exportFilePathField = JTextField("", 30)
    private val includeMetadataCheckBox = JCheckBox("Include File Metadata", initialOptions.includeMetadata)
    private val outputFormatComboBox = ComboBox(OutputFormat.values()).apply {
        selectedItem = initialOptions.outputFormat
    }
    private val removeImportsCheckBox = JCheckBox("Remove Import Statements", initialOptions.removeImports)
    private val removeCommentsCheckBox = JCheckBox("Remove Comments", initialOptions.removeComments)
    private val trimLineWhitespaceCheckBox = JCheckBox("Trim Trailing Whitespace", initialOptions.trimWhitespace)
    private val concurrencyModeComboBox = ComboBox(ConcurrencyMode.values()).apply {
        selectedItem = initialOptions.concurrencyMode
    }
    private val maxTasksField = JTextField(initialOptions.maxConcurrentTasks.toString(), 5)
    private val singleLineOutputCheckBox = JCheckBox("Single-line Output", initialOptions.singleLineOutput)
    private val chunkStrategyComboBox = ComboBox(ChunkStrategy.values()).apply {
        selectedItem = initialOptions.chunkStrategy
    }
    private val chunkSizeField = JTextField(initialOptions.chunkSize.toString(), 5)
    private val overlapStrategyComboBox = ComboBox(OverlapStrategy.values()).apply {
        selectedItem = initialOptions.overlapStrategy
    }

    init {
        title = "ClipCraft Options"

        val basicPanel = FormBuilder.createFormBuilder()
            .addComponent(includeLineNumbersCheckBox)
            .addComponent(autoDetectLanguageCheckBox)
            .addComponent(showPreviewCheckBox)
            .addComponent(exportToFileCheckBox)
            .addLabeledComponent("Export File Path:", exportFilePathField)
            .addComponent(includeMetadataCheckBox)
            .addLabeledComponent("Output Format:", outputFormatComboBox)
            .addComponent(removeImportsCheckBox)
            .addComponent(removeCommentsCheckBox)
            .addComponent(trimLineWhitespaceCheckBox)
            .panel
        basicPanel.border = BorderFactory.createTitledBorder("Basic Options")

        val concurrencyPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Concurrency Mode:", concurrencyModeComboBox)
            .addLabeledComponent("Max Concurrent Tasks:", maxTasksField)
            .panel
        concurrencyPanel.border = BorderFactory.createTitledBorder("Concurrency")

        val chunkPanel = FormBuilder.createFormBuilder()
            .addComponent(singleLineOutputCheckBox)
            .addLabeledComponent("Chunk Strategy:", chunkStrategyComboBox)
            .addLabeledComponent("Chunk Size:", chunkSizeField)
            .addLabeledComponent("Overlap Strategy:", overlapStrategyComboBox)
            .panel
        chunkPanel.border = BorderFactory.createTitledBorder("Chunk / Overlap")

        mainPanel = JPanel(BorderLayout())
        val topBox = Box.createVerticalBox()
        topBox.add(basicPanel)
        topBox.add(concurrencyPanel)
        topBox.add(chunkPanel)

        mainPanel.add(topBox, BorderLayout.NORTH)
        init()
    }

    override fun createCenterPanel(): JComponent = mainPanel

    fun getOptions(): ClipCraftOptions {
        val tasks = maxTasksField.text.toIntOrNull() ?: initialOptions.maxConcurrentTasks
        log.info("User selected concurrency: $tasks tasks")

        val updated = initialOptions.copy(
            includeLineNumbers = includeLineNumbersCheckBox.isSelected,
            autoDetectLanguage = autoDetectLanguageCheckBox.isSelected,
            includeMetadata = includeMetadataCheckBox.isSelected,
            outputFormat = outputFormatComboBox.selectedItem as OutputFormat,
            removeImports = removeImportsCheckBox.isSelected,
            removeComments = removeCommentsCheckBox.isSelected,
            trimWhitespace = trimLineWhitespaceCheckBox.isSelected,
            concurrencyMode = concurrencyModeComboBox.selectedItem as ConcurrencyMode,
            maxConcurrentTasks = if (tasks < 1) 1 else tasks,
            singleLineOutput = singleLineOutputCheckBox.isSelected,
            chunkStrategy = chunkStrategyComboBox.selectedItem as ChunkStrategy,
            chunkSize = chunkSizeField.text.toIntOrNull() ?: initialOptions.chunkSize,
            overlapStrategy = overlapStrategyComboBox.selectedItem as OverlapStrategy
        )

        updated.resolveConflicts()
        return updated
    }
}
