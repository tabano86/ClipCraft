package com.clipcraft.ui

import com.clipcraft.model.ChunkStrategy
import com.clipcraft.model.CompressionMode
import com.clipcraft.model.ConcurrencyMode
import com.clipcraft.model.OverlapStrategy
import com.clipcraft.model.Snippet
import com.clipcraft.model.ThemeMode
import com.clipcraft.services.ClipCraftProfileManager
import com.clipcraft.util.CodeFormatter
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.BoxLayout
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.border.TitledBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class ClipCraftSettingsConfigurable : Configurable {
    private val manager = ClipCraftProfileManager()
    private val savedProfile = manager.currentProfile().copy()
    private val o = savedProfile.options
    private val sampleCode = """// Example
package test
fun main() {
 println("Hello")
}"""

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

    override fun getDisplayName() = "ClipCraft"

    override fun createComponent(): JComponent {
        mainPanel = JPanel(BorderLayout())
        val form = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(panelOutput())
            add(box("Formatting", panelFormatting()))
            add(box("Chunking & Overlap", panelChunking()))
            add(box("Metadata & Language", panelMetadata()))
            add(box("Concurrency", panelConcurrency()))
            add(box("Ignore Options", panelIgnore()))
            add(box("Binary Detection", panelBinary()))
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
        if (headerField.text != (o.gptHeaderText ?: "")) return true
        if (footerField.text != (o.gptFooterText ?: "")) return true
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
        return false
    }

    override fun apply() {
        o.gptHeaderText = headerField.text
        o.gptFooterText = footerField.text
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
        o.resolveConflicts()
        manager.deleteProfile(savedProfile.profileName)
        manager.addProfile(savedProfile.copy(options = o))
        manager.switchProfile(savedProfile.profileName)
    }

    override fun reset() {
        headerField.text = o.gptHeaderText.orEmpty()
        footerField.text = o.gptFooterText.orEmpty()
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
        updatePreview()
        updateChunkUI()
        updateConcurrency()
    }

    override fun disposeUIResources() {}

    private val listener = object : DocumentListener {
        override fun insertUpdate(e: DocumentEvent?) = updatePreview()
        override fun removeUpdate(e: DocumentEvent?) = updatePreview()
        override fun changedUpdate(e: DocumentEvent?) = updatePreview()
    }

    private fun updatePreview() {
        val tmp = o.copy()
        tmp.gptHeaderText = headerField.text
        tmp.gptFooterText = footerField.text
        tmp.includeDirectorySummary = directoryStructureCheck.isSelected
        tmp.includeLineNumbers = lineNumbersCheck.isSelected
        tmp.removeImports = removeImportsCheck.isSelected
        tmp.removeComments = removeCommentsCheck.isSelected
        tmp.trimWhitespace = trimWhitespaceCheck.isSelected
        tmp.removeEmptyLines = removeEmptyLinesCheck.isSelected
        tmp.singleLineOutput = singleLineOutputCheck.isSelected
        tmp.chunkStrategy = chunkStrategyCombo.selectedItem as ChunkStrategy
        tmp.chunkSize = chunkSizeField.text.toIntOrNull() ?: tmp.chunkSize
        tmp.overlapStrategy = overlapCombo.selectedItem as OverlapStrategy
        tmp.compressionMode = compressionCombo.selectedItem as CompressionMode
        tmp.includeMetadata = metadataCheck.isSelected
        tmp.includeGitInfo = gitInfoCheck.isSelected
        tmp.autoDetectLanguage = autoLangCheck.isSelected
        tmp.themeMode = themeCombo.selectedItem as ThemeMode
        tmp.concurrencyMode = concurrencyCombo.selectedItem as ConcurrencyMode
        tmp.maxConcurrentTasks = maxTasksField.text.toIntOrNull() ?: tmp.maxConcurrentTasks
        tmp.useGitIgnore = gitIgnoreCheck.isSelected
        tmp.additionalIgnorePatterns = additionalIgnoresField.text.ifBlank { null }
        tmp.invertIgnorePatterns = invertIgnoresCheck.isSelected
        tmp.enableDirectoryPatternMatching = directoryPatternCheck.isSelected
        tmp.detectBinary = detectBinaryCheck.isSelected
        tmp.binaryCheckThreshold = binaryThresholdField.text.toIntOrNull() ?: tmp.binaryCheckThreshold
        tmp.resolveConflicts()
        val snippet = Snippet(content = sampleCode, fileName = "Sample.kt", relativePath = "src/Sample.kt")
        val out = CodeFormatter.formatSnippets(listOf(snippet), tmp).joinToString("\n\n")
        val dir = if (tmp.includeDirectorySummary) "[DirectoryStructure]\nsrc/\n  Sample.kt\n\n" else ""
        val full = buildString {
            val h = tmp.gptHeaderText.orEmpty()
            val f = tmp.gptFooterText.orEmpty()
            if (h.isNotEmpty()) appendLine(h).appendLine()
            if (dir.isNotEmpty()) append(dir)
            append(out)
            if (f.isNotEmpty()) appendLine().appendLine().append(f)
        }
        previewArea.text = full
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
        if (single) chunkLabel.text = "Single-line output is active, chunking disabled"
        else if (s == ChunkStrategy.NONE) chunkLabel.text = "Chunking disabled"
        else chunkLabel.text = ""
    }

    private fun updateConcurrency() {
        val mode = concurrencyCombo.selectedItem as ConcurrencyMode
        maxTasksField.isEnabled = mode != ConcurrencyMode.DISABLED
    }

    private fun panelOutput(): JPanel {
        val g = GridBagConstraints().apply {
            insets = JBUI.insets(4)
            anchor = GridBagConstraints.WEST
            gridx = 0
            gridy = 0
        }
        val p = JPanel(GridBagLayout())
        directoryStructureCheck = JCheckBox("Directory Structure", o.includeDirectorySummary)
        directoryStructureCheck.addActionListener { updatePreview() }
        p.add(directoryStructureCheck, g)
        g.gridy++
        p.add(JLabel("Header:"), g)
        g.gridx = 1
        headerField = JTextField(o.gptHeaderText.orEmpty(), 20)
        headerField.document.addDocumentListener(listener)
        p.add(headerField, g)
        g.gridx = 0
        g.gridy++
        p.add(JLabel("Footer:"), g)
        g.gridx = 1
        footerField = JTextField(o.gptFooterText.orEmpty(), 20)
        footerField.document.addDocumentListener(listener)
        p.add(footerField, g)
        p.border = TitledBorder("Output")
        return p
    }

    private fun panelFormatting(): JPanel {
        val g = GridBagConstraints().apply {
            insets = JBUI.insets(4)
            anchor = GridBagConstraints.WEST
            gridx = 0
            gridy = 0
        }
        val p = JPanel(GridBagLayout())
        lineNumbersCheck = JCheckBox("Line Numbers", o.includeLineNumbers)
        lineNumbersCheck.addActionListener { updatePreview() }
        p.add(lineNumbersCheck, g)
        g.gridy++
        removeImportsCheck = JCheckBox("Remove Imports", o.removeImports)
        removeImportsCheck.addActionListener { updatePreview() }
        p.add(removeImportsCheck, g)
        g.gridy++
        removeCommentsCheck = JCheckBox("Remove Comments", o.removeComments)
        removeCommentsCheck.addActionListener { updatePreview() }
        p.add(removeCommentsCheck, g)
        g.gridy++
        trimWhitespaceCheck = JCheckBox("Trim Whitespace", o.trimWhitespace)
        trimWhitespaceCheck.addActionListener { updatePreview() }
        p.add(trimWhitespaceCheck, g)
        g.gridy++
        removeEmptyLinesCheck = JCheckBox("Remove Empty Lines", o.removeEmptyLines)
        removeEmptyLinesCheck.addActionListener { updatePreview() }
        p.add(removeEmptyLinesCheck, g)
        g.gridy++
        singleLineOutputCheck = JCheckBox("Single-line Output", o.singleLineOutput)
        singleLineOutputCheck.addActionListener {
            updatePreview()
            updateChunkUI()
        }
        p.add(singleLineOutputCheck, g)
        return p
    }

    private fun panelChunking(): JPanel {
        val g = GridBagConstraints().apply {
            insets = JBUI.insets(4)
            anchor = GridBagConstraints.WEST
            gridx = 0
            gridy = 0
        }
        val p = JPanel(GridBagLayout())
        p.border = TitledBorder("Chunking")
        p.add(JLabel("Strategy:"), g)
        g.gridx = 1
        chunkStrategyCombo = ComboBox(ChunkStrategy.values())
        chunkStrategyCombo.selectedItem = o.chunkStrategy
        chunkStrategyCombo.addActionListener {
            updatePreview()
            updateChunkUI()
        }
        p.add(chunkStrategyCombo, g)
        g.gridx = 0
        g.gridy++
        p.add(JLabel("Chunk Size:"), g)
        g.gridx = 1
        chunkSizeField = JTextField(o.chunkSize.toString(), 6)
        chunkSizeField.document.addDocumentListener(listener)
        p.add(chunkSizeField, g)
        g.gridx = 0
        g.gridy++
        p.add(JLabel("Overlap:"), g)
        g.gridx = 1
        overlapCombo = ComboBox(OverlapStrategy.values())
        overlapCombo.selectedItem = o.overlapStrategy
        overlapCombo.addActionListener { updatePreview() }
        p.add(overlapCombo, g)
        g.gridx = 0
        g.gridy++
        p.add(JLabel("Compression:"), g)
        g.gridx = 1
        compressionCombo = ComboBox(CompressionMode.values())
        compressionCombo.selectedItem = o.compressionMode
        compressionCombo.addActionListener { updatePreview() }
        p.add(compressionCombo, g)
        g.gridx = 0
        g.gridy++
        g.gridwidth = 2
        chunkLabel = JLabel("")
        chunkLabel.foreground = JBColor.RED
        p.add(chunkLabel, g)
        return p
    }

    private fun panelMetadata(): JPanel {
        val g = GridBagConstraints().apply {
            insets = JBUI.insets(4)
            anchor = GridBagConstraints.WEST
            gridx = 0
            gridy = 0
        }
        val p = JPanel(GridBagLayout())
        p.border = TitledBorder("Metadata")
        metadataCheck = JCheckBox("Include Metadata", o.includeMetadata)
        metadataCheck.addActionListener { updatePreview() }
        p.add(metadataCheck, g)
        g.gridy++
        gitInfoCheck = JCheckBox("Git Info", o.includeGitInfo)
        gitInfoCheck.addActionListener { updatePreview() }
        p.add(gitInfoCheck, g)
        g.gridy++
        autoLangCheck = JCheckBox("Auto-Detect Language", o.autoDetectLanguage)
        autoLangCheck.addActionListener { updatePreview() }
        p.add(autoLangCheck, g)
        g.gridy++
        p.add(JLabel("Theme:"), g)
        g.gridx = 1
        themeCombo = ComboBox(ThemeMode.values())
        themeCombo.selectedItem = o.themeMode
        themeCombo.addActionListener { updatePreview() }
        p.add(themeCombo, g)
        return p
    }

    private fun panelConcurrency(): JPanel {
        val g = GridBagConstraints().apply {
            insets = JBUI.insets(4)
            anchor = GridBagConstraints.WEST
            gridx = 0
            gridy = 0
        }
        val p = JPanel(GridBagLayout())
        p.border = TitledBorder("Concurrency")
        p.add(JLabel("Mode:"), g)
        g.gridx = 1
        concurrencyCombo = ComboBox(ConcurrencyMode.values())
        concurrencyCombo.selectedItem = o.concurrencyMode
        concurrencyCombo.addActionListener { updateConcurrency() }
        p.add(concurrencyCombo, g)
        g.gridx = 0
        g.gridy++
        p.add(JLabel("Max Tasks:"), g)
        g.gridx = 1
        maxTasksField = JTextField(o.maxConcurrentTasks.toString(), 4)
        p.add(maxTasksField, g)
        return p
    }

    private fun panelIgnore(): JPanel {
        val g = GridBagConstraints().apply {
            insets = JBUI.insets(4)
            anchor = GridBagConstraints.WEST
            gridx = 0
            gridy = 0
        }
        val p = JPanel(GridBagLayout())
        p.border = TitledBorder("Ignore")
        gitIgnoreCheck = JCheckBox("Use .gitignore", o.useGitIgnore)
        gitIgnoreCheck.addActionListener { updatePreview() }
        p.add(gitIgnoreCheck, g)
        g.gridy++
        p.add(JLabel("Additional Patterns:"), g)
        g.gridx = 1
        additionalIgnoresField = JTextField(o.additionalIgnorePatterns.orEmpty(), 15)
        additionalIgnoresField.document.addDocumentListener(listener)
        p.add(additionalIgnoresField, g)
        g.gridx = 0
        g.gridy++
        invertIgnoresCheck = JCheckBox("Invert Patterns", o.invertIgnorePatterns)
        invertIgnoresCheck.addActionListener { updatePreview() }
        p.add(invertIgnoresCheck, g)
        g.gridy++
        directoryPatternCheck = JCheckBox("Directory Matching", o.enableDirectoryPatternMatching)
        directoryPatternCheck.addActionListener { updatePreview() }
        p.add(directoryPatternCheck, g)
        return p
    }

    private fun panelBinary(): JPanel {
        val g = GridBagConstraints().apply {
            insets = JBUI.insets(4)
            anchor = GridBagConstraints.WEST
            gridx = 0
            gridy = 0
        }
        val p = JPanel(GridBagLayout())
        p.border = TitledBorder("Binary Detection")
        detectBinaryCheck = JCheckBox("Detect Binary Files", o.detectBinary)
        detectBinaryCheck.addActionListener { updatePreview() }
        p.add(detectBinaryCheck, g)
        g.gridx = 0
        g.gridy++
        p.add(JLabel("Byte Check Threshold:"), g)
        g.gridx = 1
        binaryThresholdField = JTextField(o.binaryCheckThreshold.toString(), 5)
        binaryThresholdField.document.addDocumentListener(listener)
        p.add(binaryThresholdField, g)
        return p
    }

    private fun box(title: String, c: JPanel) = JPanel(BorderLayout()).also {
        it.border = TitledBorder(title)
        it.add(c, BorderLayout.CENTER)
    }
}
