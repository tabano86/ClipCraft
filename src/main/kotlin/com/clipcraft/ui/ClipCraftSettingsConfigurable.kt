package com.clipcraft.ui

import com.clipcraft.model.CompressionMode
import com.clipcraft.model.OutputFormat
import com.clipcraft.services.ClipCraftProfileManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.ui.components.JBTextField
import javax.swing.*

class ClipCraftSettingsConfigurable : Configurable {
    private val profileManager = ClipCraftProfileManager()
    private val currentProfile = profileManager.currentProfile().copy()

    // Swing components for a simple settings UI
    private lateinit var rootPanel: JPanel

    private lateinit var includeLineNumbersCheck: JCheckBox
    private lateinit var removeImportsCheck: JCheckBox
    private lateinit var removeCommentsCheck: JCheckBox
    private lateinit var trimWhitespaceCheck: JCheckBox
    private lateinit var chunkSizeField: JBTextField
    private lateinit var outputFormatCombo: JComboBox<OutputFormat>
    private lateinit var compressionCombo: JComboBox<CompressionMode>

    override fun getDisplayName(): String = "ClipCraft"

    override fun createComponent(): JComponent {
        rootPanel = JPanel()
        rootPanel.layout = BoxLayout(rootPanel, BoxLayout.Y_AXIS)

        includeLineNumbersCheck = JCheckBox("Include line numbers?", currentProfile.options.includeLineNumbers)
        removeImportsCheck = JCheckBox("Remove import statements?", currentProfile.options.removeImports)
        removeCommentsCheck = JCheckBox("Remove comments?", currentProfile.options.removeComments)
        trimWhitespaceCheck = JCheckBox("Trim whitespace?", currentProfile.options.trimWhitespace)

        chunkSizeField = JBTextField(currentProfile.options.chunkSize.toString(), 10)

        outputFormatCombo = JComboBox(OutputFormat.values())
        outputFormatCombo.selectedItem = currentProfile.options.outputFormat

        compressionCombo = JComboBox(CompressionMode.values())
        compressionCombo.selectedItem = currentProfile.options.compressionMode

        rootPanel.add(includeLineNumbersCheck)
        rootPanel.add(removeImportsCheck)
        rootPanel.add(removeCommentsCheck)
        rootPanel.add(trimWhitespaceCheck)
        rootPanel.add(JLabel("Chunk Size:"))
        rootPanel.add(chunkSizeField)
        rootPanel.add(JLabel("Output Format:"))
        rootPanel.add(outputFormatCombo)
        rootPanel.add(JLabel("Compression Mode:"))
        rootPanel.add(compressionCombo)

        return rootPanel
    }

    override fun isModified(): Boolean {
        val opts = currentProfile.options
        if (opts.includeLineNumbers != includeLineNumbersCheck.isSelected) return true
        if (opts.removeImports != removeImportsCheck.isSelected) return true
        if (opts.removeComments != removeCommentsCheck.isSelected) return true
        if (opts.trimWhitespace != trimWhitespaceCheck.isSelected) return true
        if (opts.chunkSize.toString() != chunkSizeField.text.trim()) return true
        if (opts.outputFormat != outputFormatCombo.selectedItem) return true
        if (opts.compressionMode != compressionCombo.selectedItem) return true
        return false
    }

    @Throws(ConfigurationException::class)
    override fun apply() {
        val opts = currentProfile.options
        opts.includeLineNumbers = includeLineNumbersCheck.isSelected
        opts.removeImports = removeImportsCheck.isSelected
        opts.removeComments = removeCommentsCheck.isSelected
        opts.trimWhitespace = trimWhitespaceCheck.isSelected
        opts.chunkSize = chunkSizeField.text.trim().toIntOrNull() ?: 4000
        opts.outputFormat = outputFormatCombo.selectedItem as OutputFormat
        opts.compressionMode = compressionCombo.selectedItem as CompressionMode

        // Save back to the real settings
        val pm = ClipCraftProfileManager()
        pm.deleteProfile(currentProfile.profileName) // remove old
        pm.addProfile(currentProfile)                 // add updated
        pm.switchProfile(currentProfile.profileName)
    }

    override fun reset() {
        // revert UI from currentProfile
        val opts = currentProfile.options
        includeLineNumbersCheck.isSelected = opts.includeLineNumbers
        removeImportsCheck.isSelected = opts.removeImports
        removeCommentsCheck.isSelected = opts.removeComments
        trimWhitespaceCheck.isSelected = opts.trimWhitespace
        chunkSizeField.text = opts.chunkSize.toString()
        outputFormatCombo.selectedItem = opts.outputFormat
        compressionCombo.selectedItem = opts.compressionMode
    }
}
