package com.clipcraft.ui

import com.clipcraft.model.ChunkStrategy
import com.clipcraft.model.CompressionMode
import com.clipcraft.model.ConcurrencyMode
import com.clipcraft.model.OverlapStrategy
import com.clipcraft.model.ThemeMode
import com.clipcraft.services.ClipCraftProfileManager
import com.clipcraft.util.CodeFormatter
import com.intellij.openapi.options.Configurable
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class ClipCraftSettingsConfigurable : Configurable {
    private val manager = ClipCraftProfileManager()
    private val savedProfile = manager.currentProfile().copy()
    private val options = savedProfile.options
    private val sampleCode = """// Example
package test
fun main() {
    println("Hello")
}"""

    private lateinit var mainPanel: JPanel
    private lateinit var previewArea: JTextArea

    // Existing fields
    private lateinit var headerField: JTextField
    private lateinit var footerField: JTextField
    private lateinit var directoryStructureCheck: JCheckBox
    private lateinit var lineNumbersCheck: JCheckBox
    private lateinit var removeImportsCheck: JCheckBox
    private lateinit var removeCommentsCheck: JCheckBox
    private lateinit var trimWhitespaceCheck: JCheckBox
    private lateinit var removeEmptyLinesCheck: JCheckBox
    private lateinit var singleLineOutputCheck: JCheckBox
    private lateinit var chunkStrategyCombo: JComboBox<ChunkStrategy>
    private lateinit var chunkSizeField: JTextField
    private lateinit var overlapCombo: JComboBox<OverlapStrategy>
    private lateinit var compressionCombo: JComboBox<CompressionMode>
    private lateinit var metadataCheck: JCheckBox
    private lateinit var gitInfoCheck: JCheckBox
    private lateinit var autoLangCheck: JCheckBox
    private lateinit var themeCombo: JComboBox<ThemeMode>
    private lateinit var concurrencyCombo: JComboBox<ConcurrencyMode>
    private lateinit var maxTasksField: JTextField
    private lateinit var gitIgnoreCheck: JCheckBox
    private lateinit var additionalIgnoresField: JTextField
    private lateinit var invertIgnoresCheck: JCheckBox
    private lateinit var directoryPatternCheck: JCheckBox
    private lateinit var detectBinaryCheck: JCheckBox
    private lateinit var binaryThresholdField: JTextField
    private lateinit var showLintCheck: JCheckBox

    // For chunking panel
    private lateinit var chunkLabel: JLabel

    // New options controls
    private lateinit var includeImageFilesCheck: JCheckBox
    private lateinit var lintErrorsOnlyCheck: JCheckBox
    private lateinit var lintWarningsOnlyCheck: JCheckBox
    private lateinit var addSnippetToQueueCheck: JCheckBox

    private val docListener = object : DocumentListener {
        override fun insertUpdate(e: DocumentEvent?) = updatePreview()
        override fun removeUpdate(e: DocumentEvent?) = updatePreview()
        override fun changedUpdate(e: DocumentEvent?) = updatePreview()
    }

    override fun getDisplayName() = "ClipCraft"

    override fun createComponent(): JComponent {
        // Outer panel for entire settings UI
        mainPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(16) // extra spacing around everything
        }

        // Form panel with sections
        val formPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(12) // spacing within the form itself
            add(panelOutput())
            add(Box.createVerticalStrut(JBUI.scale(12)))
            add(panelFormatting())
            add(Box.createVerticalStrut(JBUI.scale(12)))
            add(panelChunking())
            add(Box.createVerticalStrut(JBUI.scale(12)))
            add(panelMetadata())
            add(Box.createVerticalStrut(JBUI.scale(12)))
            add(panelConcurrency())
            add(Box.createVerticalStrut(JBUI.scale(12)))
            add(panelIgnore())
            add(Box.createVerticalStrut(JBUI.scale(12)))
            add(panelBinary())
            add(Box.createVerticalStrut(JBUI.scale(12)))
            add(panelLint())
            add(Box.createVerticalStrut(JBUI.scale(12)))
            add(panelAdditionalOptions()) // panel for new options
        }

        val formScroll = JBScrollPane(formPanel).apply {
            verticalScrollBar.unitIncrement = 16
            preferredSize = Dimension(480, 620)
        }

        // Preview text area on the right
        previewArea = JTextArea().apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
            border = JBUI.Borders.empty(10)
        }
        val previewScroll = JBScrollPane(previewArea).apply {
            verticalScrollBar.unitIncrement = 16
            preferredSize = Dimension(480, 620)
            border = JBUI.Borders.empty(5)
        }

        // Splitter for left: form; right: preview
        val splitter = JBSplitter(true, 0.5f).apply {
            firstComponent = formScroll
            secondComponent = previewScroll
            dividerWidth = JBUI.scale(5)
        }

        mainPanel.add(splitter, BorderLayout.CENTER)
        updatePreview()
        return mainPanel
    }

    /**
     * Panel for new additional options:
     *  - Include Image Files
     *  - Lint Errors Only
     *  - Lint Warnings Only
     *  - Add Snippet To Queue
     */
    private fun panelAdditionalOptions(): JPanel {
        includeImageFilesCheck = JCheckBox("Include Image Files", options.includeImageFiles)
        lintErrorsOnlyCheck = JCheckBox("Report Only Lint Errors", options.lintErrorsOnly)
        lintWarningsOnlyCheck = JCheckBox("Report Only Lint Warnings", options.lintWarningsOnly)
        addSnippetToQueueCheck = JCheckBox("Add Snippet to Queue", options.addSnippetToQueue)

        return FormBuilder.createFormBuilder()
            .addComponent(includeImageFilesCheck)
            .addComponent(lintErrorsOnlyCheck)
            .addComponent(lintWarningsOnlyCheck)
            .addComponent(addSnippetToQueueCheck)
            .panel.also {
                it.border = BorderFactory.createTitledBorder("Additional Options")
            }
    }

    private fun panelOutput(): JPanel {
        headerField = JBTextField(options.snippetHeaderText.orEmpty(), 20).apply { document.addDocumentListener(docListener) }
        footerField = JBTextField(options.snippetFooterText.orEmpty(), 20).apply { document.addDocumentListener(docListener) }
        directoryStructureCheck = JCheckBox("Directory Structure", options.includeDirectorySummary).apply {
            addActionListener { updatePreview() }
        }
        return FormBuilder.createFormBuilder()
            .addComponent(directoryStructureCheck)
            .addLabeledComponent("Header:", headerField, 1, false)
            .addLabeledComponent("Footer:", footerField, 1, false)
            .panel.also {
                it.border = BorderFactory.createTitledBorder("Output")
            }
    }

    private fun panelFormatting(): JPanel {
        lineNumbersCheck = JCheckBox("Line Numbers", options.includeLineNumbers).apply { addActionListener { updatePreview() } }
        removeImportsCheck = JCheckBox("Remove Imports", options.removeImports).apply { addActionListener { updatePreview() } }
        removeCommentsCheck = JCheckBox("Remove Comments", options.removeComments).apply { addActionListener { updatePreview() } }
        trimWhitespaceCheck = JCheckBox("Trim Whitespace", options.trimWhitespace).apply { addActionListener { updatePreview() } }
        removeEmptyLinesCheck = JCheckBox("Remove Empty Lines", options.removeEmptyLines).apply { addActionListener { updatePreview() } }
        singleLineOutputCheck = JCheckBox("Single-line Output", options.singleLineOutput).apply {
            addActionListener {
                updatePreview()
                updateChunkUI()
            }
        }
        return FormBuilder.createFormBuilder()
            .addComponent(lineNumbersCheck)
            .addComponent(removeImportsCheck)
            .addComponent(removeCommentsCheck)
            .addComponent(trimWhitespaceCheck)
            .addComponent(removeEmptyLinesCheck)
            .addComponent(singleLineOutputCheck)
            .panel.also {
                it.border = BorderFactory.createTitledBorder("Formatting")
            }
    }

    private fun panelChunking(): JPanel {
        chunkStrategyCombo = JComboBox(ChunkStrategy.entries.toTypedArray()).apply {
            selectedItem = options.chunkStrategy
            addActionListener {
                updatePreview()
                updateChunkUI()
            }
        }
        chunkSizeField = JBTextField(options.chunkSize.toString(), 6).apply { document.addDocumentListener(docListener) }
        overlapCombo = JComboBox(OverlapStrategy.entries.toTypedArray()).apply {
            selectedItem = options.overlapStrategy
            addActionListener { updatePreview() }
        }
        compressionCombo = JComboBox(CompressionMode.entries.toTypedArray()).apply {
            selectedItem = options.compressionMode
            addActionListener { updatePreview() }
        }
        chunkLabel = JLabel("").apply { foreground = javax.swing.plaf.ColorUIResource.RED }

        return FormBuilder.createFormBuilder()
            .addLabeledComponent("Chunk Strategy:", chunkStrategyCombo, 1, false)
            .addLabeledComponent("Chunk Size:", chunkSizeField, 1, false)
            .addLabeledComponent("Overlap Strategy:", overlapCombo, 1, false)
            .addLabeledComponent("Compression Mode:", compressionCombo, 1, false)
            .addComponent(chunkLabel)
            .panel.also {
                it.border = BorderFactory.createTitledBorder("Chunking & Overlap")
            }
    }

    private fun panelMetadata(): JPanel {
        metadataCheck = JCheckBox("Include Metadata", options.includeMetadata).apply { addActionListener { updatePreview() } }
        gitInfoCheck = JCheckBox("Git Info", options.includeGitInfo).apply { addActionListener { updatePreview() } }
        autoLangCheck = JCheckBox("Auto-Detect Language", options.autoDetectLanguage).apply { addActionListener { updatePreview() } }
        themeCombo = JComboBox(ThemeMode.entries.toTypedArray()).apply {
            selectedItem = options.themeMode
            addActionListener { updatePreview() }
        }
        return FormBuilder.createFormBuilder()
            .addComponent(metadataCheck)
            .addComponent(gitInfoCheck)
            .addComponent(autoLangCheck)
            .addLabeledComponent("Theme:", themeCombo, 1, false)
            .panel.also {
                it.border = BorderFactory.createTitledBorder("Metadata & Language")
            }
    }

    private fun panelConcurrency(): JPanel {
        concurrencyCombo = JComboBox(ConcurrencyMode.entries.toTypedArray()).apply {
            selectedItem = options.concurrencyMode
            addActionListener { updateConcurrency() }
        }
        maxTasksField = JBTextField(options.maxConcurrentTasks.toString(), 4).apply { document.addDocumentListener(docListener) }
        return FormBuilder.createFormBuilder()
            .addLabeledComponent("Concurrency Mode:", concurrencyCombo, 1, false)
            .addLabeledComponent("Max Tasks:", maxTasksField, 1, false)
            .panel.also {
                it.border = BorderFactory.createTitledBorder("Concurrency")
            }
    }

    private fun panelIgnore(): JPanel {
        gitIgnoreCheck = JCheckBox("Use .gitignore", options.useGitIgnore).apply { addActionListener { updatePreview() } }
        additionalIgnoresField = JBTextField(options.additionalIgnorePatterns.orEmpty(), 15).apply { document.addDocumentListener(docListener) }
        invertIgnoresCheck = JCheckBox("Invert Patterns", options.invertIgnorePatterns).apply { addActionListener { updatePreview() } }
        directoryPatternCheck = JCheckBox("Directory Matching", options.enableDirectoryPatternMatching).apply { addActionListener { updatePreview() } }

        return FormBuilder.createFormBuilder()
            .addComponent(gitIgnoreCheck)
            .addLabeledComponent("Additional Ignore Patterns:", additionalIgnoresField, 1, false)
            .addComponent(invertIgnoresCheck)
            .addComponent(directoryPatternCheck)
            .panel.also {
                it.border = BorderFactory.createTitledBorder("Ignore Options")
            }
    }

    private fun panelBinary(): JPanel {
        detectBinaryCheck = JCheckBox("Detect Binary Files", options.detectBinary).apply { addActionListener { updatePreview() } }
        binaryThresholdField = JBTextField(options.binaryCheckThreshold.toString(), 5).apply { document.addDocumentListener(docListener) }

        return FormBuilder.createFormBuilder()
            .addComponent(detectBinaryCheck)
            .addLabeledComponent("Binary Check Threshold:", binaryThresholdField, 1, false)
            .panel.also {
                it.border = BorderFactory.createTitledBorder("Binary Detection")
            }
    }

    private fun panelLint(): JPanel {
        showLintCheck = JCheckBox("Show Lint Results", options.showLint).apply { addActionListener { updatePreview() } }
        return FormBuilder.createFormBuilder()
            .addComponent(showLintCheck)
            .panel.also {
                it.border = BorderFactory.createTitledBorder("Lint")
            }
    }

    override fun isModified(): Boolean {
        return listOf(
            headerField.text != (options.snippetHeaderText ?: ""),
            footerField.text != (options.snippetFooterText ?: ""),
            directoryStructureCheck.isSelected != options.includeDirectorySummary,
            lineNumbersCheck.isSelected != options.includeLineNumbers,
            removeImportsCheck.isSelected != options.removeImports,
            removeCommentsCheck.isSelected != options.removeComments,
            trimWhitespaceCheck.isSelected != options.trimWhitespace,
            removeEmptyLinesCheck.isSelected != options.removeEmptyLines,
            singleLineOutputCheck.isSelected != options.singleLineOutput,
            chunkStrategyCombo.selectedItem != options.chunkStrategy,
            chunkSizeField.text != options.chunkSize.toString(),
            overlapCombo.selectedItem != options.overlapStrategy,
            compressionCombo.selectedItem != options.compressionMode,
            metadataCheck.isSelected != options.includeMetadata,
            gitInfoCheck.isSelected != options.includeGitInfo,
            autoLangCheck.isSelected != options.autoDetectLanguage,
            themeCombo.selectedItem != options.themeMode,
            concurrencyCombo.selectedItem != options.concurrencyMode,
            maxTasksField.text != options.maxConcurrentTasks.toString(),
            gitIgnoreCheck.isSelected != options.useGitIgnore,
            additionalIgnoresField.text != (options.additionalIgnorePatterns ?: ""),
            invertIgnoresCheck.isSelected != options.invertIgnorePatterns,
            directoryPatternCheck.isSelected != options.enableDirectoryPatternMatching,
            detectBinaryCheck.isSelected != options.detectBinary,
            binaryThresholdField.text != options.binaryCheckThreshold.toString(),
            showLintCheck.isSelected != options.showLint,
            // New options:
            includeImageFilesCheck.isSelected != options.includeImageFiles,
            lintErrorsOnlyCheck.isSelected != options.lintErrorsOnly,
            lintWarningsOnlyCheck.isSelected != options.lintWarningsOnly,
            addSnippetToQueueCheck.isSelected != options.addSnippetToQueue,
        ).any { it }
    }

    override fun apply() {
        options.snippetHeaderText = headerField.text
        options.snippetFooterText = footerField.text
        options.includeDirectorySummary = directoryStructureCheck.isSelected
        options.includeLineNumbers = lineNumbersCheck.isSelected
        options.removeImports = removeImportsCheck.isSelected
        options.removeComments = removeCommentsCheck.isSelected
        options.trimWhitespace = trimWhitespaceCheck.isSelected
        options.removeEmptyLines = removeEmptyLinesCheck.isSelected
        options.singleLineOutput = singleLineOutputCheck.isSelected
        options.chunkStrategy = chunkStrategyCombo.selectedItem as ChunkStrategy
        options.chunkSize = chunkSizeField.text.toIntOrNull() ?: options.chunkSize
        options.overlapStrategy = overlapCombo.selectedItem as OverlapStrategy
        options.compressionMode = compressionCombo.selectedItem as CompressionMode
        options.includeMetadata = metadataCheck.isSelected
        options.includeGitInfo = gitInfoCheck.isSelected
        options.autoDetectLanguage = autoLangCheck.isSelected
        options.themeMode = themeCombo.selectedItem as ThemeMode
        options.concurrencyMode = concurrencyCombo.selectedItem as ConcurrencyMode
        options.maxConcurrentTasks = maxTasksField.text.toIntOrNull() ?: options.maxConcurrentTasks
        options.useGitIgnore = gitIgnoreCheck.isSelected
        options.additionalIgnorePatterns = additionalIgnoresField.text.ifBlank { null }
        options.invertIgnorePatterns = invertIgnoresCheck.isSelected
        options.enableDirectoryPatternMatching = directoryPatternCheck.isSelected
        options.detectBinary = detectBinaryCheck.isSelected
        options.binaryCheckThreshold = binaryThresholdField.text.toIntOrNull() ?: options.binaryCheckThreshold
        options.showLint = showLintCheck.isSelected
        // New options:
        options.includeImageFiles = includeImageFilesCheck.isSelected
        options.lintErrorsOnly = lintErrorsOnlyCheck.isSelected
        options.lintWarningsOnly = lintWarningsOnlyCheck.isSelected
        options.addSnippetToQueue = addSnippetToQueueCheck.isSelected

        options.resolveConflicts()
        manager.deleteProfile(savedProfile.profileName)
        manager.addProfile(savedProfile.copy(options = options))
        manager.switchProfile(savedProfile.profileName)
    }

    override fun reset() {
        headerField.text = options.snippetHeaderText.orEmpty()
        footerField.text = options.snippetFooterText.orEmpty()
        directoryStructureCheck.isSelected = options.includeDirectorySummary
        lineNumbersCheck.isSelected = options.includeLineNumbers
        removeImportsCheck.isSelected = options.removeImports
        removeCommentsCheck.isSelected = options.removeComments
        trimWhitespaceCheck.isSelected = options.trimWhitespace
        removeEmptyLinesCheck.isSelected = options.removeEmptyLines
        singleLineOutputCheck.isSelected = options.singleLineOutput
        chunkStrategyCombo.selectedItem = options.chunkStrategy
        chunkSizeField.text = options.chunkSize.toString()
        overlapCombo.selectedItem = options.overlapStrategy
        compressionCombo.selectedItem = options.compressionMode
        metadataCheck.isSelected = options.includeMetadata
        gitInfoCheck.isSelected = options.includeGitInfo
        autoLangCheck.isSelected = options.autoDetectLanguage
        themeCombo.selectedItem = options.themeMode
        concurrencyCombo.selectedItem = options.concurrencyMode
        maxTasksField.text = options.maxConcurrentTasks.toString()
        gitIgnoreCheck.isSelected = options.useGitIgnore
        additionalIgnoresField.text = options.additionalIgnorePatterns.orEmpty()
        invertIgnoresCheck.isSelected = options.invertIgnorePatterns
        directoryPatternCheck.isSelected = options.enableDirectoryPatternMatching
        detectBinaryCheck.isSelected = options.detectBinary
        binaryThresholdField.text = options.binaryCheckThreshold.toString()
        showLintCheck.isSelected = options.showLint
        // New options:
        includeImageFilesCheck.isSelected = options.includeImageFiles
        lintErrorsOnlyCheck.isSelected = options.lintErrorsOnly
        lintWarningsOnlyCheck.isSelected = options.lintWarningsOnly
        addSnippetToQueueCheck.isSelected = options.addSnippetToQueue

        updatePreview()
        updateChunkUI()
        updateConcurrency()
    }

    override fun disposeUIResources() {}

    private fun updatePreview() {
        val tmp = options.copy().apply {
            snippetHeaderText = headerField.text
            snippetFooterText = footerField.text
            includeDirectorySummary = directoryStructureCheck.isSelected
            includeLineNumbers = lineNumbersCheck.isSelected
            removeImports = removeImportsCheck.isSelected
            removeComments = removeCommentsCheck.isSelected
            trimWhitespace = trimWhitespaceCheck.isSelected
            removeEmptyLines = removeEmptyLinesCheck.isSelected
            singleLineOutput = singleLineOutputCheck.isSelected
            chunkStrategy = chunkStrategyCombo.selectedItem as ChunkStrategy
            chunkSize = chunkSizeField.text.toIntOrNull() ?: chunkSize
            overlapStrategy = overlapCombo.selectedItem as OverlapStrategy
            compressionMode = compressionCombo.selectedItem as CompressionMode
            includeMetadata = metadataCheck.isSelected
            includeGitInfo = gitInfoCheck.isSelected
            autoDetectLanguage = autoLangCheck.isSelected
            themeMode = themeCombo.selectedItem as ThemeMode
            concurrencyMode = concurrencyCombo.selectedItem as ConcurrencyMode
            maxConcurrentTasks = maxTasksField.text.toIntOrNull() ?: maxConcurrentTasks
            useGitIgnore = gitIgnoreCheck.isSelected
            additionalIgnorePatterns = additionalIgnoresField.text.ifBlank { null }
            invertIgnorePatterns = invertIgnoresCheck.isSelected
            enableDirectoryPatternMatching = directoryPatternCheck.isSelected
            detectBinary = detectBinaryCheck.isSelected
            binaryCheckThreshold = binaryThresholdField.text.toIntOrNull() ?: binaryCheckThreshold
            showLint = showLintCheck.isSelected
            // New options:
            includeImageFiles = includeImageFilesCheck.isSelected
            lintErrorsOnly = lintErrorsOnlyCheck.isSelected
            lintWarningsOnly = lintWarningsOnlyCheck.isSelected
            addSnippetToQueue = addSnippetToQueueCheck.isSelected
        }

        tmp.resolveConflicts()
        val snippet = com.clipcraft.model.Snippet(
            content = sampleCode,
            fileName = "Sample.kt",
            relativePath = "src/Sample.kt",
            filePath = "C:/fake/path/Sample.kt",
            fileSizeBytes = sampleCode.length.toLong(),
            lastModified = System.currentTimeMillis(),
        )
        val formattedCode = CodeFormatter.formatSnippets(listOf(snippet), tmp).joinToString("\n---\n")
        val dirStruct = if (tmp.includeDirectorySummary) "Directory Structure:\n  src/Sample.kt\n\n" else ""

        val fullPreview = buildString {
            if (!tmp.snippetHeaderText.isNullOrEmpty()) {
                appendLine(tmp.snippetHeaderText)
                appendLine()
            }
            if (dirStruct.isNotEmpty()) append(dirStruct)
            append(formattedCode)
            if (!tmp.snippetFooterText.isNullOrEmpty()) {
                appendLine()
                appendLine(tmp.snippetFooterText)
            }
        }

        previewArea.text = fullPreview
        previewArea.caretPosition = 0
        updateChunkUI()
    }

    private fun updateChunkUI() {
        val active = !singleLineOutputCheck.isSelected && (chunkStrategyCombo.selectedItem as ChunkStrategy) != ChunkStrategy.NONE
        chunkSizeField.isEnabled = active && (chunkStrategyCombo.selectedItem == ChunkStrategy.BY_SIZE)
        overlapCombo.isEnabled = active
        compressionCombo.isEnabled = true
        chunkLabel.text = if (singleLineOutputCheck.isSelected) "Single-line output active; chunking disabled" else ""
    }

    private fun updateConcurrency() {
        maxTasksField.isEnabled = (concurrencyCombo.selectedItem as ConcurrencyMode) != ConcurrencyMode.DISABLED
    }
}
