package com.clipcraft.ui

import com.clipcraft.model.*
import com.clipcraft.services.ClipCraftProfileManager
import com.clipcraft.util.CodeFormatter
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.*
import javax.swing.*
import javax.swing.border.TitledBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * A settings panel that allows editing all ClipCraft options.
 * Includes options for custom header/footer, formatting, chunking/overlap, ignore patterns, and directory matching.
 * Live preview updates immediately as controls change.
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

    // Basic Output Customization
    private lateinit var includeDirectoryStructureCheckBox: JCheckBox
    private lateinit var headerTextField: JTextField
    private lateinit var footerTextField: JTextField

    // Formatting Options
    private lateinit var includeLineNumbersCheckBox: JCheckBox
    private lateinit var removeImportsCheckBox: JCheckBox
    private lateinit var removeCommentsCheckBox: JCheckBox
    private lateinit var trimWhitespaceCheckBox: JCheckBox
    private lateinit var removeEmptyLinesCheckBox: JCheckBox
    private lateinit var singleLineOutputCheckBox: JCheckBox

    // Chunking & Overlap Options
    private lateinit var chunkStrategyComboBox: JComboBox<ChunkStrategy>
    private lateinit var chunkSizeField: JTextField
    private lateinit var overlapStrategyComboBox: JComboBox<OverlapStrategy>
    private lateinit var compressionModeComboBox: JComboBox<CompressionMode>

    // Metadata & Language Options
    private lateinit var includeMetadataCheckBox: JCheckBox
    private lateinit var includeGitInfoCheckBox: JCheckBox
    private lateinit var autoDetectLanguageCheckBox: JCheckBox
    private lateinit var themeComboBox: JComboBox<ThemeMode>

    // Concurrency Options
    private lateinit var concurrencyModeComboBox: JComboBox<ConcurrencyMode>
    private lateinit var maxConcurrentTasksField: JTextField

    // **Ignore Options**:
    private lateinit var useGitIgnoreCheckBox: JCheckBox
    private lateinit var additionalIgnorePatternsField: JTextField
    private lateinit var invertIgnorePatternsCheckBox: JCheckBox
    private lateinit var enableDirectoryPatternMatchingCheckBox: JCheckBox

    private lateinit var previewArea: JTextArea
    private lateinit var mainPanel: JPanel

    override fun getDisplayName(): String = "ClipCraft"

    override fun createComponent(): JComponent {
        mainPanel = JPanel(BorderLayout())

        val settingsPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(createOutputCustomizationPanel())
            add(createGroupPanel("Formatting", createFormattingPanel()))
            add(createGroupPanel("Chunking & Overlap", createChunkingPanel()))
            add(createGroupPanel("Metadata & Language", createMetadataPanel()))
            add(createGroupPanel("Concurrency", createConcurrencyPanel()))
            add(createGroupPanel("Ignore Options", createIgnoreOptionsPanel()))
        }

        val scrollSettings = JBScrollPane(settingsPanel).apply {
            // Improve scrolling speed by increasing the unit increment
            verticalScrollBar.unitIncrement = 16
            preferredSize = Dimension(450, 600)
        }

        // Create live preview area with better performance
        previewArea = JTextArea().apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
        }
        val scrollPreview = JBScrollPane(previewArea).apply {
            verticalScrollBar.unitIncrement = 16
            preferredSize = Dimension(450, 600)
        }

        // Use a splitter to display the settings on the left and the preview on the right.
        val splitter = JBSplitter(true, 0.65f).apply {
            firstComponent = scrollSettings
            secondComponent = scrollPreview
            preferredSize = Dimension(900, 600)
        }

        // Update preview immediately on load
        updatePreview()

        return splitter
    }

    private fun createOutputCustomizationPanel(): JPanel {
        val panel = JPanel(GridBagLayout()).apply {
            border = TitledBorder("Output Customization")
        }

        val gbc = GridBagConstraints().apply {
            insets = JBUI.insets(4)
            anchor = GridBagConstraints.WEST
            gridx = 0
            gridy = 0
        }

        includeDirectoryStructureCheckBox = JCheckBox("Print directory structure?", options.includeDirectorySummary).apply {
            toolTipText = "When enabled, a directory listing will be prepended to the output."
            panel.add(this, gbc)
            addActionListener { updatePreview() }
        }

        gbc.gridy++
        panel.add(JLabel("Header Text:"), gbc)
        gbc.gridx = 1
        headerTextField = JTextField(options.gptHeaderText ?: "", 20).apply {
            toolTipText = "Text to appear at the beginning of the output (e.g., a custom header)."
            panel.add(this, gbc)
            document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent?) = updateLive()
                override fun removeUpdate(e: DocumentEvent?) = updateLive()
                override fun changedUpdate(e: DocumentEvent?) = updateLive()
            })
        }

        gbc.gridx = 0
        gbc.gridy++
        panel.add(JLabel("Footer Text:"), gbc)
        gbc.gridx = 1
        footerTextField = JTextField(options.gptFooterText ?: "", 20).apply {
            toolTipText = "Text to appear at the end of the output (e.g., a custom footer)."
            panel.add(this, gbc)
            document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent?) = updateLive()
                override fun removeUpdate(e: DocumentEvent?) = updateLive()
                override fun changedUpdate(e: DocumentEvent?) = updateLive()
            })
        }

        return panel
    }

    private fun createFormattingPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            insets = JBUI.insets(4)
            anchor = GridBagConstraints.WEST
            gridx = 0
            gridy = 0
        }

        includeLineNumbersCheckBox = JCheckBox("Include line numbers?", options.includeLineNumbers).apply {
            toolTipText = "Prepend each line with its line number."
            panel.add(this, gbc)
            addActionListener { updatePreview() }
        }

        gbc.gridy++
        removeImportsCheckBox = JCheckBox("Remove import statements?", options.removeImports).apply {
            toolTipText = "Strip out import (or include) statements from the code."
            panel.add(this, gbc)
            addActionListener { updatePreview() }
        }

        gbc.gridy++
        removeCommentsCheckBox = JCheckBox("Remove comments?", options.removeComments).apply {
            toolTipText = "Remove all single-line and multi-line comments."
            panel.add(this, gbc)
            addActionListener { updatePreview() }
        }

        gbc.gridy++
        trimWhitespaceCheckBox = JCheckBox("Trim trailing whitespace?", options.trimWhitespace).apply {
            toolTipText = "Remove extra whitespace at the end of each line."
            panel.add(this, gbc)
            addActionListener { updatePreview() }
        }

        gbc.gridy++
        removeEmptyLinesCheckBox = JCheckBox("Remove empty lines?", options.removeEmptyLines).apply {
            toolTipText = "Delete blank lines from the output."
            panel.add(this, gbc)
            addActionListener { updatePreview() }
        }

        gbc.gridy++
        singleLineOutputCheckBox = JCheckBox("Single-line output?", options.singleLineOutput).apply {
            toolTipText = "Collapse all whitespace so that the output becomes a single line."
            panel.add(this, gbc)
            addActionListener { updatePreview() }
        }

        return panel
    }

    private fun createChunkingPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            insets = JBUI.insets(4)
            anchor = GridBagConstraints.WEST
            gridx = 0
            gridy = 0
        }

        panel.add(JLabel("Chunk Strategy:"), gbc)
        gbc.gridx = 1
        chunkStrategyComboBox = ComboBox(ChunkStrategy.values()).apply {
            selectedItem = options.chunkStrategy
            toolTipText = "Select how the code output is divided into chunks."
            panel.add(this, gbc)
            addActionListener { updatePreview() }
        }

        gbc.gridx = 0
        gbc.gridy++
        panel.add(JLabel("Chunk Size:"), gbc)
        gbc.gridx = 1
        chunkSizeField = JTextField(options.chunkSize.toString(), 10).apply {
            toolTipText = "Maximum character length per chunk (for BY_SIZE strategy)."
            panel.add(this, gbc)
            document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent?) = updateLive()
                override fun removeUpdate(e: DocumentEvent?) = updateLive()
                override fun changedUpdate(e: DocumentEvent?) = updateLive()
            })
        }

        gbc.gridx = 0
        gbc.gridy++
        panel.add(JLabel("Overlap Handling:"), gbc)
        gbc.gridx = 1
        overlapStrategyComboBox = ComboBox(OverlapStrategy.values()).apply {
            selectedItem = options.overlapStrategy
            toolTipText = "Define how overlapping code between chunks is handled."
            panel.add(this, gbc)
            addActionListener { updatePreview() }
        }

        gbc.gridx = 0
        gbc.gridy++
        panel.add(JLabel("Compression Mode:"), gbc)
        gbc.gridx = 1
        compressionModeComboBox = ComboBox(CompressionMode.values()).apply {
            selectedItem = options.compressionMode
            toolTipText = "Select the level of whitespace compression."
            panel.add(this, gbc)
            addActionListener { updatePreview() }
        }

        return panel
    }

    private fun createMetadataPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            insets = JBUI.insets(4)
            anchor = GridBagConstraints.WEST
            gridx = 0
            gridy = 0
        }

        includeMetadataCheckBox = JCheckBox("Include file metadata?", options.includeMetadata).apply {
            toolTipText = "Prepend file information (name, size, timestamp) to the output."
            panel.add(this, gbc)
            addActionListener { updatePreview() }
        }

        gbc.gridy++
        includeGitInfoCheckBox = JCheckBox("Include Git info?", options.includeGitInfo).apply {
            toolTipText = "Append Git commit hash info if available."
            panel.add(this, gbc)
            addActionListener { updatePreview() }
        }

        gbc.gridy++
        autoDetectLanguageCheckBox = JCheckBox("Auto-detect language?", options.autoDetectLanguage).apply {
            toolTipText = "Guess the language based on file extension if not explicitly set."
            panel.add(this, gbc)
            addActionListener { updatePreview() }
        }

        gbc.gridy++
        val themeLabel = JLabel("Theme:")
        panel.add(themeLabel, gbc)
        gbc.gridx = 1
        themeComboBox = ComboBox(ThemeMode.values()).apply {
            selectedItem = options.themeMode
            toolTipText = "Select a light or dark theme for output highlighting."
            panel.add(this, gbc)
            addActionListener { updatePreview() }
        }

        return panel
    }

    private fun createConcurrencyPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            insets = JBUI.insets(4)
            anchor = GridBagConstraints.WEST
            gridx = 0
            gridy = 0
        }

        panel.add(JLabel("Concurrency Mode:"), gbc)
        gbc.gridx = 1
        concurrencyModeComboBox = ComboBox(ConcurrencyMode.values()).apply {
            selectedItem = options.concurrencyMode
            toolTipText = "Choose how files are processed concurrently."
            panel.add(this, gbc)
        }

        gbc.gridx = 0
        gbc.gridy++
        panel.add(JLabel("Max Concurrent Tasks:"), gbc)
        gbc.gridx = 1
        maxConcurrentTasksField = JTextField(options.maxConcurrentTasks.toString(), 10).apply {
            toolTipText = "Maximum number of concurrent tasks (if enabled)."
            panel.add(this, gbc)
        }

        return panel
    }

    // NEW: Ignore Options panel for additional patterns and directory matching
    private fun createIgnoreOptionsPanel(): JPanel {
        val panel = JPanel(GridBagLayout()).apply {
            border = TitledBorder("Ignore Options")
        }
        val gbc = GridBagConstraints().apply {
            insets = JBUI.insets(4)
            anchor = GridBagConstraints.WEST
            gridx = 0
            gridy = 0
        }

        // Must store it in a class-level field so we can reference it in isModified, apply, reset
        useGitIgnoreCheckBox = JCheckBox("Respect .gitignore", options.useGitIgnore).apply {
            toolTipText = "If enabled, the plugin will read patterns from .gitignore."
            panel.add(this, gbc)
            addActionListener { updatePreview() }
        }

        gbc.gridy++
        val additionalPatternsLabel = JLabel("Additional Ignore Patterns:")
        additionalPatternsLabel.toolTipText = "Comma-separated list of glob patterns to ignore. Use prefix '!' to invert."
        panel.add(additionalPatternsLabel, gbc)
        gbc.gridx = 1
        additionalIgnorePatternsField = JTextField(options.additionalIgnorePatterns ?: "", 20).apply {
            toolTipText = "Enter additional ignore patterns (e.g. '*.log, !important.log')."
            panel.add(this, gbc)
            document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent?) = updatePreview()
                override fun removeUpdate(e: DocumentEvent?) = updatePreview()
                override fun changedUpdate(e: DocumentEvent?) = updatePreview()
            })
        }

        gbc.gridx = 0
        gbc.gridy++
        invertIgnorePatternsCheckBox = JCheckBox("Invert Additional Patterns", options.invertIgnorePatterns).apply {
            toolTipText = "If enabled, additional patterns act as 'include' rather than 'exclude'."
            panel.add(this, gbc)
            addActionListener { updatePreview() }
        }

        gbc.gridy++
        enableDirectoryPatternMatchingCheckBox = JCheckBox("Enable Directory Pattern Matching", options.enableDirectoryPatternMatching).apply {
            toolTipText = "If enabled, directories matching the patterns are also ignored or included."
            panel.add(this, gbc)
            addActionListener { updatePreview() }
        }

        return panel
    }

    override fun isModified(): Boolean {
        // Compare UI fields to saved options (note: 'options' is the last saved state).
        if (includeDirectoryStructureCheckBox.isSelected != options.includeDirectorySummary) return true
        if (headerTextField.text != (options.gptHeaderText ?: "")) return true
        if (footerTextField.text != (options.gptFooterText ?: "")) return true

        if (includeLineNumbersCheckBox.isSelected != options.includeLineNumbers) return true
        if (removeImportsCheckBox.isSelected != options.removeImports) return true
        if (removeCommentsCheckBox.isSelected != options.removeComments) return true
        if (trimWhitespaceCheckBox.isSelected != options.trimWhitespace) return true
        if (removeEmptyLinesCheckBox.isSelected != options.removeEmptyLines) return true
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

        // New ignore options
        if (useGitIgnoreCheckBox.isSelected != options.useGitIgnore) return true
        if (additionalIgnorePatternsField.text != (options.additionalIgnorePatterns ?: "")) return true
        if (invertIgnorePatternsCheckBox.isSelected != options.invertIgnorePatterns) return true
        if (enableDirectoryPatternMatchingCheckBox.isSelected != options.enableDirectoryPatternMatching) return true

        return false
    }

    @Throws(ConfigurationException::class)
    override fun apply() {
        // Save custom header/footer and directory structure option
        options.gptHeaderText = headerTextField.text
        options.gptFooterText = footerTextField.text
        options.includeDirectorySummary = includeDirectoryStructureCheckBox.isSelected

        // Save formatting options
        options.includeLineNumbers = includeLineNumbersCheckBox.isSelected
        options.removeImports = removeImportsCheckBox.isSelected
        options.removeComments = removeCommentsCheckBox.isSelected
        options.trimWhitespace = trimWhitespaceCheckBox.isSelected
        options.removeEmptyLines = removeEmptyLinesCheckBox.isSelected
        options.singleLineOutput = singleLineOutputCheckBox.isSelected

        // Save chunking options
        options.chunkStrategy = chunkStrategyComboBox.selectedItem as ChunkStrategy
        options.chunkSize = chunkSizeField.text.toIntOrNull() ?: options.chunkSize
        options.overlapStrategy = overlapStrategyComboBox.selectedItem as OverlapStrategy
        options.compressionMode = compressionModeComboBox.selectedItem as CompressionMode

        // Save metadata / language options
        options.includeMetadata = includeMetadataCheckBox.isSelected
        options.includeGitInfo = includeGitInfoCheckBox.isSelected
        options.autoDetectLanguage = autoDetectLanguageCheckBox.isSelected
        options.themeMode = themeComboBox.selectedItem as ThemeMode

        // Save concurrency options
        options.concurrencyMode = concurrencyModeComboBox.selectedItem as ConcurrencyMode
        options.maxConcurrentTasks = maxConcurrentTasksField.text.toIntOrNull() ?: options.maxConcurrentTasks

        // Save ignore options
        options.useGitIgnore = useGitIgnoreCheckBox.isSelected
        options.additionalIgnorePatterns = additionalIgnorePatternsField.text.takeIf { it.isNotBlank() }
        options.invertIgnorePatterns = invertIgnorePatternsCheckBox.isSelected
        options.enableDirectoryPatternMatching = enableDirectoryPatternMatchingCheckBox.isSelected

        options.resolveConflicts()

        // Persist changes to the current profile
        profileManager.deleteProfile(currentProfile.profileName)
        profileManager.addProfile(currentProfile.copy(options = options))
        profileManager.switchProfile(currentProfile.profileName)
    }

    override fun reset() {
        // Reset UI fields from saved options
        includeDirectoryStructureCheckBox.isSelected = options.includeDirectorySummary
        headerTextField.text = options.gptHeaderText ?: ""
        footerTextField.text = options.gptFooterText ?: ""

        includeLineNumbersCheckBox.isSelected = options.includeLineNumbers
        removeImportsCheckBox.isSelected = options.removeImports
        removeCommentsCheckBox.isSelected = options.removeComments
        trimWhitespaceCheckBox.isSelected = options.trimWhitespace
        removeEmptyLinesCheckBox.isSelected = options.removeEmptyLines
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

        useGitIgnoreCheckBox.isSelected = options.useGitIgnore
        additionalIgnorePatternsField.text = options.additionalIgnorePatterns ?: ""
        invertIgnorePatternsCheckBox.isSelected = options.invertIgnorePatterns
        enableDirectoryPatternMatchingCheckBox.isSelected = options.enableDirectoryPatternMatching

        updatePreview()
    }

    override fun disposeUIResources() {
        // Dispose any resources if needed
    }

    private fun updateLive() {
        updatePreview()
    }

    private fun updatePreview() {
        // Build a temporary copy of options from UI fields so the preview reflects current selections
        val tempOpts = options.copy().also { temp ->
            temp.gptHeaderText = headerTextField.text
            temp.gptFooterText = footerTextField.text
            temp.includeDirectorySummary = includeDirectoryStructureCheckBox.isSelected

            temp.includeLineNumbers = includeLineNumbersCheckBox.isSelected
            temp.removeImports = removeImportsCheckBox.isSelected
            temp.removeComments = removeCommentsCheckBox.isSelected
            temp.trimWhitespace = trimWhitespaceCheckBox.isSelected
            temp.removeEmptyLines = removeEmptyLinesCheckBox.isSelected
            temp.singleLineOutput = singleLineOutputCheckBox.isSelected

            temp.chunkStrategy = chunkStrategyComboBox.selectedItem as ChunkStrategy
            temp.chunkSize = chunkSizeField.text.toIntOrNull() ?: temp.chunkSize
            temp.overlapStrategy = overlapStrategyComboBox.selectedItem as OverlapStrategy
            temp.compressionMode = compressionModeComboBox.selectedItem as CompressionMode

            temp.includeMetadata = includeMetadataCheckBox.isSelected
            temp.includeGitInfo = includeGitInfoCheckBox.isSelected
            temp.autoDetectLanguage = autoDetectLanguageCheckBox.isSelected
            temp.themeMode = themeComboBox.selectedItem as ThemeMode

            temp.concurrencyMode = concurrencyModeComboBox.selectedItem as ConcurrencyMode
            temp.maxConcurrentTasks = maxConcurrentTasksField.text.toIntOrNull() ?: temp.maxConcurrentTasks

            temp.useGitIgnore = useGitIgnoreCheckBox.isSelected
            temp.additionalIgnorePatterns = additionalIgnorePatternsField.text.takeIf { it.isNotBlank() }
            temp.invertIgnorePatterns = invertIgnorePatternsCheckBox.isSelected
            temp.enableDirectoryPatternMatching = enableDirectoryPatternMatchingCheckBox.isSelected

            temp.resolveConflicts()
        }

        // Create a sample snippet
        val snippet = Snippet(
            content = sampleSnippetText,
            fileName = "HelloWorld.java",
            relativePath = "src/com/example/HelloWorld.java"
        )

        // Format snippet using CodeFormatter
        val formatted = CodeFormatter.formatSnippets(listOf(snippet), tempOpts)
            .joinToString("\n\n")

        // Build directory structure preview if enabled
        val dirStructure = if (tempOpts.includeDirectorySummary) {
            "[DirectoryStructure]\nsrc/\n  com/\n    example/\n      HelloWorld.java\n\n"
        } else ""

        // Combine header, directory structure, formatted snippet, and footer
        val finalPreviewText = buildString {
            val previewHeader = tempOpts.gptHeaderText ?: ""
            val previewFooter = tempOpts.gptFooterText ?: ""
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

    /**
     * Utility to create a titled group panel.
     */
    private fun createGroupPanel(title: String, content: JPanel): JPanel {
        return JPanel(BorderLayout()).apply {
            border = TitledBorder(title)
            add(content, BorderLayout.CENTER)
        }
    }
}
