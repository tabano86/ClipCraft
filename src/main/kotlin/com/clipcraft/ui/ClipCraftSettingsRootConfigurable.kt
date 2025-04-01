package com.clipcraft.ui

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import javax.swing.JComponent

/**
 * The root configurable that hosts the ClipCraft settings.
 */
class ClipCraftSettingsRootConfigurable : SearchableConfigurable.Parent {
    // Store a single instance so `reset()` and `apply()` always refer to the same object
    private val configurable = ClipCraftSettingsConfigurable()

    override fun getId(): String = "clipcraft.settings"
    override fun getDisplayName(): String = "ClipCraft"
    override fun hasOwnContent(): Boolean = true

    override fun createComponent(): JComponent? =
        configurable.createComponent()

    override fun isModified(): Boolean =
        configurable.isModified

    override fun apply() {
        configurable.apply()
    }

    override fun reset() {
        configurable.reset()
    }

    override fun getConfigurables(): Array<Configurable> =
        arrayOf(configurable)
}
