package com.clipcraft.ui

import com.clipcraft.model.OutputFormat
import com.clipcraft.model.ThemeMode
import com.clipcraft.services.ClipCraftSettings
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import javax.swing.*

/**
 * Main settings panel for ClipCraft, displayed in IntelliJ's Settings > Tools > ClipCraft.
 */
class ClipCraftConfigurable : Configurable {

    private val panel = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }

    private val themeComboBox = JComboBox(ThemeMode.values())
    private val filterRegexField = JTextField(30)
    private val shareGistCheckBox = JCheckBox("Share to Gist")
    private val macrosArea = JTextArea(5, 30).apply {
        toolTipText = "Enter macros as KEY=VALUE (one per line)."
    }
    private val multiExportComboBox = JComboBox(arrayOf("None", "Markdown,HTML", "Markdown,HTML,Plain"))

    override fun getDisplayName(): String = "ClipCraft"

    override fun createComponent(): JComponent {
        panel.add(JLabel("Theme:"))
        panel.add(themeComboBox)

        panel.add(JLabel("Filter Regex:"))
        panel.add(filterRegexField)

        panel.add(JLabel("Macros:"))
        panel.add(JScrollPane(macrosArea))

        panel.add(shareGistCheckBox)
        panel.add(multiExportComboBox)

        reset()
        return panel
    }

    override fun isModified(): Boolean {
        // Simplified: always returns true. In a production plugin, compare each field vs. ClipCraftSettings.getInstance().state
        return true
    }

    @Throws(ConfigurationException::class)
    override fun apply() {
        val opts = ClipCraftSettings.getInstance().state

        opts.themeMode = themeComboBox.selectedItem as ThemeMode
        opts.filterRegex = filterRegexField.text
        opts.shareToGistEnabled = shareGistCheckBox.isSelected

        val macroMap = mutableMapOf<String, String>()
        macrosArea.text.lines().forEach { line ->
            val kv = line.split("=")
            if (kv.size == 2) macroMap[kv[0].trim()] = kv[1].trim()
        }
        opts.macros = macroMap

        when (multiExportComboBox.selectedIndex) {
            1 -> opts.simultaneousExports = setOf(OutputFormat.MARKDOWN, OutputFormat.HTML)
            2 -> opts.simultaneousExports = setOf(OutputFormat.MARKDOWN, OutputFormat.HTML, OutputFormat.PLAIN)
            else -> opts.simultaneousExports = emptySet()
        }

        ClipCraftSettings.getInstance().loadState(opts)
    }

    override fun reset() {
        val opts = ClipCraftSettings.getInstance().state
        themeComboBox.selectedItem = opts.themeMode
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
