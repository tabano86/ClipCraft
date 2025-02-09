package com.clipcraft.ui

import com.clipcraft.model.*
import com.clipcraft.services.ClipCraftSettings
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

class ClipCraftConfigurable : Configurable {

    private val mainPanel = JPanel(BorderLayout())
    private val profileComboBox = JComboBox<String>()
    private val newProfileButton = JButton("New Profile")
    private val deleteProfileButton = JButton("Delete Profile")
    private val saveProfileButton = JButton("Save Profile As...")

    // Quick toggles
    private val removeCommentsCheck = JCheckBox("Comments", false)
    private val chunkingCheck = JCheckBox("Chunking", false)
    private val autoApplyCheck = JCheckBox("Auto-Apply", false)

    // Basic references
    private val filterRegexField = JTextField(30)
    private val shareGistCheckBox = JCheckBox("Share to Gist")
    private val macrosArea = JTextArea(5, 30)
    private val multiExportComboBox = JComboBox(arrayOf("None", "Markdown, HTML", "Markdown, HTML, Plain"))

    // Next-gen
    private val includeDirSummaryCheckBox = JCheckBox("Include Directory Summary")
    private val collapseBlankLinesCheckBox = JCheckBox("Collapse Blank Lines")
    private val removeLeadingBlanksCheckBox = JCheckBox("Remove Leading Blank Lines")
    private val singleLineOutputCheckBox = JCheckBox("Single Line Output")
    private val enableChunkingCheckBox = JCheckBox("Enable GPT Chunking")
    private val chunkSizeField = JTextField(5)

    private val useGitIgnoreCheckBox = JCheckBox("Use .gitignore", false)
    private val compressionModeCombo = JComboBox(CompressionMode.values())
    private val selectiveCompressionCheckBox = JCheckBox("Selective Compression")

    private var currentProfileName: String = ""
    private val recentProfiles = mutableListOf<String>()

    init {
        profileComboBox.preferredSize = Dimension(200, 30)
        loadProfileNames()
        profileComboBox.addActionListener {
            val selected = profileComboBox.selectedItem as String?
            if (selected != null) {
                val settings = ClipCraftSettings.getInstance()
                settings.setActiveProfile(selected)
                currentProfileName = selected
                if (!recentProfiles.contains(selected)) {
                    recentProfiles.add(0, selected)
                }
                if (getActiveOptions().autoApplyOnChange) {
                    apply()
                }
                reset()
            }
        }

        newProfileButton.addActionListener {
            val name = JOptionPane.showInputDialog("New Profile Name:")
            if (!name.isNullOrBlank()) {
                val settings = ClipCraftSettings.getInstance()
                settings.saveProfile(name, ClipCraftOptions())
                settings.setActiveProfile(name)
                loadProfileNames()
                profileComboBox.selectedItem = name
                currentProfileName = name
                reset()
            }
        }

        deleteProfileButton.addActionListener {
            val selected = profileComboBox.selectedItem as String? ?: return@addActionListener
            val settings = ClipCraftSettings.getInstance()
            settings.deleteProfile(selected)
            loadProfileNames()
            if (profileComboBox.itemCount > 0) {
                profileComboBox.selectedIndex = 0
                currentProfileName = profileComboBox.selectedItem as String
            }
            reset()
        }

        saveProfileButton.addActionListener {
            val name = JOptionPane.showInputDialog("Save current settings as Profile:")
            if (!name.isNullOrBlank()) {
                val newOpts = gatherFields()
                val settings = ClipCraftSettings.getInstance()
                settings.saveProfile(name, newOpts)
                settings.setActiveProfile(name)
                loadProfileNames()
                profileComboBox.selectedItem = name
                currentProfileName = name
            }
        }

        val profilePanel = JPanel().apply {
            add(JLabel("Profile: "))
            add(profileComboBox)
            add(newProfileButton)
            add(deleteProfileButton)
            add(saveProfileButton)
        }

        val quickTogglesPanel = JPanel().apply {
            add(JLabel("Quick Toggles:"))
            add(removeCommentsCheck)
            add(chunkingCheck)
            add(autoApplyCheck)
        }

        val basicForm = FormBuilder.createFormBuilder()
            .addLabeledComponent(JLabel("Filter Regex:"), filterRegexField, 1, false)
            .addComponent(shareGistCheckBox)
            .panel.apply {
                border = BorderFactory.createTitledBorder("Basic Settings")
            }

        val nextGenForm = FormBuilder.createFormBuilder()
            .addComponent(JLabel("Next-Gen Options:"))
            .addComponent(includeDirSummaryCheckBox)
            .addComponent(collapseBlankLinesCheckBox)
            .addComponent(removeLeadingBlanksCheckBox)
            .addComponent(singleLineOutputCheckBox)
            .addComponent(enableChunkingCheckBox)
            .addLabeledComponent(JLabel("Max Chunk Size:"), chunkSizeField)
            .panel.apply {
                border = BorderFactory.createTitledBorder("Next-Gen")
            }

        val advancedIntegrationForm = FormBuilder.createFormBuilder()
            .addComponent(JLabel("Integration & Compression:"))
            .addComponent(useGitIgnoreCheckBox)
            .addLabeledComponent(JLabel("Compression Mode:"), compressionModeCombo)
            .addComponent(selectiveCompressionCheckBox)
            .panel.apply {
                border = BorderFactory.createTitledBorder("Integration")
            }

        val advancedForm = FormBuilder.createFormBuilder()
            .addComponent(JLabel("Macros (KEY=VALUE):"))
            .addComponent(JScrollPane(macrosArea))
            .addLabeledComponent(JLabel("Additional Export Formats:"), multiExportComboBox, 1, false)
            .panel.apply {
                border = BorderFactory.createTitledBorder("Advanced Settings")
            }

        val centerForm = FormBuilder.createFormBuilder()
            .addComponent(quickTogglesPanel)
            .addVerticalGap(10)
            .addComponent(basicForm)
            .addVerticalGap(10)
            .addComponent(nextGenForm)
            .addVerticalGap(10)
            .addComponent(advancedIntegrationForm)
            .addVerticalGap(10)
            .addComponent(advancedForm)
            .panel

        mainPanel.add(profilePanel, BorderLayout.NORTH)
        mainPanel.add(centerForm, BorderLayout.CENTER)
    }

    override fun getDisplayName(): String = "ClipCraft"

    override fun createComponent(): JComponent {
        reset()
        return mainPanel
    }

    private fun loadProfileNames() {
        val settings = ClipCraftSettings.getInstance()
        val names = settings.listProfileNames()
        profileComboBox.model = DefaultComboBoxModel(names.toTypedArray())
        if (names.isNotEmpty()) {
            val activeName = settings.state.activeProfileName
            profileComboBox.selectedItem = activeName
            currentProfileName = activeName
        }
    }

    private fun getActiveOptions(): ClipCraftOptions {
        return ClipCraftSettings.getInstance().getActiveOptions()
    }

    private fun gatherFields(): ClipCraftOptions {
        val activeOpts = getActiveOptions()

        // Quick toggles
        activeOpts.removeComments = removeCommentsCheck.isSelected
        activeOpts.enableChunkingForGPT = chunkingCheck.isSelected
        activeOpts.autoApplyOnChange = autoApplyCheck.isSelected

        // Basic
        activeOpts.filterRegex = filterRegexField.text
        activeOpts.shareToGistEnabled = shareGistCheckBox.isSelected

        // Macros
        val macroMap = macrosArea.text.lines()
            .filter { it.contains("=") }
            .associate {
                val (key, value) = it.split("=", limit = 2)
                key.trim() to value.trim()
            }
        activeOpts.macros = macroMap

        // Multi export
        activeOpts.simultaneousExports = when (multiExportComboBox.selectedIndex) {
            1 -> setOf(OutputFormat.MARKDOWN, OutputFormat.HTML)
            2 -> setOf(OutputFormat.MARKDOWN, OutputFormat.HTML, OutputFormat.PLAIN)
            else -> emptySet()
        }

        // Next-gen
        activeOpts.includeDirectorySummary = includeDirSummaryCheckBox.isSelected
        activeOpts.collapseBlankLines = collapseBlankLinesCheckBox.isSelected
        activeOpts.removeLeadingBlankLines = removeLeadingBlanksCheckBox.isSelected
        activeOpts.singleLineOutput = singleLineOutputCheckBox.isSelected
        // Combine chunk toggles
        activeOpts.enableChunkingForGPT = enableChunkingCheckBox.isSelected || chunkingCheck.isSelected
        val sizeVal = chunkSizeField.text.toIntOrNull() ?: 3000
        activeOpts.maxChunkSize = if (sizeVal <= 0) 3000 else sizeVal

        // Integration
        activeOpts.useGitIgnore = useGitIgnoreCheckBox.isSelected
        activeOpts.compressionMode = compressionModeCombo.selectedItem as CompressionMode
        activeOpts.selectiveCompression = selectiveCompressionCheckBox.isSelected

        return activeOpts
    }

    override fun isModified(): Boolean = true

    @Throws(ConfigurationException::class)
    override fun apply() {
        val newOpts = gatherFields()
        val settings = ClipCraftSettings.getInstance()
        settings.saveProfile(currentProfileName, newOpts)
    }

    override fun reset() {
        val activeOpts = getActiveOptions()

        // Quick toggles
        removeCommentsCheck.isSelected = activeOpts.removeComments
        chunkingCheck.isSelected = activeOpts.enableChunkingForGPT
        autoApplyCheck.isSelected = activeOpts.autoApplyOnChange

        // Basic
        filterRegexField.text = activeOpts.filterRegex
        shareGistCheckBox.isSelected = activeOpts.shareToGistEnabled

        // Macros
        macrosArea.text = activeOpts.macros.entries.joinToString("\n") { "${it.key}=${it.value}" }
        multiExportComboBox.selectedIndex = when (activeOpts.simultaneousExports) {
            setOf(OutputFormat.MARKDOWN, OutputFormat.HTML) -> 1
            setOf(OutputFormat.MARKDOWN, OutputFormat.HTML, OutputFormat.PLAIN) -> 2
            else -> 0
        }

        // Next-gen
        includeDirSummaryCheckBox.isSelected = activeOpts.includeDirectorySummary
        collapseBlankLinesCheckBox.isSelected = activeOpts.collapseBlankLines
        removeLeadingBlanksCheckBox.isSelected = activeOpts.removeLeadingBlankLines
        singleLineOutputCheckBox.isSelected = activeOpts.singleLineOutput
        enableChunkingCheckBox.isSelected = activeOpts.enableChunkingForGPT
        chunkSizeField.text = activeOpts.maxChunkSize.toString()

        // Integration
        useGitIgnoreCheckBox.isSelected = activeOpts.useGitIgnore
        compressionModeCombo.selectedItem = activeOpts.compressionMode
        selectiveCompressionCheckBox.isSelected = activeOpts.selectiveCompression
    }

    override fun disposeUIResources() {}
}
