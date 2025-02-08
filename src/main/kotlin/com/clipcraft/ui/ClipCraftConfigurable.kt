package com.clipcraft.ui

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.OutputFormat
import com.clipcraft.services.ClipCraftSettings
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

/**
 * The main ApplicationConfigurable UI for ClipCraft,
 * which now includes an interface for multiple named profiles.
 */
class ClipCraftConfigurable : Configurable {

    private val mainPanel = JPanel(BorderLayout())
    private val profileComboBox = JComboBox<String>()
    private val newProfileButton = JButton("New Profile")
    private val deleteProfileButton = JButton("Delete Profile")
    private val saveProfileButton = JButton("Save Profile As...")

    // Quick references for options:
    private val filterRegexField = JTextField(30)
    private val shareGistCheckBox = JCheckBox("Share to Gist")
    private val macrosArea = JTextArea(5, 30)
    private val multiExportComboBox = JComboBox(arrayOf("None", "Markdown, HTML", "Markdown, HTML, Plain"))

    // Next-gen fields:
    private val includeDirSummaryCheckBox = JCheckBox("Include Directory Summary")
    private val collapseBlankLinesCheckBox = JCheckBox("Collapse Blank Lines")
    private val removeLeadingBlanksCheckBox = JCheckBox("Remove Leading Blank Lines")
    private val singleLineOutputCheckBox = JCheckBox("Single Line Output")
    private val enableChunkingCheckBox = JCheckBox("Enable GPT Chunking")
    private val chunkSizeField = JTextField(5)

    private var currentProfileName: String = ""

    init {
        // Set up the profile UI
        profileComboBox.preferredSize = Dimension(200, 30)
        loadProfileNames()
        profileComboBox.addActionListener {
            val selected = profileComboBox.selectedItem as String?
            if (selected != null) {
                ClipCraftSettings.getInstance().setActiveProfile(selected)
                currentProfileName = selected
                reset() // reload fields
            }
        }

        newProfileButton.addActionListener {
            val name = JOptionPane.showInputDialog("New Profile Name:")
            if (!name.isNullOrBlank()) {
                ClipCraftSettings.getInstance().saveProfile(name, ClipCraftOptions())
                ClipCraftSettings.getInstance().setActiveProfile(name)
                loadProfileNames()
                profileComboBox.selectedItem = name
                currentProfileName = name
                reset()
            }
        }

        deleteProfileButton.addActionListener {
            val selected = profileComboBox.selectedItem as String? ?: return@addActionListener
            ClipCraftSettings.getInstance().deleteProfile(selected)
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
                val opts = gatherFields()
                ClipCraftSettings.getInstance().saveProfile(name, opts)
                ClipCraftSettings.getInstance().setActiveProfile(name)
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

        // Basic form
        val basicForm = FormBuilder.createFormBuilder()
            .addLabeledComponent(JLabel("Filter Regex:"), filterRegexField, 1, false)
            .addComponent(shareGistCheckBox)
            .panel
            .apply {
                border = BorderFactory.createTitledBorder("Basic Settings")
            }

        // Next-gen form
        val nextGenForm = FormBuilder.createFormBuilder()
            .addComponent(JLabel("Next-Gen Options:"))
            .addComponent(includeDirSummaryCheckBox)
            .addComponent(collapseBlankLinesCheckBox)
            .addComponent(removeLeadingBlanksCheckBox)
            .addComponent(singleLineOutputCheckBox)
            .addComponent(enableChunkingCheckBox)
            .addLabeledComponent(JLabel("Max Chunk Size:"), chunkSizeField)
            .panel
            .apply {
                border = BorderFactory.createTitledBorder("Next-Gen")
            }

        // Macros & multi export
        val advancedForm = FormBuilder.createFormBuilder()
            .addComponent(JLabel("Macros (KEY=VALUE):"))
            .addComponent(JScrollPane(macrosArea))
            .addLabeledComponent(JLabel("Additional Export Formats:"), multiExportComboBox, 1, false)
            .panel
            .apply {
                border = BorderFactory.createTitledBorder("Advanced Settings")
            }

        val centerForm = FormBuilder.createFormBuilder()
            .addComponent(basicForm)
            .addVerticalGap(10)
            .addComponent(nextGenForm)
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
            val active = settings.getActiveOptions()
            val activeName = settings.state.activeProfileName
            profileComboBox.selectedItem = activeName
            currentProfileName = activeName
        }
    }

    /**
     * Gather the fields from the UI into a ClipCraftOptions instance.
     */
    private fun gatherFields(): ClipCraftOptions {
        val settings = ClipCraftSettings.getInstance()
        val activeOpts = settings.getActiveOptions()

        // We only override fields presented in the UI
        activeOpts.filterRegex = filterRegexField.text
        activeOpts.shareToGistEnabled = shareGistCheckBox.isSelected

        val macroMap = macrosArea.text.lines()
            .filter { it.contains("=") }
            .associate {
                val (key, value) = it.split("=", limit = 2)
                key.trim() to value.trim()
            }
        activeOpts.macros = macroMap

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
        activeOpts.enableChunkingForGPT = enableChunkingCheckBox.isSelected
        activeOpts.maxChunkSize = chunkSizeField.text.toIntOrNull() ?: 3000

        return activeOpts
    }

    override fun isModified(): Boolean = true

    @Throws(ConfigurationException::class)
    override fun apply() {
        val newOpts = gatherFields()
        // Save to the active profile
        val settings = ClipCraftSettings.getInstance()
        settings.saveProfile(currentProfileName, newOpts)
    }

    override fun reset() {
        val settings = ClipCraftSettings.getInstance()
        val activeOpts = settings.getActiveOptions()

        filterRegexField.text = activeOpts.filterRegex
        shareGistCheckBox.isSelected = activeOpts.shareToGistEnabled

        macrosArea.text = activeOpts.macros.entries.joinToString("\n") { "${it.key}=${it.value}" }
        multiExportComboBox.selectedIndex = when (activeOpts.simultaneousExports) {
            setOf(OutputFormat.MARKDOWN, OutputFormat.HTML) -> 1
            setOf(OutputFormat.MARKDOWN, OutputFormat.HTML, OutputFormat.PLAIN) -> 2
            else -> 0
        }

        includeDirSummaryCheckBox.isSelected = activeOpts.includeDirectorySummary
        collapseBlankLinesCheckBox.isSelected = activeOpts.collapseBlankLines
        removeLeadingBlanksCheckBox.isSelected = activeOpts.removeLeadingBlankLines
        singleLineOutputCheckBox.isSelected = activeOpts.singleLineOutput
        enableChunkingCheckBox.isSelected = activeOpts.enableChunkingForGPT
        chunkSizeField.text = activeOpts.maxChunkSize.toString()
    }

    override fun disposeUIResources() {}
}
