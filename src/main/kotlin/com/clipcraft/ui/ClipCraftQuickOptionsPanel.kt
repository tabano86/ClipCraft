package com.clipcraft.ui

import com.clipcraft.model.CompressionMode
import com.clipcraft.model.OutputFormat
import com.clipcraft.services.ClipCraftSettings
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.panel
import javax.swing.JCheckBox
import javax.swing.JComboBox

/**
 * A small panel that can pop up for quick overrides (e.g., holding Alt while triggering ClipCraft).
 */
class ClipCraftQuickOptionsPanel {

    val panel: DialogPanel
    private val currentOptions = ClipCraftSettings.getInstance().state.activeProfile.options

    // Use standard Swing components (JComboBox and JCheckBox)
    private lateinit var outputFormatCombo: JComboBox<OutputFormat>
    private lateinit var compressionModeCombo: JComboBox<CompressionMode>
    private lateinit var removeCommentsCheckBox: JCheckBox

    init {
        panel = panel {
            row("Output Format:") {
                outputFormatCombo = JComboBox(OutputFormat.values())
                cell(outputFormatCombo)
            }
            row("Compression Mode:") {
                compressionModeCombo = JComboBox(CompressionMode.values())
                cell(compressionModeCombo)
            }
            row {
                removeCommentsCheckBox = JCheckBox("Remove Comments", currentOptions.removeComments)
                cell(removeCommentsCheckBox)
            }
        }
        // Initialize UI components with current settings.
        outputFormatCombo.selectedItem = currentOptions.outputFormat
        compressionModeCombo.selectedItem = currentOptions.compressionMode
        removeCommentsCheckBox.isSelected = currentOptions.removeComments
    }

    /**
     * Call this method to update the persistent state with the overridden quick options.
     */
    fun applyChanges() {
        val settingsService = ClipCraftSettings.getInstance()
        val currentProfile = settingsService.state.activeProfile
        val newOptions = currentProfile.options.copy(
            outputFormat = outputFormatCombo.selectedItem as OutputFormat,
            compressionMode = compressionModeCombo.selectedItem as CompressionMode,
            removeComments = removeCommentsCheckBox.isSelected
        )
        // Update the active profile with new options.
        settingsService.state.activeProfile = currentProfile.copy(options = newOptions)
    }
}
