package com.clipcraft.ui

import com.clipcraft.services.ClipCraftSettings
import javax.swing.JCheckBox
import javax.swing.JTextArea
import javax.swing.JTextField
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ClipCraftSettingsConfigurableTest {
    private lateinit var configurable: ClipCraftSettingsConfigurable

    @BeforeEach
    fun setUp() {
        configurable = ClipCraftSettingsConfigurable()
        configurable.createComponent()
        configurable.reset()
    }

    @Test
    fun testApplyUpdatesSettings() {
        val headerField = configurable.javaClass.getDeclaredField("headerField").apply { isAccessible = true }
            .get(configurable) as JTextField
        val footerField = configurable.javaClass.getDeclaredField("footerField").apply { isAccessible = true }
            .get(configurable) as JTextField
        val directoryStructureCheck = configurable.javaClass.getDeclaredField("directoryStructureCheck").apply { isAccessible = true }
            .get(configurable) as JCheckBox
        val previewArea = configurable.javaClass.getDeclaredField("previewArea").apply { isAccessible = true }
            .get(configurable) as JTextArea

        headerField.text = "New Snippet Header"
        footerField.text = "New Snippet Footer"
        directoryStructureCheck.isSelected = true

        configurable.apply()

        val settings = ClipCraftSettings.getInstance()
        val currentOptions = settings.getCurrentProfile().options

        assertEquals("New Snippet Header", currentOptions.snippetHeaderText)
        assertEquals("New Snippet Footer", currentOptions.snippetFooterText)
        assertTrue(currentOptions.includeDirectorySummary)

        val previewText = previewArea.text
        assertTrue(previewText.contains("New Snippet Header"))
        assertTrue(previewText.contains("New Snippet Footer"))
    }
}
