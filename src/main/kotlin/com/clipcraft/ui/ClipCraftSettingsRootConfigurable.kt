package com.clipcraft.ui

import com.intellij.openapi.options.SearchableConfigurable
import javax.swing.JComponent

/**
 * The root configurable for ClipCraft.
 * Now it provides its own content (by delegating to its first child) so that something
 * is shown immediately when the settings are opened.
 */
class ClipCraftSettingsRootConfigurable : SearchableConfigurable.Parent {

    override fun getId(): String = "clipcraft.settings"
    override fun getDisplayName(): String = "ClipCraft"
    override fun hasOwnContent(): Boolean = true
    override fun createComponent(): JComponent? = getConfigurables().firstOrNull()?.createComponent()

    override fun isModified(): Boolean = getConfigurables().firstOrNull()?.isModified == true
    override fun apply() {
        getConfigurables().firstOrNull()?.apply()
    }

    override fun reset() {
        getConfigurables().firstOrNull()?.reset()
    }

    override fun getConfigurables(): Array<SearchableConfigurable> = arrayOf(
        ClipCraftSettingsConfigurable(),
    )
}
