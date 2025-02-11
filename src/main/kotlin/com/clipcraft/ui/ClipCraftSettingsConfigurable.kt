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

class ClipCraftSettingsConfigurable : Configurable {

    private val profileManager = ClipCraftProfileManager()
    private val currentProfile = profileManager.currentProfile().copy()
    private val options = currentProfile.options

    // Sample snippet used for live preview
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
    private lateinit var includeLineNumbersCheckBox: JCheckBox
    private lateinit var removeImportsCheckBox: JCheckBox
    private lateinit var removeCommentsCheckBox: JCheckBox
    private lateinit var trimWhitespaceCheckBox: JCheckBox
    private lateinit var singleLineOutputCheckBox: JCheckBox

    private lateinit var chunkStrategyComboBox: ComboBox<ChunkStrategy>
    private lateinit var chunkSizeField: JTextField
    private lateinit var overlapStrategyComboBox: ComboBox<OverlapStrategy>
    private lateinit var compressionModeComboBox: ComboBox<CompressionMode>

    private lateinit var includeMetadataCheckBox: JCheckBox
    private lateinit var includeGitInfoCheckBox: JCheckBox
    private lateinit var autoDetectLanguageCheckBox: JCheckBox
    private lateinit var themeComboBox: ComboBox<ThemeMode>

    private lateinit var concurrencyModeComboBox: ComboBox<ConcurrencyMode>
    private lateinit var maxConcurrentTasksField: JTextField

    private lateinit var previewArea: JTextArea
    private lateinit var mainPanel: JPanel

    override fun getDisplayName(): String = "ClipCraft"

    override fun createComponent(): JComponent {
        // Build the settings panel manually.
        mainPanel = JPanel(BorderLayout())

        val settingsPanel = JPanel()
        settingsPanel.layout = BoxLayout(settingsPanel, BoxLayout.Y_AXIS)
        settingsPanel.add(createGroupPanel("Formatting", createFormattingPanel()))
        settingsPanel.add(createGroupPanel("Chunking & Overlap", createChunkingPanel()))
        settingsPanel.add(createGroupPanel("Metadata & Language", createMetadataPanel()))
        settingsPanel.add(createGroupPanel("Concurrency", createConcurrencyPanel()))

        val scrollSettings = JBScrollPane(settingsPanel)
        scrollSettings.preferredSize = Dimension(450, 600)

        // Create live preview area.
        previewArea = JTextArea()
        previewArea.isEditable = false
        previewArea.lineWrap = true
        previewArea.wrapStyleWord = true
        val scrollPreview = JBScrollPane(previewArea)
        scrollPreview.preferredSize = Dimension(450, 600)

        // Use a split pane to display settings on left and preview on right.
        val splitter = JBSplitter(true, 0.65f).apply {
            firstComponent = scrollSettings
            secondComponent = scrollPreview
            preferredSize = Dimension(900, 600)
        }

        updatePreview()
        return splitter
    }

    private fun createGroupPanel(title: String, content: JPanel): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = TitledBorder(title)
        panel.add(content, BorderLayout.CENTER)
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
        includeLineNumbersCheckBox = JCheckBox("Include line numbers?", options.includeLineNumbers)
        panel.add(includeLineNumbersCheckBox, gbc)

        gbc.gridy++
        removeImportsCheckBox = JCheckBox("Remove import statements?", options.removeImports)
        panel.add(removeImportsCheckBox, gbc)

        gbc.gridy++
        removeCommentsCheckBox = JCheckBox("Remove comments?", options.removeComments)
        panel.add(removeCommentsCheckBox, gbc)

        gbc.gridy++
        trimWhitespaceCheckBox = JCheckBox("Trim trailing whitespace?", options.trimWhitespace)
        panel.add(trimWhitespaceCheckBox, gbc)

        gbc.gridy++
        singleLineOutputCheckBox = JCheckBox("Single-line output?", options.singleLineOutput)
        panel.add(singleLineOutputCheckBox, gbc)

        return panel
    }

    private fun createChunkingPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            insets = JBUI.insets(4)
            anchor = GridBagConstraints.WEST
        }
        gbc.gridx = 0
        gbc.gridy = 0
        panel.add(JLabel("Chunk Strategy:"), gbc)
        gbc.gridx = 1
        chunkStrategyComboBox = ComboBox(ChunkStrategy.values())
        chunkStrategyComboBox.selectedItem = options.chunkStrategy
        panel.add(chunkStrategyComboBox, gbc)

        gbc.gridx = 0
        gbc.gridy++
        panel.add(JLabel("Chunk Size:"), gbc)
        gbc.gridx = 1
        chunkSizeField = JTextField(options.chunkSize.toString(), 10)
        panel.add(chunkSizeField, gbc)

        gbc.gridx = 0
        gbc.gridy++
        panel.add(JLabel("Overlap Handling:"), gbc)
        gbc.gridx = 1
        overlapStrategyComboBox = ComboBox(OverlapStrategy.values())
        overlapStrategyComboBox.selectedItem = options.overlapStrategy
        panel.add(overlapStrategyComboBox, gbc)

        gbc.gridx = 0
        gbc.gridy++
        panel.add(JLabel("Compression Mode:"), gbc)
        gbc.gridx = 1
        compressionModeComboBox = ComboBox(CompressionMode.values())
        compressionModeComboBox.selectedItem = options.compressionMode
        panel.add(compressionModeComboBox, gbc)

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
        includeMetadataCheckBox = JCheckBox("Include file metadata?", options.includeMetadata)
        panel.add(includeMetadataCheckBox, gbc)

        gbc.gridy++
        includeGitInfoCheckBox = JCheckBox("Include Git info?", options.includeGitInfo)
        panel.add(includeGitInfoCheckBox, gbc)

        gbc.gridy++
        autoDetectLanguageCheckBox = JCheckBox("Auto-detect language?", options.autoDetectLanguage)
        panel.add(autoDetectLanguageCheckBox, gbc)

        gbc.gridy++
        panel.add(JLabel("Theme:"), gbc)
        gbc.gridx = 1
        themeComboBox = ComboBox(ThemeMode.values())
        themeComboBox.selectedItem = options.themeMode
        panel.add(themeComboBox, gbc)

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
        concurrencyModeComboBox = ComboBox(ConcurrencyMode.values())
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

    override fun isModified(): Boolean {
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
        options.includeLineNumbers = includeLineNumbersCheckBox.isSelected
        options.removeImports = removeImportsCheckBox.isSelected
        options.removeComments = removeCommentsCheckBox.isSelected
        options.trimWhitespace = trimWhitespaceCheckBox.isSelected
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

        // Resolve conflicting settings before saving.
        options.resolveConflicts()

        profileManager.deleteProfile(currentProfile.profileName)
        profileManager.addProfile(currentProfile.copy(options = options))
        profileManager.switchProfile(currentProfile.profileName)
    }

    override fun reset() {
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

    /**
     * Reformats the sample snippet using the current options and displays it in the preview area.
     */
    private fun updatePreview() {
        val snippet = Snippet(
            content = sampleSnippetText,
            fileName = "HelloWorld.java",
            relativePath = "src/com/example/HelloWorld.java"
        )
        val tempOptions = options.copy().also { it.resolveConflicts() }
        val formattedCode = CodeFormatter.formatSnippets(listOf(snippet), tempOptions)
            .joinToString("\n\n")
        previewArea.text = formattedCode
        previewArea.caretPosition = 0
    }

    override fun disposeUIResources() {
        // Dispose any resources if needed.
    }
}
