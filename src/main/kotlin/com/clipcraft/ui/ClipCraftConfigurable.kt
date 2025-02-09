package com.clipcraft.ui

import com.clipcraft.model.*
import com.clipcraft.services.ClipCraftSettings
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.util.ui.FormBuilder
import org.slf4j.LoggerFactory
import java.awt.BorderLayout
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * An IntelliJ settings UI panel implementing [Configurable] for ClipCraft.
 * This panel uses asynchronous preview updates (via SwingWorker) to avoid UI hangs.
 */
class ClipCraftConfigurable : Configurable {

    private val log = LoggerFactory.getLogger(ClipCraftConfigurable::class.java)

    private val mainPanel = JPanel(BorderLayout())

    private val settingsPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }
    private val previewArea = JTextArea(10, 50).apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        border = BorderFactory.createTitledBorder("Live Preview")
    }

    // Basic Settings
    private val filterRegexField = JTextField()
    private val shareGistCheckBox = JCheckBox("Share to Gist")

    // Advanced Settings
    private val ignoreFoldersField = JTextField()
    private val ignoreFilesField = JTextField()
    private val ignorePatternsField = JTextField()
    private val removeCommentsCheckBox = JCheckBox("Remove Comments")
    private val trimWhitespaceCheckBox = JCheckBox("Trim Whitespace")

    // Additional Settings
    private val exportToCloudServicesCheckBox = JCheckBox("Export to Cloud Services")
    private val measurePerformanceCheckBox = JCheckBox("Measure Performance")
    private val perProjectConfigCheckBox = JCheckBox("Per Project Config")
    private val localeField = JTextField()
    private val enableFeedbackButtonCheckBox = JCheckBox("Enable Feedback Button")
    private val enableNotificationCenterCheckBox = JCheckBox("Enable Notification Center")
    private val includeDirectorySummaryCheckBox = JCheckBox("Include Directory Summary")
    private val directorySummaryDepthSpinner = JSpinner(SpinnerNumberModel(999, 1, 10000, 1))
    private val collapseBlankLinesCheckBox = JCheckBox("Collapse Blank Lines")
    private val removeLeadingBlankLinesCheckBox = JCheckBox("Remove Leading Blank Lines")
    private val singleLineOutputCheckBox = JCheckBox("Single Line Output")
    private val enableChunkingForGPTCheckBox = JCheckBox("Enable Chunking for GPT")
    private val maxChunkSizeSpinner = JSpinner(SpinnerNumberModel(3000, 100, 10000, 100))
    private val themeModeComboBox = JComboBox(ThemeMode.values())
    private val useGitIgnoreCheckBox = JCheckBox("Use Git Ignore")
    private val compressionModeComboBox = JComboBox(CompressionMode.values())
    private val selectiveCompressionCheckBox = JCheckBox("Selective Compression")
    private val autoApplyOnChangeCheckBox = JCheckBox("Auto Apply on Change")

    private val quickSettingsPanel = ClipCraftQuickOptionsPanel(ClipCraftSettings.getInstance().getActiveOptions(), null)
    private val resetButton = JButton("Reset to Defaults").apply {
        addActionListener { resetDefaultsFields() }
    }

    init {
        val options = ClipCraftSettings.getInstance().getActiveOptions()

        // Initialize field values.
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

        // Build UI panels.
        val basicPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JLabel("Filter Regex:"), filterRegexField, 1, false)
            .addComponent(shareGistCheckBox)
            .panel.apply { border = BorderFactory.createTitledBorder("Basic Settings") }

        val advancedPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JLabel("Ignore Folders:"), ignoreFoldersField, 1, false)
            .addLabeledComponent(JLabel("Ignore Files:"), ignoreFilesField, 1, false)
            .addLabeledComponent(JLabel("Ignore Patterns:"), ignorePatternsField, 1, false)
            .addComponent(removeCommentsCheckBox)
            .addComponent(trimWhitespaceCheckBox)
            .panel.apply { border = BorderFactory.createTitledBorder("Advanced Settings") }

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
            .panel.apply { border = BorderFactory.createTitledBorder("Miscellaneous Settings") }

        settingsPanel.add(basicPanel)
        settingsPanel.add(Box.createVerticalStrut(10))
        settingsPanel.add(advancedPanel)
        settingsPanel.add(Box.createVerticalStrut(10))
        settingsPanel.add(miscPanel)
        settingsPanel.add(Box.createVerticalStrut(10))
        settingsPanel.add(quickSettingsPanel)
        settingsPanel.add(Box.createVerticalStrut(10))
        settingsPanel.add(resetButton)

        mainPanel.add(settingsPanel, BorderLayout.NORTH)
        mainPanel.add(JScrollPane(previewArea), BorderLayout.CENTER)

        // Attach listeners to update preview asynchronously.
        val textFieldsForPreview = listOf(filterRegexField, ignoreFoldersField, ignoreFilesField, ignorePatternsField)
        textFieldsForPreview.forEach { field ->
            field.document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent?) = updatePreviewAsync()
                override fun removeUpdate(e: DocumentEvent?) = updatePreviewAsync()
                override fun changedUpdate(e: DocumentEvent?) = updatePreviewAsync()
            })
        }
        shareGistCheckBox.addActionListener { updatePreviewAsync() }
        removeCommentsCheckBox.addActionListener { updatePreviewAsync() }
        trimWhitespaceCheckBox.addActionListener { updatePreviewAsync() }
        quickSettingsPanel.addChangeListener { updatePreviewAsync() }

        updatePreviewAsync()
    }

    override fun getDisplayName(): String = "Enhanced ClipCraft Settings"

    override fun createComponent(): JComponent = mainPanel

    /**
     * Asynchronously updates the preview using SwingWorker to avoid blocking the EDT.
     */
    private fun updatePreviewAsync() {
        object : SwingWorker<String, Void>() {
            override fun doInBackground(): String {
                val baseOpts = ClipCraftSettings.getInstance().getActiveOptions().copy(
                    filterRegex = filterRegexField.text,
                    shareToGistEnabled = shareGistCheckBox.isSelected,
                    ignoreFolders = parseCommaList(ignoreFoldersField.text),
                    ignoreFiles = parseCommaList(ignoreFilesField.text),
                    ignorePatterns = parseCommaList(ignorePatternsField.text),
                    removeComments = removeCommentsCheckBox.isSelected,
                    trimLineWhitespace = trimWhitespaceCheckBox.isSelected
                )
                val quickOpts = quickSettingsPanel.getOptions()
                val merged = baseOpts.copy(
                    ignoreFolders = quickOpts.ignoreFolders,
                    ignoreFiles = quickOpts.ignoreFiles,
                    ignorePatterns = quickOpts.ignorePatterns,
                    removeComments = quickOpts.removeComments,
                    trimLineWhitespace = quickOpts.trimLineWhitespace,
                    outputFormat = quickOpts.outputFormat,
                    removeImports = quickOpts.removeImports
                )
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
                return com.clipcraft.util.ClipCraftFormatter.processContent(sample, merged, "java")
            }

            override fun done() {
                try {
                    previewArea.text = get()
                } catch (e: Exception) {
                    log.error("Failed to update preview", e)
                }
            }
        }.execute()
    }

    private fun parseCommaList(input: String): List<String> =
        input.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    /**
     * Resets the UI fields to a new instance of default ClipCraftOptions.
     */
    private fun resetDefaultsFields() {
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
        updatePreviewAsync()
    }

    override fun isModified(): Boolean {
        val current = ClipCraftSettings.getInstance().getActiveOptions()
        return filterRegexField.text != current.filterRegex ||
                shareGistCheckBox.isSelected != current.shareToGistEnabled ||
                parseCommaList(ignoreFoldersField.text) != current.ignoreFolders ||
                parseCommaList(ignoreFilesField.text) != current.ignoreFiles ||
                parseCommaList(ignorePatternsField.text) != current.ignorePatterns ||
                removeCommentsCheckBox.isSelected != current.removeComments ||
                trimWhitespaceCheckBox.isSelected != current.trimLineWhitespace ||
                exportToCloudServicesCheckBox.isSelected != current.exportToCloudServices ||
                measurePerformanceCheckBox.isSelected != current.measurePerformance ||
                perProjectConfigCheckBox.isSelected != current.perProjectConfig ||
                localeField.text != current.locale ||
                enableFeedbackButtonCheckBox.isSelected != current.enableFeedbackButton ||
                enableNotificationCenterCheckBox.isSelected != current.enableNotificationCenter ||
                includeDirectorySummaryCheckBox.isSelected != current.includeDirectorySummary ||
                (directorySummaryDepthSpinner.value as Int) != current.directorySummaryDepth ||
                collapseBlankLinesCheckBox.isSelected != current.collapseBlankLines ||
                removeLeadingBlankLinesCheckBox.isSelected != current.removeLeadingBlankLines ||
                singleLineOutputCheckBox.isSelected != current.singleLineOutput ||
                enableChunkingForGPTCheckBox.isSelected != current.enableChunkingForGPT ||
                (maxChunkSizeSpinner.value as Int) != current.maxChunkSize ||
                themeModeComboBox.selectedItem != current.themeMode ||
                useGitIgnoreCheckBox.isSelected != current.useGitIgnore ||
                compressionModeComboBox.selectedItem != current.compressionMode ||
                selectiveCompressionCheckBox.isSelected != current.selectiveCompression ||
                autoApplyOnChangeCheckBox.isSelected != current.autoApplyOnChange ||
                quickSettingsPanel.isModified(current)
    }

    @Throws(ConfigurationException::class)
    override fun apply() {
        log.info("Applying new settings in ClipCraftConfigurable")
        val settings = ClipCraftSettings.getInstance()
        val current = settings.getActiveOptions()

        current.filterRegex = filterRegexField.text
        current.shareToGistEnabled = shareGistCheckBox.isSelected
        current.ignoreFolders = parseCommaList(ignoreFoldersField.text)
        current.ignoreFiles = parseCommaList(ignoreFilesField.text)
        current.ignorePatterns = parseCommaList(ignorePatternsField.text)
        current.removeComments = removeCommentsCheckBox.isSelected
        current.trimLineWhitespace = trimWhitespaceCheckBox.isSelected
        current.exportToCloudServices = exportToCloudServicesCheckBox.isSelected
        current.measurePerformance = measurePerformanceCheckBox.isSelected
        current.perProjectConfig = perProjectConfigCheckBox.isSelected
        current.locale = localeField.text
        current.enableFeedbackButton = enableFeedbackButtonCheckBox.isSelected
        current.enableNotificationCenter = enableNotificationCenterCheckBox.isSelected
        current.includeDirectorySummary = includeDirectorySummaryCheckBox.isSelected
        current.directorySummaryDepth = directorySummaryDepthSpinner.value as Int
        current.collapseBlankLines = collapseBlankLinesCheckBox.isSelected
        current.removeLeadingBlankLines = removeLeadingBlankLinesCheckBox.isSelected
        current.singleLineOutput = singleLineOutputCheckBox.isSelected
        current.enableChunkingForGPT = enableChunkingForGPTCheckBox.isSelected
        current.maxChunkSize = maxChunkSizeSpinner.value as Int
        current.themeMode = themeModeComboBox.selectedItem as ThemeMode
        current.useGitIgnore = useGitIgnoreCheckBox.isSelected
        current.compressionMode = compressionModeComboBox.selectedItem as CompressionMode
        current.selectiveCompression = selectiveCompressionCheckBox.isSelected
        current.autoApplyOnChange = autoApplyOnChangeCheckBox.isSelected

        // Merge quick settings.
        val quickOpts = quickSettingsPanel.getOptions()
        current.ignoreFolders = quickOpts.ignoreFolders
        current.ignoreFiles = quickOpts.ignoreFiles
        current.ignorePatterns = quickOpts.ignorePatterns
        current.removeComments = quickOpts.removeComments
        current.trimLineWhitespace = quickOpts.trimLineWhitespace
        current.outputFormat = quickOpts.outputFormat
        current.removeImports = quickOpts.removeImports

        settings.saveProfile(settings.state.activeProfileName, current)
    }

    override fun reset() {
        val opts = ClipCraftSettings.getInstance().getActiveOptions()
        filterRegexField.text = opts.filterRegex
        shareGistCheckBox.isSelected = opts.shareToGistEnabled
        ignoreFoldersField.text = opts.ignoreFolders.joinToString(", ")
        ignoreFilesField.text = opts.ignoreFiles.joinToString(", ")
        ignorePatternsField.text = opts.ignorePatterns.joinToString(", ")
        removeCommentsCheckBox.isSelected = opts.removeComments
        trimWhitespaceCheckBox.isSelected = opts.trimLineWhitespace
        exportToCloudServicesCheckBox.isSelected = opts.exportToCloudServices
        measurePerformanceCheckBox.isSelected = opts.measurePerformance
        perProjectConfigCheckBox.isSelected = opts.perProjectConfig
        localeField.text = opts.locale
        enableFeedbackButtonCheckBox.isSelected = opts.enableFeedbackButton
        enableNotificationCenterCheckBox.isSelected = opts.enableNotificationCenter
        includeDirectorySummaryCheckBox.isSelected = opts.includeDirectorySummary
        directorySummaryDepthSpinner.value = opts.directorySummaryDepth
        collapseBlankLinesCheckBox.isSelected = opts.collapseBlankLines
        removeLeadingBlankLinesCheckBox.isSelected = opts.removeLeadingBlankLines
        singleLineOutputCheckBox.isSelected = opts.singleLineOutput
        enableChunkingForGPTCheckBox.isSelected = opts.enableChunkingForGPT
        maxChunkSizeSpinner.value = opts.maxChunkSize
        themeModeComboBox.selectedItem = opts.themeMode
        useGitIgnoreCheckBox.isSelected = opts.useGitIgnore
        compressionModeComboBox.selectedItem = opts.compressionMode
        selectiveCompressionCheckBox.isSelected = opts.selectiveCompression
        autoApplyOnChangeCheckBox.isSelected = opts.autoApplyOnChange

        quickSettingsPanel.resetFields(opts)
        updatePreviewAsync()
    }

    override fun disposeUIResources() {
        // Nothing special to dispose.
    }
}
