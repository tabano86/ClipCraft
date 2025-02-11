package com.clipcraft.ui

import com.clipcraft.model.*
import com.clipcraft.services.ClipCraftProfileManager
import com.clipcraft.util.CodeFormatter
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.ui.JBSplitter
import java.awt.*
import javax.swing.*
import javax.swing.border.TitledBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * A settings panel that allows editing all ClipCraft options, including
 * new fields for custom header/footer and directory-structure printing.
 * Live preview is updated as soon as fields change.
 */
class ClipCraftSettingsConfigurable : Configurable {

    private val profileManager = ClipCraftProfileManager()
    private val currentProfile = profileManager.currentProfile().copy()
    private val options = currentProfile.options

    private val sampleSnippetText = """
        // Sample HelloWorld in Java
        package com.example;
        
        import java.util.*;
        
        public class HelloWorld {
            /* A multi-line
               comment */
            public static void main(String[] args) {
                System.out.println("Hello, World!");
            }
        }
    """.trimIndent()

    // UI components
    private lateinit var includeDirectoryStructureCheckBox: JCheckBox
    private lateinit var headerTextField: JTextField
    private lateinit var footerTextField: JTextField

    private lateinit var includeLineNumbersCheckBox: JCheckBox
    private lateinit var removeImportsCheckBox: JCheckBox
    private lateinit var removeCommentsCheckBox: JCheckBox
    private lateinit var trimWhitespaceCheckBox: JCheckBox
    private lateinit var singleLineOutputCheckBox: JCheckBox

    private lateinit var chunkStrategyComboBox: JComboBox<ChunkStrategy>
    private lateinit var chunkSizeField: JTextField
    private lateinit var overlapStrategyComboBox: JComboBox<OverlapStrategy>
    private lateinit var compressionModeComboBox: JComboBox<CompressionMode>

    private lateinit var includeMetadataCheckBox: JCheckBox
    private lateinit var includeGitInfoCheckBox: JCheckBox
    private lateinit var autoDetectLanguageCheckBox: JCheckBox
    private lateinit var themeComboBox: JComboBox<ThemeMode>

    private lateinit var concurrencyModeComboBox: JComboBox<ConcurrencyMode>
    private lateinit var maxConcurrentTasksField: JTextField

    private lateinit var previewArea: JTextArea
    private lateinit var mainPanel: JPanel

    override fun getDisplayName(): String = "ClipCraft"

    override fun createComponent(): JComponent {
        mainPanel = JPanel(BorderLayout())

        val settingsPanel = JPanel()
        settingsPanel.layout = BoxLayout(settingsPanel, BoxLayout.Y_AXIS)
        settingsPanel.add(createOutputCustomizationPanel())
        settingsPanel.add(createGroupPanel("Formatting", createFormattingPanel()))
        settingsPanel.add(createGroupPanel("Chunking & Overlap", createChunkingPanel()))
        settingsPanel.add(createGroupPanel("Metadata & Language", createMetadataPanel()))
        settingsPanel.add(createGroupPanel("Concurrency", createConcurrencyPanel()))

        val scrollSettings = JScrollPane(settingsPanel)
        scrollSettings.preferredSize = Dimension(450, 600)

        // Create live preview area
        previewArea = JTextArea().apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
        }
        val scrollPreview = JScrollPane(previewArea)
        scrollPreview.preferredSize = Dimension(450, 600)

        // Use a splitter to display the settings on the left and preview on the right.
        val splitter = JBSplitter(true, 0.65f).apply {
            firstComponent = scrollSettings
            secondComponent = scrollPreview
            preferredSize = Dimension(900, 600)
        }

        // Initially load from existing options
        updatePreview()

        return splitter
    }

    private fun createOutputCustomizationPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.border = TitledBorder("Output Customization")

        val gbc = GridBagConstraints().apply {
            insets = Insets(4, 4, 4, 4)
            anchor = GridBagConstraints.WEST
            gridx = 0
            gridy = 0
        }

        // Check box: "Include Directory Structure"
        includeDirectoryStructureCheckBox = JCheckBox("Print directory structure?", options.includeDirectorySummary)
        panel.add(includeDirectoryStructureCheckBox, gbc)
        includeDirectoryStructureCheckBox.addActionListener {
            options.includeDirectorySummary = includeDirectoryStructureCheckBox.isSelected
            updatePreview()
        }

        // Next row: header
        gbc.gridy++
        panel.add(JLabel("Header Text:"), gbc)
        gbc.gridx = 1
        headerTextField = JTextField(options.gptHeaderText ?: "", 20)
        panel.add(headerTextField, gbc)
        headerTextField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = updateLive()
            override fun removeUpdate(e: DocumentEvent?) = updateLive()
            override fun changedUpdate(e: DocumentEvent?) = updateLive()
        })

        // Next row: footer
        gbc.gridx = 0
        gbc.gridy++
        panel.add(JLabel("Footer Text:"), gbc)
        gbc.gridx = 1
        footerTextField = JTextField(options.gptFooterText ?: "", 20)
        panel.add(footerTextField, gbc)
        footerTextField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = updateLive()
            override fun removeUpdate(e: DocumentEvent?) = updateLive()
            override fun changedUpdate(e: DocumentEvent?) = updateLive()
        })

        return panel
    }

    private fun createFormattingPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            insets = Insets(4, 4, 4, 4)
            anchor = GridBagConstraints.WEST
            gridx = 0
            gridy = 0
        }

        includeLineNumbersCheckBox = JCheckBox("Include line numbers?", options.includeLineNumbers)
        panel.add(includeLineNumbersCheckBox, gbc)
        includeLineNumbersCheckBox.addActionListener { updatePreview() }

        gbc.gridy++
        removeImportsCheckBox = JCheckBox("Remove import statements?", options.removeImports)
        panel.add(removeImportsCheckBox, gbc)
        removeImportsCheckBox.addActionListener { updatePreview() }

        gbc.gridy++
        removeCommentsCheckBox = JCheckBox("Remove comments?", options.removeComments)
        panel.add(removeCommentsCheckBox, gbc)
        removeCommentsCheckBox.addActionListener { updatePreview() }

        gbc.gridy++
        trimWhitespaceCheckBox = JCheckBox("Trim trailing whitespace?", options.trimWhitespace)
        panel.add(trimWhitespaceCheckBox, gbc)
        trimWhitespaceCheckBox.addActionListener { updatePreview() }

        gbc.gridy++
        singleLineOutputCheckBox = JCheckBox("Single-line output?", options.singleLineOutput)
        panel.add(singleLineOutputCheckBox, gbc)
        singleLineOutputCheckBox.addActionListener { updatePreview() }

        return panel
    }

    private fun createChunkingPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            insets = Insets(4, 4, 4, 4)
            anchor = GridBagConstraints.WEST
            gridx = 0
            gridy = 0
        }

        panel.add(JLabel("Chunk Strategy:"), gbc)
        gbc.gridx = 1
        chunkStrategyComboBox = JComboBox(ChunkStrategy.values())
        chunkStrategyComboBox.selectedItem = options.chunkStrategy
        panel.add(chunkStrategyComboBox, gbc)
        chunkStrategyComboBox.addActionListener { updatePreview() }

        gbc.gridx = 0
        gbc.gridy++
        panel.add(JLabel("Chunk Size:"), gbc)
        gbc.gridx = 1
        chunkSizeField = JTextField(options.chunkSize.toString(), 10)
        panel.add(chunkSizeField, gbc)
        chunkSizeField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = updateLive()
            override fun removeUpdate(e: DocumentEvent?) = updateLive()
            override fun changedUpdate(e: DocumentEvent?) = updateLive()
        })

        gbc.gridx = 0
        gbc.gridy++
        panel.add(JLabel("Overlap Handling:"), gbc)
        gbc.gridx = 1
        overlapStrategyComboBox = JComboBox(OverlapStrategy.values())
        overlapStrategyComboBox.selectedItem = options.overlapStrategy
        panel.add(overlapStrategyComboBox, gbc)
        overlapStrategyComboBox.addActionListener { updatePreview() }

        gbc.gridx = 0
        gbc.gridy++
        panel.add(JLabel("Compression Mode:"), gbc)
        gbc.gridx = 1
        compressionModeComboBox = JComboBox(CompressionMode.values())
        compressionModeComboBox.selectedItem = options.compressionMode
        panel.add(compressionModeComboBox, gbc)
        compressionModeComboBox.addActionListener { updatePreview() }

        return panel
    }

    private fun createMetadataPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            insets = Insets(4, 4, 4, 4)
            anchor = GridBagConstraints.WEST
            gridx = 0
            gridy = 0
        }
        includeMetadataCheckBox = JCheckBox("Include file metadata?", options.includeMetadata)
        panel.add(includeMetadataCheckBox, gbc)
        includeMetadataCheckBox.addActionListener { updatePreview() }

        gbc.gridy++
        includeGitInfoCheckBox = JCheckBox("Include Git info?", options.includeGitInfo)
        panel.add(includeGitInfoCheckBox, gbc)
        includeGitInfoCheckBox.addActionListener { updatePreview() }

        gbc.gridy++
        autoDetectLanguageCheckBox = JCheckBox("Auto-detect language?", options.autoDetectLanguage)
        panel.add(autoDetectLanguageCheckBox, gbc)
        autoDetectLanguageCheckBox.addActionListener { updatePreview() }

        gbc.gridy++
        val themeLabel = JLabel("Theme:")
        panel.add(themeLabel, gbc)
        gbc.gridx = 1
        themeComboBox = JComboBox(ThemeMode.values())
        themeComboBox.selectedItem = options.themeMode
        panel.add(themeComboBox, gbc)
        // Theme doesn't usually affect code text, no immediate listener needed, but let's do it anyway
        themeComboBox.addActionListener { /* Possibly no direct effect on snippet text. */ }

        return panel
    }

    private fun createConcurrencyPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            insets = Insets(4, 4, 4, 4)
            anchor = GridBagConstraints.WEST
            gridx = 0
            gridy = 0
        }
        panel.add(JLabel("Concurrency Mode:"), gbc)
        gbc.gridx = 1
        concurrencyModeComboBox = JComboBox(ConcurrencyMode.values())
        concurrencyModeComboBox.selectedItem = options.concurrencyMode
        panel.add(concurrencyModeComboBox, gbc)

        gbc.gridx = 0
        gbc.gridy++
        panel.add(JLabel("Max Concurrent Tasks:"), gbc)
        gbc.gridx = 1
        maxConcurrentTasksField = JTextField(options.maxConcurrentTasks.toString(), 10)
        panel.add(maxConcurrentTasksField, gbc)

        return panel
    }

    private fun createGroupPanel(title: String, content: JPanel): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = TitledBorder(title)
        panel.add(content, BorderLayout.CENTER)
        return panel
    }

    override fun isModified(): Boolean {
        if (includeDirectoryStructureCheckBox.isSelected != options.includeDirectorySummary) return true
        if (headerTextField.text != (options.gptHeaderText ?: "")) return true
        if (footerTextField.text != (options.gptFooterText ?: "")) return true

        if (includeLineNumbersCheckBox.isSelected != options.includeLineNumbers) return true
        if (removeImportsCheckBox.isSelected != options.removeImports) return true
        if (removeCommentsCheckBox.isSelected != options.removeComments) return true
        if (trimWhitespaceCheckBox.isSelected != options.trimWhitespace) return true
        if (singleLineOutputCheckBox.isSelected != options.singleLineOutput) return true

        if (chunkStrategyComboBox.selectedItem != options.chunkStrategy) return true
        if (chunkSizeField.text != options.chunkSize.toString()) return true
        if (overlapStrategyComboBox.selectedItem != options.overlapStrategy) return true
        if (compressionModeComboBox.selectedItem != options.compressionMode) return true

        if (includeMetadataCheckBox.isSelected != options.includeMetadata) return true
        if (includeGitInfoCheckBox.isSelected != options.includeGitInfo) return true
        if (autoDetectLanguageCheckBox.isSelected != options.autoDetectLanguage) return true
        if (themeComboBox.selectedItem != options.themeMode) return true

        if (concurrencyModeComboBox.selectedItem != options.concurrencyMode) return true
        if (maxConcurrentTasksField.text != options.maxConcurrentTasks.toString()) return true

        return false
    }

    @Throws(ConfigurationException::class)
    override fun apply() {
        // Save custom header/footer
        options.gptHeaderText = headerTextField.text
        options.gptFooterText = footerTextField.text
        options.includeDirectorySummary = includeDirectoryStructureCheckBox.isSelected

        // Save formatting
        options.includeLineNumbers = includeLineNumbersCheckBox.isSelected
        options.removeImports = removeImportsCheckBox.isSelected
        options.removeComments = removeCommentsCheckBox.isSelected
        options.trimWhitespace = trimWhitespaceCheckBox.isSelected
        options.singleLineOutput = singleLineOutputCheckBox.isSelected

        // Save chunking
        options.chunkStrategy = chunkStrategyComboBox.selectedItem as ChunkStrategy
        options.chunkSize = chunkSizeField.text.toIntOrNull() ?: options.chunkSize
        options.overlapStrategy = overlapStrategyComboBox.selectedItem as OverlapStrategy
        options.compressionMode = compressionModeComboBox.selectedItem as CompressionMode

        // Save metadata / language
        options.includeMetadata = includeMetadataCheckBox.isSelected
        options.includeGitInfo = includeGitInfoCheckBox.isSelected
        options.autoDetectLanguage = autoDetectLanguageCheckBox.isSelected
        options.themeMode = themeComboBox.selectedItem as ThemeMode

        // Save concurrency
        options.concurrencyMode = concurrencyModeComboBox.selectedItem as ConcurrencyMode
        options.maxConcurrentTasks = maxConcurrentTasksField.text.toIntOrNull() ?: options.maxConcurrentTasks

        // Reconcile conflicts
        options.resolveConflicts()

        // Persist
        profileManager.deleteProfile(currentProfile.profileName)
        profileManager.addProfile(currentProfile.copy(options = options))
        profileManager.switchProfile(currentProfile.profileName)
    }

    override fun reset() {
        includeDirectoryStructureCheckBox.isSelected = options.includeDirectorySummary
        headerTextField.text = options.gptHeaderText ?: ""
        footerTextField.text = options.gptFooterText ?: ""

        includeLineNumbersCheckBox.isSelected = options.includeLineNumbers
        removeImportsCheckBox.isSelected = options.removeImports
        removeCommentsCheckBox.isSelected = options.removeComments
        trimWhitespaceCheckBox.isSelected = options.trimWhitespace
        singleLineOutputCheckBox.isSelected = options.singleLineOutput

        chunkStrategyComboBox.selectedItem = options.chunkStrategy
        chunkSizeField.text = options.chunkSize.toString()
        overlapStrategyComboBox.selectedItem = options.overlapStrategy
        compressionModeComboBox.selectedItem = options.compressionMode

        includeMetadataCheckBox.isSelected = options.includeMetadata
        includeGitInfoCheckBox.isSelected = options.includeGitInfo
        autoDetectLanguageCheckBox.isSelected = options.autoDetectLanguage
        themeComboBox.selectedItem = options.themeMode

        concurrencyModeComboBox.selectedItem = options.concurrencyMode
        maxConcurrentTasksField.text = options.maxConcurrentTasks.toString()

        updatePreview()
    }

    override fun disposeUIResources() {
        // If you need to dispose references, do it here
    }

    /**
     * Called whenever a control changes to update the preview snippet.
     */
    private fun updateLive() {
        updatePreview()
    }

    /**
     * Reformats the sample snippet using the current in-memory [options]
     * and updates the preview text area.
     */
    private fun updatePreview() {
        // Using the three-argument constructor for Snippet
        val snippet = Snippet(
            content = sampleSnippetText,
            fileName = "HelloWorld.java",
            relativePath = "src/com/example/HelloWorld.java"
        )

        val tempOpts = options.copy().also { it.resolveConflicts() }
        val previewHeader = tempOpts.gptHeaderText ?: ""
        val previewFooter = tempOpts.gptFooterText ?: ""

        // Format snippet
        val formatted = CodeFormatter.formatSnippets(listOf(snippet), tempOpts)
            .joinToString("\n\n")

        // Show directory structure if needed
        val dirStructure = if (tempOpts.includeDirectorySummary) {
            "[DirectoryStructure]\nsrc/\n  com/\n    example/\n      HelloWorld.java\n\n"
        } else ""

        val finalPreviewText = buildString {
            append(previewHeader)
            if (previewHeader.isNotEmpty()) append("\n\n")
            append(dirStructure)
            append(formatted)
            if (previewFooter.isNotEmpty()) {
                append("\n\n").append(previewFooter)
            }
        }

        previewArea.text = finalPreviewText
        previewArea.caretPosition = 0
    }
}
