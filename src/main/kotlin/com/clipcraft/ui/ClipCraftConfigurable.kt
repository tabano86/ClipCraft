package com.clipcraft.ui

import com.clipcraft.model.*
import com.clipcraft.services.ClipCraftSettings
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import javax.swing.*

class ClipCraftConfigurable : Configurable {

    private val panel = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }

    private val themeCombo = JComboBox(ThemeMode.values())
    private val macrosArea = JTextArea(5, 30).apply {
        toolTipText = "Enter macros as KEY=VALUE (one per line)."
    }
    private val filterRegexField = JTextField(30)
    private val shareGistCheck = JCheckBox("Share to Gist")
    private val multiExportCombo = JComboBox(arrayOf("None", "Markdown,HTML", "Markdown,HTML,Plain"))

    override fun getDisplayName(): String = "ClipCraft"

    override fun createComponent(): JComponent {
        panel.add(JLabel("Theme:"))
        panel.add(themeCombo)
        panel.add(JLabel("Filter Regex:"))
        panel.add(filterRegexField)
        panel.add(JLabel("Macros:"))
        panel.add(JScrollPane(macrosArea))
        panel.add(shareGistCheck)
        panel.add(multiExportCombo)
        reset()
        return panel
    }

    override fun isModified(): Boolean {
        val opts = ClipCraftSettings.getInstance().state
        // This simplified version always returns true. Implement field-by-field checking for production.
        return true
    }

    @Throws(ConfigurationException::class)
    override fun apply() {
        val opts = ClipCraftSettings.getInstance().state
        opts.themeMode = themeCombo.selectedItem as ThemeMode
        opts.filterRegex = filterRegexField.text
        opts.shareToGistEnabled = shareGistCheck.isSelected
        val macroMap = mutableMapOf<String, String>()
        macrosArea.text.lines().forEach { line ->
            val kv = line.split("=")
            if (kv.size == 2) macroMap[kv[0].trim()] = kv[1].trim()
        }
        opts.macros = macroMap

        when (multiExportCombo.selectedIndex) {
            1 -> opts.simultaneousExports = setOf(OutputFormat.MARKDOWN, OutputFormat.HTML)
            2 -> opts.simultaneousExports = setOf(OutputFormat.MARKDOWN, OutputFormat.HTML, OutputFormat.PLAIN)
            else -> opts.simultaneousExports = emptySet()
        }

        ClipCraftSettings.getInstance().loadState(opts)
    }

    override fun reset() {
        val opts = ClipCraftSettings.getInstance().state
        themeCombo.selectedItem = opts.themeMode
        filterRegexField.text = opts.filterRegex
        shareGistCheck.isSelected = opts.shareToGistEnabled
        macrosArea.text = opts.macros.entries.joinToString("\n") { "${it.key}=${it.value}" }
        if (opts.simultaneousExports == setOf(OutputFormat.MARKDOWN, OutputFormat.HTML)) {
            multiExportCombo.selectedIndex = 1
        } else if (opts.simultaneousExports == setOf(OutputFormat.MARKDOWN, OutputFormat.HTML, OutputFormat.PLAIN)) {
            multiExportCombo.selectedIndex = 2
        } else {
            multiExportCombo.selectedIndex = 0
        }
    }

    override fun disposeUIResources() {}
}
