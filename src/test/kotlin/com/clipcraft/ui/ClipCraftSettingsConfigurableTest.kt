package com.clipcraft.ui

import com.clipcraft.services.ClipCraftSettings
import com.intellij.testFramework.LightPlatform4TestCase
import com.intellij.ui.EditorTextField
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.swing.JCheckBox
import javax.swing.JTextArea

class ClipCraftSettingsConfigurableTest : LightPlatform4TestCase() {

    private lateinit var configurable: ClipCraftSettingsConfigurable

    @BeforeEach
    override fun setUp() {
        // Bootstraps the IntelliJ Platform environment.
        super.setUp()
        configurable = ClipCraftSettingsConfigurable()
        configurable.createComponent()
        configurable.reset()
    }

    @Test
    fun testApplyUpdatesSettings() {
        val headerArea = configurable.javaClass.getDeclaredField("headerArea").apply { isAccessible = true }
            .get(configurable) as JTextArea
        val footerArea = configurable.javaClass.getDeclaredField("footerArea").apply { isAccessible = true }
            .get(configurable) as JTextArea
        val directoryStructureCheck = configurable.javaClass.getDeclaredField("directoryStructureCheck").apply { isAccessible = true }
            .get(configurable) as JCheckBox
        val previewEditor = configurable.javaClass.getDeclaredField("previewEditor").apply { isAccessible = true }
            .get(configurable) as EditorTextField

        headerArea.text = "New Snippet Header"
        footerArea.text = "New Snippet Footer"
        directoryStructureCheck.isSelected = true

        configurable.apply()

        val settings = ClipCraftSettings.getInstance()
        val currentOptions = settings.getCurrentProfile().options
        assertEquals("New Snippet Header", currentOptions.snippetHeaderText)
        assertEquals("New Snippet Footer", currentOptions.snippetFooterText)
        assertTrue(currentOptions.includeDirectorySummary)

        val previewText = previewEditor.text
        assertTrue(previewText.contains("New Snippet Header"))
        assertTrue(previewText.contains("New Snippet Footer"))
    }
}
