package com.clipcraft.ui

import com.clipcraft.model.*
import com.clipcraft.services.ClipCraftProfileManager
import com.clipcraft.util.CodeFormatter
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.*
import javax.swing.*
import javax.swing.border.TitledBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class ClipCraftSettingsConfigurable : Configurable {

    private val profileManager = ClipCraftProfileManager()
    private val currentProfile = profileManager.currentProfile().copy()
    private val options = currentProfile.options

    // For demonstration, a snippet to preview formatting changes
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

    private lateinit var includeDirectoryStructureCheckBox: JCheckBox
    private lateinit var headerTextField: JTextField
    private lateinit var footerTextField: JTextField
    private lateinit var includeLineNumbersCheckBox: JCheckBox
    private lateinit var removeImportsCheckBox: JCheckBox
    private lateinit var removeCommentsCheckBox: JCheckBox
    private lateinit var trimWhitespaceCheckBox: JCheckBox
    private lateinit var removeEmptyLinesCheckBox: JCheckBox
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
    private lateinit var useGitIgnoreCheckBox: JCheckBox
    private lateinit var additionalIgnorePatternsField: JTextField
    private lateinit var invertIgnorePatternsCheckBox: JCheckBox
    private lateinit var enableDirectoryPatternMatchingCheckBox: JCheckBox
    private lateinit var previewArea: JTextArea
    private lateinit var mainPanel: JPanel

    // Additional conflict notification
    private lateinit var chunkConflictLabel: JLabel

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

        val splitter = JBSplitter(true, 0.65f).apply {
            firstComponent = scrollSettings
            secondComponent = scrollPreview
            preferredSize = Dimension(900, 600)
        }

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

        includeDirectoryStructureCheckBox =
            JCheckBox("Print directory structure?", options.includeDirectorySummary).apply {
                toolTipText = "When enabled, a directory listing is prepended to the output."
                addActionListener { updatePreview() }
            }
        panel.add(includeDirectoryStructureCheckBox, gbc)

        gbc.gridy++
        panel.add(JLabel("Header Text:"), gbc)
        gbc.gridx = 1
        headerTextField = JTextField(options.gptHeaderText ?: "", 20).apply {
            toolTipText = "Text to appear at the beginning of the output."
            document.addDocumentListener(liveDocumentListener)
        }
        panel.add(headerTextField, gbc)

        gbc.gridx = 0
        gbc.gridy++
        panel.add(JLabel("Footer Text:"), gbc)
        gbc.gridx = 1
        footerTextField = JTextField(options.gptFooterText ?: "", 20).apply {
            toolTipText = "Text to appear at the end of the output."
            document.addDocumentListener(liveDocumentListener)
        }
        panel.add(footerTextField, gbc)

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
            addActionListener { updatePreview() }
        }
        panel.add(includeLineNumbersCheckBox, gbc)

        gbc.gridy++
        removeImportsCheckBox = JCheckBox("Remove import statements?", options.removeImports).apply {
            addActionListener { updatePreview() }
        }
        panel.add(removeImportsCheckBox, gbc)

        gbc.gridy++
        removeCommentsCheckBox = JCheckBox("Remove comments?", options.removeComments).apply {
            addActionListener { updatePreview() }
        }
        panel.add(removeCommentsCheckBox, gbc)

        gbc.gridy++
        trimWhitespaceCheckBox = JCheckBox("Trim trailing whitespace?", options.trimWhitespace).apply {
            addActionListener { updatePreview() }
        }
        panel.add(trimWhitespaceCheckBox, gbc)

        gbc.gridy++
        removeEmptyLinesCheckBox = JCheckBox("Remove empty lines?", options.removeEmptyLines).apply {
            addActionListener { updatePreview() }
        }
        panel.add(removeEmptyLinesCheckBox, gbc)

        gbc.gridy++
        singleLineOutputCheckBox = JCheckBox("Single-line output?", options.singleLineOutput).apply {
            addActionListener {
                updatePreview()
                updateChunkUIState()
            }
        }
        panel.add(singleLineOutputCheckBox, gbc)

        return panel
    }

    private fun createChunkingPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.border = TitledBorder("Chunking & Overlap")
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
            toolTipText = "How to split code into chunks."
            addActionListener {
                updatePreview()
                updateChunkUIState()
            }
        }
        panel.add(chunkStrategyComboBox, gbc)

        gbc.gridx = 0
        gbc.gridy++
        panel.add(JLabel("Chunk Size:"), gbc)
        gbc.gridx = 1
        chunkSizeField = JTextField(options.chunkSize.toString(), 10).apply {
            toolTipText = "Maximum characters per chunk (for BY_SIZE)."
            document.addDocumentListener(liveDocumentListener)
        }
        panel.add(chunkSizeField, gbc)

        gbc.gridx = 0
        gbc.gridy++
        panel.add(JLabel("Overlap Strategy:"), gbc)
        gbc.gridx = 1
        overlapStrategyComboBox = ComboBox(OverlapStrategy.values()).apply {
            selectedItem = options.overlapStrategy
            toolTipText = "How chunk overlaps are handled."
            addActionListener { updatePreview() }
        }
        panel.add(overlapStrategyComboBox, gbc)

        gbc.gridx = 0
        gbc.gridy++
        panel.add(JLabel("Compression Mode:"), gbc)
        gbc.gridx = 1
        compressionModeComboBox = ComboBox(CompressionMode.values()).apply {
            selectedItem = options.compressionMode
            toolTipText = "Select the level of whitespace compression."
            addActionListener { updatePreview() }
        }
        panel.add(compressionModeComboBox, gbc)

        // Conflict label
        gbc.gridx = 0
        gbc.gridy++
        gbc.gridwidth = 2
        chunkConflictLabel = JLabel("").apply {
            foreground = JBColor.RED
        }
        panel.add(chunkConflictLabel, gbc)

        updateChunkUIState() // for initial load
        return panel
    }

    private fun createMetadataPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.border = TitledBorder("Metadata & Language")
        val gbc = GridBagConstraints().apply {
            insets = JBUI.insets(4)
            anchor = GridBagConstraints.WEST
            gridx = 0
            gridy = 0
        }

        includeMetadataCheckBox = JCheckBox("Include file metadata?", options.includeMetadata).apply {
            toolTipText = "Prepend file info to snippet output."
            addActionListener { updatePreview() }
        }
        panel.add(includeMetadataCheckBox, gbc)

        gbc.gridy++
        includeGitInfoCheckBox = JCheckBox("Include Git info?", options.includeGitInfo).apply {
            toolTipText = "Append Git commit hash if available."
            addActionListener { updatePreview() }
        }
        panel.add(includeGitInfoCheckBox, gbc)

        gbc.gridy++
        autoDetectLanguageCheckBox = JCheckBox("Auto-detect language?", options.autoDetectLanguage).apply {
            toolTipText = "Guess language based on file extension."
            addActionListener { updatePreview() }
        }
        panel.add(autoDetectLanguageCheckBox, gbc)

        gbc.gridy++
        val themeLabel = JLabel("Theme:")
        panel.add(themeLabel, gbc)
        gbc.gridx = 1
        themeComboBox = ComboBox(ThemeMode.values()).apply {
            selectedItem = options.themeMode
            toolTipText = "Choose light or dark theme for highlighting."
            addActionListener { updatePreview() }
        }
        panel.add(themeComboBox, gbc)

        return panel
    }

    private fun createConcurrencyPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.border = TitledBorder("Concurrency")
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
            addActionListener { updateConcurrencyUIState() }
        }
        panel.add(concurrencyModeComboBox, gbc)

        gbc.gridx = 0
        gbc.gridy++
        panel.add(JLabel("Max Concurrent Tasks:"), gbc)
        gbc.gridx = 1
        maxConcurrentTasksField = JTextField(options.maxConcurrentTasks.toString(), 10)
        panel.add(maxConcurrentTasksField, gbc)

        updateConcurrencyUIState()
        return panel
    }

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

        useGitIgnoreCheckBox = JCheckBox("Respect .gitignore", options.useGitIgnore).apply {
            toolTipText = "If enabled, plugin reads ignore patterns from .gitignore."
            addActionListener { updatePreview() }
        }
        panel.add(useGitIgnoreCheckBox, gbc)

        gbc.gridy++
        val additionalPatternsLabel = JLabel("Additional Ignore Patterns:")
        panel.add(additionalPatternsLabel, gbc)
        gbc.gridx = 1
        additionalIgnorePatternsField = JTextField(options.additionalIgnorePatterns ?: "", 20).apply {
            toolTipText = "Comma-separated list of patterns to ignore. Use '!' to invert."
            document.addDocumentListener(liveDocumentListener)
        }
        panel.add(additionalIgnorePatternsField, gbc)

        gbc.gridx = 0
        gbc.gridy++
        invertIgnorePatternsCheckBox = JCheckBox("Invert Additional Patterns", options.invertIgnorePatterns).apply {
            toolTipText = "If enabled, these patterns become 'include' rather than 'exclude'."
            addActionListener { updatePreview() }
        }
        panel.add(invertIgnorePatternsCheckBox, gbc)

        gbc.gridy++
        enableDirectoryPatternMatchingCheckBox =
            JCheckBox("Enable Directory Pattern Matching", options.enableDirectoryPatternMatching).apply {
                toolTipText = "If enabled, directories matching patterns are also ignored/included."
                addActionListener { updatePreview() }
            }
        panel.add(enableDirectoryPatternMatchingCheckBox, gbc)

        return panel
    }

    override fun isModified(): Boolean {
        // Compare all relevant fields with the 'options' object.
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
        if (useGitIgnoreCheckBox.isSelected != options.useGitIgnore) return true
        if (additionalIgnorePatternsField.text != (options.additionalIgnorePatterns ?: "")) return true
        if (invertIgnorePatternsCheckBox.isSelected != options.invertIgnorePatterns) return true
        if (enableDirectoryPatternMatchingCheckBox.isSelected != options.enableDirectoryPatternMatching) return true

        return false
    }

    @Throws(ConfigurationException::class)
    override fun apply() {
        options.gptHeaderText = headerTextField.text
        options.gptFooterText = footerTextField.text
        options.includeDirectorySummary = includeDirectoryStructureCheckBox.isSelected
        options.includeLineNumbers = includeLineNumbersCheckBox.isSelected
        options.removeImports = removeImportsCheckBox.isSelected
        options.removeComments = removeCommentsCheckBox.isSelected
        options.trimWhitespace = trimWhitespaceCheckBox.isSelected
        options.removeEmptyLines = removeEmptyLinesCheckBox.isSelected
        options.singleLineOutput = singleLineOutputCheckBox.isSelected
        options.chunkStrategy = chunkStrategyComboBox.selectedItem as ChunkStrategy
        options.chunkSize = chunkSizeField.text.toIntOrNull() ?: options.chunkSize
        options.overlapStrategy = overlapStrategyComboBox.selectedItem as OverlapStrategy
        options.compressionMode = compressionModeComboBox.selectedItem as CompressionMode
        options.includeMetadata = includeMetadataCheckBox.isSelected
        options.includeGitInfo = includeGitInfoCheckBox.isSelected
        options.autoDetectLanguage = autoDetectLanguageCheckBox.isSelected
        options.themeMode = themeComboBox.selectedItem as ThemeMode
        options.concurrencyMode = concurrencyModeComboBox.selectedItem as ConcurrencyMode
        options.maxConcurrentTasks = maxConcurrentTasksField.text.toIntOrNull() ?: options.maxConcurrentTasks
        options.useGitIgnore = useGitIgnoreCheckBox.isSelected
        options.additionalIgnorePatterns = additionalIgnorePatternsField.text.takeIf { it.isNotBlank() }
        options.invertIgnorePatterns = invertIgnorePatternsCheckBox.isSelected
        options.enableDirectoryPatternMatching = enableDirectoryPatternMatchingCheckBox.isSelected

        options.resolveConflicts()

        // Persist changes back to the current profile
        profileManager.deleteProfile(currentProfile.profileName)
        profileManager.addProfile(currentProfile.copy(options = options))
        profileManager.switchProfile(currentProfile.profileName)
    }

    override fun reset() {
        includeDirectoryStructureCheckBox.isSelected = options.includeDirectorySummary
        headerTextField.text = options.gptHeaderText.orEmpty()
        footerTextField.text = options.gptFooterText.orEmpty()
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
        additionalIgnorePatternsField.text = options.additionalIgnorePatterns.orEmpty()
        invertIgnorePatternsCheckBox.isSelected = options.invertIgnorePatterns
        enableDirectoryPatternMatchingCheckBox.isSelected = options.enableDirectoryPatternMatching

        updatePreview()
        updateChunkUIState()
        updateConcurrencyUIState()
    }

    override fun disposeUIResources() {
        // no-op
    }

    private val liveDocumentListener = object : DocumentListener {
        override fun insertUpdate(e: DocumentEvent?) = updatePreview()
        override fun removeUpdate(e: DocumentEvent?) = updatePreview()
        override fun changedUpdate(e: DocumentEvent?) = updatePreview()
    }

    private fun updatePreview() {
        // Build a temporary copy of options from UI fields so the preview reflects current selections.
        val tempOpts = options.copy().apply {
            gptHeaderText = headerTextField.text
            gptFooterText = footerTextField.text
            includeDirectorySummary = includeDirectoryStructureCheckBox.isSelected
            includeLineNumbers = includeLineNumbersCheckBox.isSelected
            removeImports = removeImportsCheckBox.isSelected
            removeComments = removeCommentsCheckBox.isSelected
            trimWhitespace = trimWhitespaceCheckBox.isSelected
            removeEmptyLines = removeEmptyLinesCheckBox.isSelected
            singleLineOutput = singleLineOutputCheckBox.isSelected
            chunkStrategy = chunkStrategyComboBox.selectedItem as ChunkStrategy
            chunkSize = chunkSizeField.text.toIntOrNull() ?: chunkSize
            overlapStrategy = overlapStrategyComboBox.selectedItem as OverlapStrategy
            compressionMode = compressionModeComboBox.selectedItem as CompressionMode
            includeMetadata = includeMetadataCheckBox.isSelected
            includeGitInfo = includeGitInfoCheckBox.isSelected
            autoDetectLanguage = autoDetectLanguageCheckBox.isSelected
            themeMode = themeComboBox.selectedItem as ThemeMode
            concurrencyMode = concurrencyModeComboBox.selectedItem as ConcurrencyMode
            maxConcurrentTasks = maxConcurrentTasksField.text.toIntOrNull() ?: maxConcurrentTasks
            useGitIgnore = useGitIgnoreCheckBox.isSelected
            additionalIgnorePatterns = additionalIgnorePatternsField.text.takeIf { it.isNotBlank() }
            invertIgnorePatterns = invertIgnorePatternsCheckBox.isSelected
            enableDirectoryPatternMatching = enableDirectoryPatternMatchingCheckBox.isSelected
            resolveConflicts()
        }

        val snippet = Snippet(
            content = sampleSnippetText,
            fileName = "HelloWorld.java",
            relativePath = "src/com/example/HelloWorld.java"
        )

        val formatted = CodeFormatter.formatSnippets(listOf(snippet), tempOpts)
            .joinToString("\n\n")

        val dirStructure = if (tempOpts.includeDirectorySummary) {
            "[DirectoryStructure]\nsrc/\n  com/\n    example/\n      HelloWorld.java\n\n"
        } else ""

        val finalPreviewText = buildString {
            val previewHeader = tempOpts.gptHeaderText.orEmpty()
            val previewFooter = tempOpts.gptFooterText.orEmpty()
            if (previewHeader.isNotEmpty()) append(previewHeader).append("\n\n")
            append(dirStructure)
            append(formatted)
            if (previewFooter.isNotEmpty()) {
                append("\n\n").append(previewFooter)
            }
        }

        previewArea.text = finalPreviewText
        previewArea.caretPosition = 0

        updateChunkUIState()
    }

    private fun updateChunkUIState() {
        val singleLine = singleLineOutputCheckBox.isSelected
        val strategy = chunkStrategyComboBox.selectedItem as ChunkStrategy

        // If single-line is on, chunking is effectively disabled
        val chunkEnabled = !singleLine && strategy != ChunkStrategy.NONE
        chunkSizeField.isEnabled = chunkEnabled && strategy == ChunkStrategy.BY_SIZE
        overlapStrategyComboBox.isEnabled = chunkEnabled
        compressionModeComboBox.isEnabled = true

        if (singleLine) {
            chunkConflictLabel.text = "Single-line output is active; chunking is disabled."
        } else {
            chunkConflictLabel.text = if (strategy == ChunkStrategy.NONE) {
                "Chunking is disabled."
            } else {
                ""
            }
        }
    }

    private fun updateConcurrencyUIState() {
        val mode = concurrencyModeComboBox.selectedItem as ConcurrencyMode
        maxConcurrentTasksField.isEnabled = (mode != ConcurrencyMode.DISABLED)
    }

    private fun createGroupPanel(title: String, content: JPanel): JPanel {
        return JPanel(BorderLayout()).apply {
            border = TitledBorder(title)
            add(content, BorderLayout.CENTER)
        }
    }
}
