package com.clipcraft.ui

import com.clipcraft.model.CompressionMode
import com.clipcraft.model.OutputFormat
import com.clipcraft.services.ClipCraftSettings
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.panel
import javax.swing.JCheckBox

class ClipCraftQuickOptionsPanel {

    val panel: DialogPanel
    private val currentOptions = ClipCraftSettings.getInstance().getCurrentProfile().options

    private lateinit var outputFormatCombo: ComboBox<OutputFormat>
    private lateinit var compressionModeCombo: ComboBox<CompressionMode>
    private lateinit var removeCommentsCheckBox: JCheckBox

    init {
        panel = panel {
            row("Output Format:") {
                outputFormatCombo = ComboBox(OutputFormat.values())
                cell(outputFormatCombo)
            }
            row("Compression Mode:") {
                compressionModeCombo = ComboBox(CompressionMode.values())
                cell(compressionModeCombo)
            }
            row {
                removeCommentsCheckBox = JCheckBox("Remove Comments", currentOptions.removeComments)
                cell(removeCommentsCheckBox)
            }
        }
        // Initialize
        outputFormatCombo.selectedItem = currentOptions.outputFormat
        compressionModeCombo.selectedItem = currentOptions.compressionMode
        removeCommentsCheckBox.isSelected = currentOptions.removeComments
    }

    fun applyChanges() {
        val settingsService = ClipCraftSettings.getInstance()
        val currentProfile = settingsService.getCurrentProfile()
        val newOptions = currentProfile.options.copy(
            outputFormat = outputFormatCombo.selectedItem as OutputFormat,
            compressionMode = compressionModeCombo.selectedItem as CompressionMode,
            removeComments = removeCommentsCheckBox.isSelected
        )
        settingsService.addProfile(currentProfile.copy(options = newOptions))
        settingsService.setCurrentProfile(currentProfile.profileName)
    }
}
