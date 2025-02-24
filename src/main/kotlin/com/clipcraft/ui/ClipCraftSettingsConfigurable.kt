package com.clipcraft.ui

import com.clipcraft.services.ClipCraftSettingsState
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class ClipCraftSettingsConfigurable : SearchableConfigurable, Configurable.NoScroll {

    private val settings = ClipCraftSettingsState.getInstance()
    private var modified = false

    override fun getId(): String = "clipcraft.settings"
    override fun getDisplayName(): String = "ClipCraft"

    override fun createComponent(): JComponent {
        return panel {
            row("Max Copy Characters:") {
                intTextField()
                    .label("If combined content exceeds this limit, you will be asked to confirm copying.")
                    .bindIntText(settings::maxCopyCharacters)
                    .columns(10)
                    .onChanged {
                        modified = true
                    }
            }
            // Replacing noteRow with a simple label row
            row {
                label("Set to 0 for no limit (be careful with large copies).")
                    .comment("Caution: copying very large content can freeze or overload other apps.")
            }
        }
    }

    override fun isModified(): Boolean = modified

    @Throws(ConfigurationException::class)
    override fun apply() {
        modified = false
    }

    override fun reset() {
        modified = false
    }
}
