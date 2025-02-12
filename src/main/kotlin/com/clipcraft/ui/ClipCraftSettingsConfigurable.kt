package com.clipcraft.ui

import com.clipcraft.model.ChunkStrategy
import com.clipcraft.model.CompressionMode
import com.clipcraft.model.ConcurrencyMode
import com.clipcraft.model.OverlapStrategy
import com.clipcraft.model.ThemeMode
import com.clipcraft.services.ClipCraftProfileManager
import com.clipcraft.util.CodeFormatter
import com.intellij.openapi.options.Configurable
import com.intellij.ui.JBColor
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

/**
 * Main settings panel for ClipCraft plugin.
 */
class ClipCraftSettingsConfigurable : Configurable {
    private val manager = ClipCraftProfileManager()
    private val savedProfile = manager.currentProfile().copy()
    private val o = savedProfile.options
    private val sampleCode = """// Example
package test
fun main() {
    println("Hello")
}"""

    // UI components
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
    private lateinit var chunkLabel: JLabel
    private lateinit var previewArea: JTextArea
    private lateinit var mainPanel: JPanel
    private lateinit var showLintCheck: JCheckBox

    private val listener = object : DocumentListener {
        override fun insertUpdate(e: DocumentEvent?) = updatePreview()
        override fun removeUpdate(e: DocumentEvent?) = updatePreview()
        override fun changedUpdate(e: DocumentEvent?) = updatePreview()
    }

    override fun getDisplayName() = "ClipCraft"

    override fun createComponent(): JComponent {
        mainPanel = JPanel(BorderLayout())
        val form = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(8)
            add(panelOutput())
            add(Box.createVerticalStrut(JBUI.scale(8)))
            add(panelFormatting())
            add(Box.createVerticalStrut(JBUI.scale(8)))
            add(panelChunking())
            add(Box.createVerticalStrut(JBUI.scale(8)))
            add(panelMetadata())
            add(Box.createVerticalStrut(JBUI.scale(8)))
            add(panelConcurrency())
            add(Box.createVerticalStrut(JBUI.scale(8)))
            add(panelIgnore())
            add(Box.createVerticalStrut(JBUI.scale(8)))
            add(panelBinary())
            add(Box.createVerticalStrut(JBUI.scale(8)))
            add(panelLint())
        }
        val scrollForm = JBScrollPane(form).apply {
            verticalScrollBar.unitIncrement = 16
            preferredSize = Dimension(450, 600)
        }
        previewArea = JTextArea().apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
        }
        val scrollPreview = JBScrollPane(previewArea).apply {
            verticalScrollBar.unitIncrement = 16
            preferredSize = Dimension(450, 600)
        }
        val splitter = JBSplitter(true, 0.6f).apply {
            firstComponent = scrollForm
            secondComponent = scrollPreview
            preferredSize = Dimension(900, 600)
        }
        updatePreview()
        return splitter
    }

    override fun isModified(): Boolean {
        if (headerField.text != (o.snippetHeaderText ?: "")) return true
        if (footerField.text != (o.snippetFooterText ?: "")) return true
        if (directoryStructureCheck.isSelected != o.includeDirectorySummary) return true
        if (lineNumbersCheck.isSelected != o.includeLineNumbers) return true
        if (removeImportsCheck.isSelected != o.removeImports) return true
        if (removeCommentsCheck.isSelected != o.removeComments) return true
        if (trimWhitespaceCheck.isSelected != o.trimWhitespace) return true
        if (removeEmptyLinesCheck.isSelected != o.removeEmptyLines) return true
        if (singleLineOutputCheck.isSelected != o.singleLineOutput) return true
        if (chunkStrategyCombo.selectedItem != o.chunkStrategy) return true
        if (chunkSizeField.text != o.chunkSize.toString()) return true
        if (overlapCombo.selectedItem != o.overlapStrategy) return true
        if (compressionCombo.selectedItem != o.compressionMode) return true
        if (metadataCheck.isSelected != o.includeMetadata) return true
        if (gitInfoCheck.isSelected != o.includeGitInfo) return true
        if (autoLangCheck.isSelected != o.autoDetectLanguage) return true
        if (themeCombo.selectedItem != o.themeMode) return true
        if (concurrencyCombo.selectedItem != o.concurrencyMode) return true
        if (maxTasksField.text != o.maxConcurrentTasks.toString()) return true
        if (gitIgnoreCheck.isSelected != o.useGitIgnore) return true
        if (additionalIgnoresField.text != (o.additionalIgnorePatterns ?: "")) return true
        if (invertIgnoresCheck.isSelected != o.invertIgnorePatterns) return true
        if (directoryPatternCheck.isSelected != o.enableDirectoryPatternMatching) return true
        if (detectBinaryCheck.isSelected != o.detectBinary) return true
        if (binaryThresholdField.text != o.binaryCheckThreshold.toString()) return true
        if (showLintCheck.isSelected != o.showLint) return true

        return false
    }

    override fun apply() {
        o.snippetHeaderText = headerField.text
        o.snippetFooterText = footerField.text
        o.includeDirectorySummary = directoryStructureCheck.isSelected
        o.includeLineNumbers = lineNumbersCheck.isSelected
        o.removeImports = removeImportsCheck.isSelected
        o.removeComments = removeCommentsCheck.isSelected
        o.trimWhitespace = trimWhitespaceCheck.isSelected
        o.removeEmptyLines = removeEmptyLinesCheck.isSelected
        o.singleLineOutput = singleLineOutputCheck.isSelected
        o.chunkStrategy = chunkStrategyCombo.selectedItem as ChunkStrategy
        o.chunkSize = chunkSizeField.text.toIntOrNull() ?: o.chunkSize
        o.overlapStrategy = overlapCombo.selectedItem as OverlapStrategy
        o.compressionMode = compressionCombo.selectedItem as CompressionMode
        o.includeMetadata = metadataCheck.isSelected
        o.includeGitInfo = gitInfoCheck.isSelected
        o.autoDetectLanguage = autoLangCheck.isSelected
        o.themeMode = themeCombo.selectedItem as ThemeMode
        o.concurrencyMode = concurrencyCombo.selectedItem as ConcurrencyMode
        o.maxConcurrentTasks = maxTasksField.text.toIntOrNull() ?: o.maxConcurrentTasks
        o.useGitIgnore = gitIgnoreCheck.isSelected
        o.additionalIgnorePatterns = additionalIgnoresField.text.ifBlank { null }
        o.invertIgnorePatterns = invertIgnoresCheck.isSelected
        o.enableDirectoryPatternMatching = directoryPatternCheck.isSelected
        o.detectBinary = detectBinaryCheck.isSelected
        o.binaryCheckThreshold = binaryThresholdField.text.toIntOrNull() ?: o.binaryCheckThreshold
        o.showLint = showLintCheck.isSelected

        o.resolveConflicts()
        manager.deleteProfile(savedProfile.profileName)
        manager.addProfile(savedProfile.copy(options = o))
        manager.switchProfile(savedProfile.profileName)
    }

    override fun reset() {
        headerField.text = o.snippetHeaderText.orEmpty()
        footerField.text = o.snippetFooterText.orEmpty()
        directoryStructureCheck.isSelected = o.includeDirectorySummary
        lineNumbersCheck.isSelected = o.includeLineNumbers
        removeImportsCheck.isSelected = o.removeImports
        removeCommentsCheck.isSelected = o.removeComments
        trimWhitespaceCheck.isSelected = o.trimWhitespace
        removeEmptyLinesCheck.isSelected = o.removeEmptyLines
        singleLineOutputCheck.isSelected = o.singleLineOutput
        chunkStrategyCombo.selectedItem = o.chunkStrategy
        chunkSizeField.text = o.chunkSize.toString()
        overlapCombo.selectedItem = o.overlapStrategy
        compressionCombo.selectedItem = o.compressionMode
        metadataCheck.isSelected = o.includeMetadata
        gitInfoCheck.isSelected = o.includeGitInfo
        autoLangCheck.isSelected = o.autoDetectLanguage
        themeCombo.selectedItem = o.themeMode
        concurrencyCombo.selectedItem = o.concurrencyMode
        maxTasksField.text = o.maxConcurrentTasks.toString()
        gitIgnoreCheck.isSelected = o.useGitIgnore
        additionalIgnoresField.text = o.additionalIgnorePatterns.orEmpty()
        invertIgnoresCheck.isSelected = o.invertIgnorePatterns
        directoryPatternCheck.isSelected = o.enableDirectoryPatternMatching
        detectBinaryCheck.isSelected = o.detectBinary
        binaryThresholdField.text = o.binaryCheckThreshold.toString()
        showLintCheck.isSelected = o.showLint

        updatePreview()
        updateChunkUI()
        updateConcurrency()
    }

    override fun disposeUIResources() {}

    private fun updatePreview() {
        // Create a temporary copy of options with current UI values
        val tmp = o.copy().apply {
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
        val dirStruct = if (tmp.includeDirectorySummary) {
            "Directory Structure:\n  src/Sample.kt\n\n"
        } else {
            ""
        }
        val h = tmp.snippetHeaderText.orEmpty()
        val f = tmp.snippetFooterText.orEmpty()
        val fullPreview = buildString {
            if (h.isNotEmpty()) appendLine(h).appendLine()
            if (dirStruct.isNotEmpty()) append(dirStruct)
            append(formattedCode)
            if (f.isNotEmpty()) {
                appendLine().appendLine(f)
            }
        }
        previewArea.text = fullPreview
        previewArea.caretPosition = 0
        updateChunkUI()
    }

    private fun updateChunkUI() {
        val single = singleLineOutputCheck.isSelected
        val s = chunkStrategyCombo.selectedItem as ChunkStrategy
        val active = !single && s != ChunkStrategy.NONE
        chunkSizeField.isEnabled = active && s == ChunkStrategy.BY_SIZE
        overlapCombo.isEnabled = active
        compressionCombo.isEnabled = true
        chunkLabel.text = when {
            single -> "Single-line output is active, chunking disabled"
            s == ChunkStrategy.NONE -> "Chunking disabled"
            else -> ""
        }
    }

    private fun updateConcurrency() {
        val mode = concurrencyCombo.selectedItem as ConcurrencyMode
        maxTasksField.isEnabled = mode != ConcurrencyMode.DISABLED
    }

    // Panels
    private fun panelOutput(): JPanel {
        headerField = JBTextField(o.snippetHeaderText.orEmpty(), 20).apply {
            document.addDocumentListener(listener)
        }
        footerField = JBTextField(o.snippetFooterText.orEmpty(), 20).apply {
            document.addDocumentListener(listener)
        }
        directoryStructureCheck = JCheckBox("Directory Structure", o.includeDirectorySummary).apply {
            addActionListener { updatePreview() }
        }
        return FormBuilder.createFormBuilder()
            .addComponent(directoryStructureCheck)
            .addLabeledComponent("Header:", headerField, 1, false)
            .addLabeledComponent("Footer:", footerField, 1, false)
            .panel.also { it.border = BorderFactory.createTitledBorder("Output") }
    }

    private fun panelFormatting(): JPanel {
        lineNumbersCheck = JCheckBox("Line Numbers", o.includeLineNumbers).apply {
            addActionListener { updatePreview() }
        }
        removeImportsCheck = JCheckBox("Remove Imports", o.removeImports).apply {
            addActionListener { updatePreview() }
        }
        removeCommentsCheck = JCheckBox("Remove Comments", o.removeComments).apply {
            addActionListener { updatePreview() }
        }
        trimWhitespaceCheck = JCheckBox("Trim Whitespace", o.trimWhitespace).apply {
            addActionListener { updatePreview() }
        }
        removeEmptyLinesCheck = JCheckBox("Remove Empty Lines", o.removeEmptyLines).apply {
            addActionListener { updatePreview() }
        }
        singleLineOutputCheck = JCheckBox("Single-line Output", o.singleLineOutput).apply {
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
            .panel.also { it.border = BorderFactory.createTitledBorder("Formatting") }
    }

    private fun panelChunking(): JPanel {
        chunkStrategyCombo = JComboBox(ChunkStrategy.entries.toTypedArray()).apply {
            selectedItem = o.chunkStrategy
            addActionListener {
                updatePreview()
                updateChunkUI()
            }
        }
        chunkSizeField = JBTextField(o.chunkSize.toString(), 6).apply {
            document.addDocumentListener(listener)
        }
        overlapCombo = JComboBox(OverlapStrategy.entries.toTypedArray()).apply {
            selectedItem = o.overlapStrategy
            addActionListener { updatePreview() }
        }
        compressionCombo = JComboBox(CompressionMode.entries.toTypedArray()).apply {
            selectedItem = o.compressionMode
            addActionListener { updatePreview() }
        }
        chunkLabel = JLabel("").apply { foreground = JBColor.RED }
        return FormBuilder.createFormBuilder()
            .addLabeledComponent("Chunk Strategy:", chunkStrategyCombo, 1, false)
            .addLabeledComponent("Chunk Size:", chunkSizeField, 1, false)
            .addLabeledComponent("Overlap Strategy:", overlapCombo, 1, false)
            .addLabeledComponent("Compression Mode:", compressionCombo, 1, false)
            .addComponent(chunkLabel)
            .panel.also { it.border = BorderFactory.createTitledBorder("Chunking & Overlap") }
    }

    private fun panelMetadata(): JPanel {
        metadataCheck = JCheckBox("Include Metadata", o.includeMetadata).apply {
            addActionListener { updatePreview() }
        }
        gitInfoCheck = JCheckBox("Git Info", o.includeGitInfo).apply {
            addActionListener { updatePreview() }
        }
        autoLangCheck = JCheckBox("Auto-Detect Language", o.autoDetectLanguage).apply {
            addActionListener { updatePreview() }
        }
        themeCombo = JComboBox(ThemeMode.entries.toTypedArray()).apply {
            selectedItem = o.themeMode
            addActionListener { updatePreview() }
        }
        return FormBuilder.createFormBuilder()
            .addComponent(metadataCheck)
            .addComponent(gitInfoCheck)
            .addComponent(autoLangCheck)
            .addLabeledComponent("Theme:", themeCombo, 1, false)
            .panel.also { it.border = BorderFactory.createTitledBorder("Metadata & Language") }
    }

    private fun panelConcurrency(): JPanel {
        concurrencyCombo = JComboBox(ConcurrencyMode.entries.toTypedArray()).apply {
            selectedItem = o.concurrencyMode
            addActionListener { updateConcurrency() }
        }
        maxTasksField = JBTextField(o.maxConcurrentTasks.toString(), 4).apply {
            document.addDocumentListener(listener)
        }
        return FormBuilder.createFormBuilder()
            .addLabeledComponent("Concurrency Mode:", concurrencyCombo, 1, false)
            .addLabeledComponent("Max Tasks:", maxTasksField, 1, false)
            .panel.also { it.border = BorderFactory.createTitledBorder("Concurrency") }
    }

    private fun panelIgnore(): JPanel {
        gitIgnoreCheck = JCheckBox("Use .gitignore", o.useGitIgnore).apply {
            addActionListener { updatePreview() }
        }
        additionalIgnoresField = JBTextField(o.additionalIgnorePatterns.orEmpty(), 15).apply {
            document.addDocumentListener(listener)
        }
        invertIgnoresCheck = JCheckBox("Invert Patterns", o.invertIgnorePatterns).apply {
            addActionListener { updatePreview() }
        }
        directoryPatternCheck = JCheckBox("Directory Matching", o.enableDirectoryPatternMatching).apply {
            addActionListener { updatePreview() }
        }
        return FormBuilder.createFormBuilder()
            .addComponent(gitIgnoreCheck)
            .addLabeledComponent("Additional Ignore Patterns:", additionalIgnoresField, 1, false)
            .addComponent(invertIgnoresCheck)
            .addComponent(directoryPatternCheck)
            .panel.also { it.border = BorderFactory.createTitledBorder("Ignore Options") }
    }

    private fun panelBinary(): JPanel {
        detectBinaryCheck = JCheckBox("Detect Binary Files", o.detectBinary).apply {
            addActionListener { updatePreview() }
        }
        binaryThresholdField = JBTextField(o.binaryCheckThreshold.toString(), 5).apply {
            document.addDocumentListener(listener)
        }
        return FormBuilder.createFormBuilder()
            .addComponent(detectBinaryCheck)
            .addLabeledComponent("Binary Check Threshold:", binaryThresholdField, 1, false)
            .panel.also { it.border = BorderFactory.createTitledBorder("Binary Detection") }
    }

    private fun panelLint(): JPanel {
        showLintCheck = JCheckBox("Show Lint Results", o.showLint).apply {
            addActionListener { updatePreview() }
        }
        return FormBuilder.createFormBuilder()
            .addComponent(showLintCheck)
            .panel.also { it.border = BorderFactory.createTitledBorder("Lint") }
    }
}
