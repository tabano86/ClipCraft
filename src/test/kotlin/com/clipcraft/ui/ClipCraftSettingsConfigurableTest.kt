package com.clipcraft.ui

import com.clipcraft.services.ClipCraftSettings
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.swing.JCheckBox
import javax.swing.JTextArea
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        // Updated reflection field names: "headerArea" and "footerArea" (both JTextArea)
        val headerArea = configurable.javaClass.getDeclaredField("headerArea").apply { isAccessible = true }
            .get(configurable) as JTextArea
        val footerArea = configurable.javaClass.getDeclaredField("footerArea").apply { isAccessible = true }
            .get(configurable) as JTextArea
        val directoryStructureCheck = configurable.javaClass.getDeclaredField("directoryStructureCheck")
            .apply { isAccessible = true }
            .get(configurable) as JCheckBox
        val previewArea = configurable.javaClass.getDeclaredField("previewArea")
            .apply { isAccessible = true }
            .get(configurable) as JTextArea

        // Set new values
        headerArea.text = "New Snippet Header"
        footerArea.text = "New Snippet Footer"
        directoryStructureCheck.isSelected = true

        // Apply the settings
        configurable.apply()

        // Retrieve the current options from the global settings
        val settings = ClipCraftSettings.getInstance()
        val currentOptions = settings.getCurrentProfile().options

        // Verify that the new header, footer, and directory summary flag have been applied
        assertEquals("New Snippet Header", currentOptions.snippetHeaderText)
        assertEquals("New Snippet Footer", currentOptions.snippetFooterText)
        assertTrue(currentOptions.includeDirectorySummary)

        // Also verify that the preview area text now contains the new header and footer
        val previewText = previewArea.text
        assertTrue(previewText.contains("New Snippet Header"))
        assertTrue(previewText.contains("New Snippet Footer"))
    }
}
