package com.clipcraft

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.ThemeMode
import com.clipcraft.ui.ClipCraftSetupWizard
import org.junit.Assert.assertEquals
import org.junit.Test
import javax.swing.JCheckBox

class ClipCraftSetupWizardTest {

    @Test
    fun testWizardDefaultTheme() {
        val initial = ClipCraftOptions()
        val wizard = ClipCraftSetupWizard(initial)
        // Without user interaction, options remain unchanged.
        val finalOpts = wizard.getConfiguredOptions()
        assertEquals(ThemeMode.SYSTEM_DEFAULT, finalOpts.themeMode)
    }

    @Test
    fun testSkipAdvancedStep() {
        val initial = ClipCraftOptions(
            includeLineNumbers = true,
            enableChunkingForGPT = false,
            maxChunkSize = 2000,
            includeDirectorySummary = true,
            removeComments = true,
            removeLeadingBlankLines = true,
            useGitIgnore = true
        )
        val wizard = ClipCraftSetupWizard(initial)
        // Use reflection to simulate user selecting "Skip Advanced Step"
        val field = wizard.javaClass.getDeclaredField("skipAdvancedCheckBox")
        field.isAccessible = true
        (field.get(wizard) as JCheckBox).isSelected = true

        // Simulate advancing the wizard.
        wizard.doNextAction() // from step 0 to step 2 (skipping advanced step)
        val finalOpts = wizard.getConfiguredOptions()
        // Advanced options should remain as originally set.
        assertEquals(initial.includeDirectorySummary, finalOpts.includeDirectorySummary)
        assertEquals(initial.removeComments, finalOpts.removeComments)
        assertEquals(initial.removeLeadingBlankLines, finalOpts.removeLeadingBlankLines)
        assertEquals(initial.useGitIgnore, finalOpts.useGitIgnore)
    }
}
