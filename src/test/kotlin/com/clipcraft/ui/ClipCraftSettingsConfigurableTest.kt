// ClipCraftSettingsConfigurableTest.kt
package com.clipcraft.ui

import com.clipcraft.services.ClipCraftSettings
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.swing.JCheckBox
import javax.swing.JTextArea
import javax.swing.JTextField
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit test for verifying that changes in the settings UI are immediately reflected
 * in the underlying ClipCraftOptions.
 */
class ClipCraftSettingsConfigurableTest {

    private lateinit var configurable: ClipCraftSettingsConfigurable

    @BeforeEach
    fun setUp() {
        // Instantiate and initialize the configurable. Calling createComponent() sets up all UI components.
        configurable = ClipCraftSettingsConfigurable()
        configurable.createComponent()
        configurable.reset() // Populate UI fields with current options
    }

    @Test
    fun testApplyUpdatesSettings() {
        // Use reflection correctly to retrieve the field values by calling get(instance) on the Field.
        val headerField = configurable.javaClass
            .getDeclaredField("headerField")
            .apply { isAccessible = true }
            .get(configurable) as JTextField

        val footerField = configurable.javaClass
            .getDeclaredField("footerField")
            .apply { isAccessible = true }
            .get(configurable) as JTextField

        val directoryStructureCheck = configurable.javaClass
            .getDeclaredField("directoryStructureCheck")
            .apply { isAccessible = true }
            .get(configurable) as JCheckBox

        val previewArea = configurable.javaClass
            .getDeclaredField("previewArea")
            .apply { isAccessible = true }
            .get(configurable) as JTextArea

        // Simulate user updates.
        headerField.text = "New Header Text"
        footerField.text = "New Footer Text"
        directoryStructureCheck.isSelected = true

        // Call apply() to commit the changes to the underlying settings.
        configurable.apply()

        // Verify that the settings in ClipCraftSettings have been updated.
        val settings = ClipCraftSettings.getInstance()
        val currentOptions = settings.getCurrentProfile().options

        assertEquals("New Header Text", currentOptions.gptHeaderText)
        assertEquals("New Footer Text", currentOptions.gptFooterText)
        assertTrue(currentOptions.includeDirectorySummary)

        // Also check that the preview area reflects the updated header and footer.
        val previewText = previewArea.text
        assertTrue(previewText.contains("New Header Text"), "Preview should contain the new header")
        assertTrue(previewText.contains("New Footer Text"), "Preview should contain the new footer")
    }
}
