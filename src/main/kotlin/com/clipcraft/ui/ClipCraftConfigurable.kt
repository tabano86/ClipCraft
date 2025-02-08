package com.clipcraft.ui

import com.clipcraft.model.OutputFormat
import com.clipcraft.services.ClipCraftSettings
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.util.ui.FormBuilder
import javax.swing.BorderFactory
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.JScrollPane
import javax.swing.JTextArea

/**
 * The ClipCraft settings panel. This UI groups settings into clear sections with descriptions,
 * shows default values, and provides helpful tooltips so the user knows what each option does.
 */
class ClipCraftConfigurable : Configurable {

    private val mainPanel: JPanel

    // Basic settings
    private val filterRegexField: JTextField = JTextField(30).apply {
        toolTipText = "Enter a regular expression to filter out files (e.g. .*\\.kt for Kotlin files)."
    }
    private val shareGistCheckBox: JCheckBox = JCheckBox("Share to Gist").apply {
        toolTipText = "When enabled, ClipCraft will automatically attempt to share your output as a GitHub Gist."
    }

    // Advanced settings
    private val macrosArea: JTextArea = JTextArea(5, 30).apply {
        toolTipText = "Enter macros in the format KEY=VALUE (one per line). For example:\nPROJECT=MyProject"
    }
    private val multiExportComboBox = javax.swing.JComboBox(arrayOf("None", "Markdown, HTML", "Markdown, HTML, Plain")).apply {
        toolTipText = "Select additional formats to export along with your primary format."
    }

    init {
        // Build the settings form using FormBuilder for a clear, form–style layout.
        val basicForm = FormBuilder.createFormBuilder()
            .addLabeledComponent(JLabel("Filter Regex:"), filterRegexField, 1, false)
            .addComponent(shareGistCheckBox)
            .panel.also {
                it.border = BorderFactory.createTitledBorder("Basic Settings")
            }

        val advancedForm = FormBuilder.createFormBuilder()
            .addComponent(JLabel("Macros (one per line, e.g., KEY=VALUE):"))
            .addComponent(JScrollPane(macrosArea))
            .addLabeledComponent(JLabel("Additional Export Formats:"), multiExportComboBox, 1, false)
            .panel.also {
                it.border = BorderFactory.createTitledBorder("Advanced Settings")
            }

        mainPanel = FormBuilder.createFormBuilder()
            .addComponent(basicForm)
            .addVerticalGap(10)
            .addComponent(advancedForm)
            .panel
    }

    override fun getDisplayName(): String = "ClipCraft"

    override fun createComponent(): JComponent {
        reset()
        return mainPanel
    }

    override fun isModified(): Boolean {
        // In production, you should compare each field with the stored state.
        // For brevity, this returns true.
        return true
    }

    @Throws(ConfigurationException::class)
    override fun apply() {
        val opts = ClipCraftSettings.getInstance().state

        opts.filterRegex = filterRegexField.text
        opts.shareToGistEnabled = shareGistCheckBox.isSelected

        // Parse macros from each line of text (ignore empty lines)
        val macroMap = macrosArea.text.lines()
            .filter { it.contains("=") }
            .associate {
                val (key, value) = it.split("=", limit = 2)
                key.trim() to value.trim()
            }
        opts.macros = macroMap

        opts.simultaneousExports = when (multiExportComboBox.selectedIndex) {
            1 -> setOf(OutputFormat.MARKDOWN, OutputFormat.HTML)
            2 -> setOf(OutputFormat.MARKDOWN, OutputFormat.HTML, OutputFormat.PLAIN)
            else -> emptySet()
        }

        ClipCraftSettings.getInstance().loadState(opts)
    }

    override fun reset() {
        val opts = ClipCraftSettings.getInstance().state
        filterRegexField.text = opts.filterRegex
        shareGistCheckBox.isSelected = opts.shareToGistEnabled
        macrosArea.text = opts.macros.entries.joinToString("\n") { "${it.key}=${it.value}" }
        multiExportComboBox.selectedIndex = when (opts.simultaneousExports) {
            setOf(OutputFormat.MARKDOWN, OutputFormat.HTML) -> 1
            setOf(OutputFormat.MARKDOWN, OutputFormat.HTML, OutputFormat.PLAIN) -> 2
            else -> 0
        }
    }

    override fun disposeUIResources() {}
}
