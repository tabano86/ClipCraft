package com.clipcraft.ui

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.CompressionMode
import com.clipcraft.model.ThemeMode
import com.clipcraft.services.ClipCraftSettings
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class ClipCraftConfigurable : Configurable {

    private val mainPanel: JPanel = JPanel(BorderLayout())

    // Panels for settings and preview.
    private val settingsPanel: JPanel = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }
    private val previewPanel: JPanel = JPanel(BorderLayout())

    // ----- Basic Settings UI -----
    private val filterRegexField: JTextField = JTextField()
    private val shareGistCheckBox: JCheckBox = JCheckBox("Share to Gist")

    // ----- Advanced Settings UI -----
    private val ignoreFoldersField: JTextField = JTextField()
    private val ignoreFilesField: JTextField = JTextField()
    private val ignorePatternsField: JTextField = JTextField()
    private val removeCommentsCheckBox: JCheckBox = JCheckBox("Remove Comments")
    private val trimWhitespaceCheckBox: JCheckBox = JCheckBox("Trim Whitespace")

    // ----- Additional Settings UI -----
    private val exportToCloudServicesCheckBox: JCheckBox = JCheckBox("Export to Cloud Services")
    private val measurePerformanceCheckBox: JCheckBox = JCheckBox("Measure Performance")
    private val perProjectConfigCheckBox: JCheckBox = JCheckBox("Per Project Config")
    private val localeField: JTextField = JTextField()
    private val enableFeedbackButtonCheckBox: JCheckBox = JCheckBox("Enable Feedback Button")
    private val enableNotificationCenterCheckBox: JCheckBox = JCheckBox("Enable Notification Center")
    private val includeDirectorySummaryCheckBox: JCheckBox = JCheckBox("Include Directory Summary")
    private val directorySummaryDepthSpinner: JSpinner = JSpinner(SpinnerNumberModel(999, 1, 10000, 1))
    private val collapseBlankLinesCheckBox: JCheckBox = JCheckBox("Collapse Blank Lines")
    private val removeLeadingBlankLinesCheckBox: JCheckBox = JCheckBox("Remove Leading Blank Lines")
    private val singleLineOutputCheckBox: JCheckBox = JCheckBox("Single Line Output")
    private val enableChunkingForGPTCheckBox: JCheckBox = JCheckBox("Enable Chunking for GPT")
    private val maxChunkSizeSpinner: JSpinner = JSpinner(SpinnerNumberModel(3000, 100, 10000, 100))
    private val themeModeComboBox: JComboBox<ThemeMode> = JComboBox(ThemeMode.values())
    private val useGitIgnoreCheckBox: JCheckBox = JCheckBox("Use Git Ignore")
    private val compressionModeComboBox: JComboBox<CompressionMode> = JComboBox(CompressionMode.values())
    private val selectiveCompressionCheckBox: JCheckBox = JCheckBox("Selective Compression")
    private val autoApplyOnChangeCheckBox: JCheckBox = JCheckBox("Auto Apply on Change")

    // ----- Unified Quick Settings Panel -----
    private val quickSettingsPanel: ClipCraftQuickOptionsPanel

    // ----- Live Preview Area -----
    private val previewArea: JTextArea = JTextArea(10, 50).apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        border = BorderFactory.createTitledBorder("Live Preview")
    }

    // ----- Reset Defaults Button -----
    private val resetButton: JButton = JButton("Reset to Defaults").apply {
        addActionListener { resetDefaults() }
    }

    init {
        // Load current settings from persistent state.
        val options: ClipCraftOptions = ClipCraftSettings.getInstance().getActiveOptions()

        // Initialize basic fields
        filterRegexField.text = options.filterRegex
        shareGistCheckBox.isSelected = options.shareToGistEnabled
        shareGistCheckBox.toolTipText = "If enabled, processed code is shared to your Gist account."

        // Initialize advanced fields
        ignoreFoldersField.text = options.ignoreFolders.joinToString(", ")
        ignoreFilesField.text = options.ignoreFiles.joinToString(", ")
        ignorePatternsField.text = options.ignorePatterns.joinToString(", ")
        removeCommentsCheckBox.isSelected = options.removeComments
        trimWhitespaceCheckBox.isSelected = options.trimLineWhitespace

        // Initialize additional fields
        exportToCloudServicesCheckBox.isSelected = options.exportToCloudServices
        measurePerformanceCheckBox.isSelected = options.measurePerformance
        perProjectConfigCheckBox.isSelected = options.perProjectConfig
        localeField.text = options.locale
        enableFeedbackButtonCheckBox.isSelected = options.enableFeedbackButton
        enableNotificationCenterCheckBox.isSelected = options.enableNotificationCenter
        includeDirectorySummaryCheckBox.isSelected = options.includeDirectorySummary
        directorySummaryDepthSpinner.value = options.directorySummaryDepth
        collapseBlankLinesCheckBox.isSelected = options.collapseBlankLines
        removeLeadingBlankLinesCheckBox.isSelected = options.removeLeadingBlankLines
        singleLineOutputCheckBox.isSelected = options.singleLineOutput
        enableChunkingForGPTCheckBox.isSelected = options.enableChunkingForGPT
        maxChunkSizeSpinner.value = options.maxChunkSize
        themeModeComboBox.selectedItem = options.themeMode
        useGitIgnoreCheckBox.isSelected = options.useGitIgnore
        compressionModeComboBox.selectedItem = options.compressionMode
        selectiveCompressionCheckBox.isSelected = options.selectiveCompression
        autoApplyOnChangeCheckBox.isSelected = options.autoApplyOnChange

        // Build Basic Settings panel
        val basicPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(
                JLabel("Filter Regex:").apply {
                    toolTipText = "Enter a regex to filter files (e.g. \".*\\.java$\")."
                },
                filterRegexField, 1, false
            )
            .addComponent(shareGistCheckBox)
            .panel.apply {
                border = BorderFactory.createTitledBorder("Basic Settings")
            }

        // Build Advanced Settings panel
        val advancedPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JLabel("Ignore Folders:"), ignoreFoldersField, 1, false)
            .addLabeledComponent(JLabel("Ignore Files:"), ignoreFilesField, 1, false)
            .addLabeledComponent(JLabel("Ignore Patterns:"), ignorePatternsField, 1, false)
            .addComponent(removeCommentsCheckBox)
            .addComponent(trimWhitespaceCheckBox)
            .panel.apply {
                border = BorderFactory.createTitledBorder("Advanced Settings")
            }

        // Build Misc Settings panel
        val miscPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JLabel("Export to Cloud Services:"), exportToCloudServicesCheckBox, 1, false)
            .addLabeledComponent(JLabel("Measure Performance:"), measurePerformanceCheckBox, 1, false)
            .addLabeledComponent(JLabel("Per Project Config:"), perProjectConfigCheckBox, 1, false)
            .addLabeledComponent(JLabel("Locale:"), localeField, 1, false)
            .addLabeledComponent(JLabel("Enable Feedback Button:"), enableFeedbackButtonCheckBox, 1, false)
            .addLabeledComponent(JLabel("Enable Notification Center:"), enableNotificationCenterCheckBox, 1, false)
            .addLabeledComponent(JLabel("Include Directory Summary:"), includeDirectorySummaryCheckBox, 1, false)
            .addLabeledComponent(JLabel("Directory Summary Depth:"), directorySummaryDepthSpinner, 1, false)
            .addLabeledComponent(JLabel("Collapse Blank Lines:"), collapseBlankLinesCheckBox, 1, false)
            .addLabeledComponent(JLabel("Remove Leading Blank Lines:"), removeLeadingBlankLinesCheckBox, 1, false)
            .addLabeledComponent(JLabel("Single Line Output:"), singleLineOutputCheckBox, 1, false)
            .addLabeledComponent(JLabel("Enable Chunking for GPT:"), enableChunkingForGPTCheckBox, 1, false)
            .addLabeledComponent(JLabel("Max Chunk Size:"), maxChunkSizeSpinner, 1, false)
            .addLabeledComponent(JLabel("Theme Mode:"), themeModeComboBox, 1, false)
            .addLabeledComponent(JLabel("Use Git Ignore:"), useGitIgnoreCheckBox, 1, false)
            .addLabeledComponent(JLabel("Compression Mode:"), compressionModeComboBox, 1, false)
            .addComponent(selectiveCompressionCheckBox)
            .addLabeledComponent(JLabel("Auto Apply on Change:"), autoApplyOnChangeCheckBox, 1, false)
            .panel.apply {
                border = BorderFactory.createTitledBorder("Miscellaneous Settings")
            }

        // Instantiate the unified quick settings panel
        quickSettingsPanel = ClipCraftQuickOptionsPanel(options, null)

        // Compose the settings container
        settingsPanel.add(basicPanel)
        settingsPanel.add(Box.createVerticalStrut(10))
        settingsPanel.add(advancedPanel)
        settingsPanel.add(Box.createVerticalStrut(10))
        settingsPanel.add(miscPanel)
        settingsPanel.add(Box.createVerticalStrut(10))
        settingsPanel.add(quickSettingsPanel)
        settingsPanel.add(Box.createVerticalStrut(10))
        settingsPanel.add(resetButton)

        // Compose the main panel
        mainPanel.add(settingsPanel, BorderLayout.NORTH)
        mainPanel.add(JScrollPane(previewArea), BorderLayout.CENTER)

        // Attach update listeners
        val textFieldsForPreview = listOf(filterRegexField, ignoreFoldersField, ignoreFilesField, ignorePatternsField)
        textFieldsForPreview.forEach { field ->
            field.document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent?) = updatePreview()
                override fun removeUpdate(e: DocumentEvent?) = updatePreview()
                override fun changedUpdate(e: DocumentEvent?) = updatePreview()
            })
        }
        shareGistCheckBox.addActionListener { updatePreview() }
        removeCommentsCheckBox.addActionListener { updatePreview() }
        trimWhitespaceCheckBox.addActionListener { updatePreview() }

        // Also update preview when quick settings change
        quickSettingsPanel.addChangeListener { updatePreview() }

        // Initialize preview content
        updatePreview()
    }

    private fun updatePreview() {
        val tempOptions = ClipCraftSettings.getInstance().getActiveOptions().copy(
            filterRegex = filterRegexField.text,
            shareToGistEnabled = shareGistCheckBox.isSelected,
            ignoreFolders = ignoreFoldersField.text.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            ignoreFiles = ignoreFilesField.text.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            ignorePatterns = ignorePatternsField.text.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            removeComments = removeCommentsCheckBox.isSelected,
            trimLineWhitespace = trimWhitespaceCheckBox.isSelected
        )
        // Merge quick settings
        val quickOpts = quickSettingsPanel.getOptions()
        tempOptions.ignoreFolders = quickOpts.ignoreFolders
        tempOptions.ignoreFiles = quickOpts.ignoreFiles
        tempOptions.ignorePatterns = quickOpts.ignorePatterns
        tempOptions.removeComments = quickOpts.removeComments
        tempOptions.trimLineWhitespace = quickOpts.trimLineWhitespace
        tempOptions.outputFormat = quickOpts.outputFormat
        tempOptions.removeImports = quickOpts.removeImports

        // Generate a sample preview
        val sample = """
            // This is a sample comment
            public class HelloWorld {
                import java.util.List; // Another comment
                public static void main(String[] args) {
                    System.out.println("Hello, world!");
                }
            }
            
            // Another comment line
        """.trimIndent()

        // For demonstration: apply minimal transformations
        val processed = com.clipcraft.util.ClipCraftFormatter.processContent(sample, tempOptions, "java")
        previewArea.text = processed
    }

    private fun resetDefaults() {
        val defaults = ClipCraftOptions()
        filterRegexField.text = defaults.filterRegex
        shareGistCheckBox.isSelected = defaults.shareToGistEnabled

        ignoreFoldersField.text = defaults.ignoreFolders.joinToString(", ")
        ignoreFilesField.text = defaults.ignoreFiles.joinToString(", ")
        ignorePatternsField.text = defaults.ignorePatterns.joinToString(", ")
        removeCommentsCheckBox.isSelected = defaults.removeComments
        trimWhitespaceCheckBox.isSelected = defaults.trimLineWhitespace

        exportToCloudServicesCheckBox.isSelected = defaults.exportToCloudServices
        measurePerformanceCheckBox.isSelected = defaults.measurePerformance
        perProjectConfigCheckBox.isSelected = defaults.perProjectConfig
        localeField.text = defaults.locale
        enableFeedbackButtonCheckBox.isSelected = defaults.enableFeedbackButton
        enableNotificationCenterCheckBox.isSelected = defaults.enableNotificationCenter
        includeDirectorySummaryCheckBox.isSelected = defaults.includeDirectorySummary
        directorySummaryDepthSpinner.value = defaults.directorySummaryDepth
        collapseBlankLinesCheckBox.isSelected = defaults.collapseBlankLines
        removeLeadingBlankLinesCheckBox.isSelected = defaults.removeLeadingBlankLines
        singleLineOutputCheckBox.isSelected = defaults.singleLineOutput
        enableChunkingForGPTCheckBox.isSelected = defaults.enableChunkingForGPT
        maxChunkSizeSpinner.value = defaults.maxChunkSize
        themeModeComboBox.selectedItem = defaults.themeMode
        useGitIgnoreCheckBox.isSelected = defaults.useGitIgnore
        compressionModeComboBox.selectedItem = defaults.compressionMode
        selectiveCompressionCheckBox.isSelected = defaults.selectiveCompression
        autoApplyOnChangeCheckBox.isSelected = defaults.autoApplyOnChange

        quickSettingsPanel.resetFields(defaults)
        updatePreview()
    }

    override fun getDisplayName(): String = "Enhanced ClipCraft Settings"

    override fun createComponent(): JComponent = mainPanel

    override fun isModified(): Boolean {
        val currentOptions = ClipCraftSettings.getInstance().getActiveOptions()
        // Compare all fields
        if (filterRegexField.text != currentOptions.filterRegex) return true
        if (shareGistCheckBox.isSelected != currentOptions.shareToGistEnabled) return true

        val ignoreFoldersList = ignoreFoldersField.text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (ignoreFoldersList != currentOptions.ignoreFolders) return true

        val ignoreFilesList = ignoreFilesField.text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (ignoreFilesList != currentOptions.ignoreFiles) return true

        val ignorePatternsList = ignorePatternsField.text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (ignorePatternsList != currentOptions.ignorePatterns) return true

        if (removeCommentsCheckBox.isSelected != currentOptions.removeComments) return true
        if (trimWhitespaceCheckBox.isSelected != currentOptions.trimLineWhitespace) return true

        if (exportToCloudServicesCheckBox.isSelected != currentOptions.exportToCloudServices) return true
        if (measurePerformanceCheckBox.isSelected != currentOptions.measurePerformance) return true
        if (perProjectConfigCheckBox.isSelected != currentOptions.perProjectConfig) return true
        if (localeField.text != currentOptions.locale) return true
        if (enableFeedbackButtonCheckBox.isSelected != currentOptions.enableFeedbackButton) return true
        if (enableNotificationCenterCheckBox.isSelected != currentOptions.enableNotificationCenter) return true
        if (includeDirectorySummaryCheckBox.isSelected != currentOptions.includeDirectorySummary) return true
        if ((directorySummaryDepthSpinner.value as Int) != currentOptions.directorySummaryDepth) return true
        if (collapseBlankLinesCheckBox.isSelected != currentOptions.collapseBlankLines) return true
        if (removeLeadingBlankLinesCheckBox.isSelected != currentOptions.removeLeadingBlankLines) return true
        if (singleLineOutputCheckBox.isSelected != currentOptions.singleLineOutput) return true
        if (enableChunkingForGPTCheckBox.isSelected != currentOptions.enableChunkingForGPT) return true
        if ((maxChunkSizeSpinner.value as Int) != currentOptions.maxChunkSize) return true
        if (themeModeComboBox.selectedItem != currentOptions.themeMode) return true
        if (useGitIgnoreCheckBox.isSelected != currentOptions.useGitIgnore) return true
        if (compressionModeComboBox.selectedItem != currentOptions.compressionMode) return true
        if (selectiveCompressionCheckBox.isSelected != currentOptions.selectiveCompression) return true
        if (autoApplyOnChangeCheckBox.isSelected != currentOptions.autoApplyOnChange) return true

        // Also check quick settings panel
        val qsModified = quickSettingsPanel.isModified(currentOptions)
        if (qsModified) return true

        return false
    }

    @Throws(ConfigurationException::class)
    override fun apply() {
        val settings = ClipCraftSettings.getInstance()
        val currentOptions = settings.getActiveOptions()

        // Basic
        currentOptions.filterRegex = filterRegexField.text
        currentOptions.shareToGistEnabled = shareGistCheckBox.isSelected

        // Advanced
        currentOptions.ignoreFolders = ignoreFoldersField.text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        currentOptions.ignoreFiles = ignoreFilesField.text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        currentOptions.ignorePatterns = ignorePatternsField.text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        currentOptions.removeComments = removeCommentsCheckBox.isSelected
        currentOptions.trimLineWhitespace = trimWhitespaceCheckBox.isSelected

        // Misc
        currentOptions.exportToCloudServices = exportToCloudServicesCheckBox.isSelected
        currentOptions.measurePerformance = measurePerformanceCheckBox.isSelected
        currentOptions.perProjectConfig = perProjectConfigCheckBox.isSelected
        currentOptions.locale = localeField.text
        currentOptions.enableFeedbackButton = enableFeedbackButtonCheckBox.isSelected
        currentOptions.enableNotificationCenter = enableNotificationCenterCheckBox.isSelected
        currentOptions.includeDirectorySummary = includeDirectorySummaryCheckBox.isSelected
        currentOptions.directorySummaryDepth = (directorySummaryDepthSpinner.value as? Int) ?: 999
        currentOptions.collapseBlankLines = collapseBlankLinesCheckBox.isSelected
        currentOptions.removeLeadingBlankLines = removeLeadingBlankLinesCheckBox.isSelected
        currentOptions.singleLineOutput = singleLineOutputCheckBox.isSelected
        currentOptions.enableChunkingForGPT = enableChunkingForGPTCheckBox.isSelected
        currentOptions.maxChunkSize = (maxChunkSizeSpinner.value as? Int) ?: 3000
        currentOptions.themeMode = themeModeComboBox.selectedItem as ThemeMode
        currentOptions.useGitIgnore = useGitIgnoreCheckBox.isSelected
        currentOptions.compressionMode = compressionModeComboBox.selectedItem as CompressionMode
        currentOptions.selectiveCompression = selectiveCompressionCheckBox.isSelected
        currentOptions.autoApplyOnChange = autoApplyOnChangeCheckBox.isSelected

        // Merge quick settings
        val quickOpts = quickSettingsPanel.getOptions()
        currentOptions.ignoreFolders = quickOpts.ignoreFolders
        currentOptions.ignoreFiles = quickOpts.ignoreFiles
        currentOptions.ignorePatterns = quickOpts.ignorePatterns
        currentOptions.removeComments = quickOpts.removeComments
        currentOptions.trimLineWhitespace = quickOpts.trimLineWhitespace
        currentOptions.outputFormat = quickOpts.outputFormat
        currentOptions.removeImports = quickOpts.removeImports

        settings.saveProfile(settings.state.activeProfileName, currentOptions)
    }

    override fun reset() {
        val options = ClipCraftSettings.getInstance().getActiveOptions()
        filterRegexField.text = options.filterRegex
        shareGistCheckBox.isSelected = options.shareToGistEnabled

        ignoreFoldersField.text = options.ignoreFolders.joinToString(", ")
        ignoreFilesField.text = options.ignoreFiles.joinToString(", ")
        ignorePatternsField.text = options.ignorePatterns.joinToString(", ")
        removeCommentsCheckBox.isSelected = options.removeComments
        trimWhitespaceCheckBox.isSelected = options.trimLineWhitespace

        exportToCloudServicesCheckBox.isSelected = options.exportToCloudServices
        measurePerformanceCheckBox.isSelected = options.measurePerformance
        perProjectConfigCheckBox.isSelected = options.perProjectConfig
        localeField.text = options.locale
        enableFeedbackButtonCheckBox.isSelected = options.enableFeedbackButton
        enableNotificationCenterCheckBox.isSelected = options.enableNotificationCenter
        includeDirectorySummaryCheckBox.isSelected = options.includeDirectorySummary
        directorySummaryDepthSpinner.value = options.directorySummaryDepth
        collapseBlankLinesCheckBox.isSelected = options.collapseBlankLines
        removeLeadingBlankLinesCheckBox.isSelected = options.removeLeadingBlankLines
        singleLineOutputCheckBox.isSelected = options.singleLineOutput
        enableChunkingForGPTCheckBox.isSelected = options.enableChunkingForGPT
        maxChunkSizeSpinner.value = options.maxChunkSize
        themeModeComboBox.selectedItem = options.themeMode
        useGitIgnoreCheckBox.isSelected = options.useGitIgnore
        compressionModeComboBox.selectedItem = options.compressionMode
        selectiveCompressionCheckBox.isSelected = options.selectiveCompression
        autoApplyOnChangeCheckBox.isSelected = options.autoApplyOnChange

        quickSettingsPanel.resetFields(options)
        updatePreview()
    }

    override fun disposeUIResources() {
        // No specific disposal needed
    }
}
