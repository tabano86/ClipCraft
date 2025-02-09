package com.clipcraft

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.ui.ClipCraftSetupWizardCore
import org.junit.Assert.*
import org.junit.Test

class ClipCraftSetupWizardCoreTest {

    @Test
    fun testDefaultCoreValues() {
        val initial = ClipCraftOptions()
        val core = ClipCraftSetupWizardCore(initial)
        assertEquals(0, core.getCurrentStep())
        assertFalse(core.skipAdvanced)
    }

    @Test
    fun testSkipAdvancedStep() {
        val initial = ClipCraftOptions(
            enableChunkingForGPT = false,
            includeDirectorySummary = true,
            removeComments = true,
            removeLeadingBlankLines = true,
            useGitIgnore = true
        )
        val core = ClipCraftSetupWizardCore(initial)
        core.skipAdvanced = true
        core.goNextStep() // Should jump to final step.
        assertEquals(2, core.getCurrentStep())
        val final = core.getFinalOptions()
        assertTrue(final.includeDirectorySummary)
        assertTrue(final.removeComments)
        assertTrue(final.removeLeadingBlankLines)
        assertTrue(final.useGitIgnore)
    }

    @Test
    fun testChunkSizeDefaults() {
        val initial = ClipCraftOptions(maxChunkSize = 5000)
        val core = ClipCraftSetupWizardCore(initial)
        core.chunkSize = -10 // simulate invalid input
        val final = core.getFinalOptions()
        assertEquals(3000, final.maxChunkSize) // forced default if <= 0
    }

    @Test
    fun testMultipleSteps() {
        val initial = ClipCraftOptions(includeLineNumbers = false)
        val core = ClipCraftSetupWizardCore(initial)
        // Simulate going through all steps.
        core.goNextStep() // step 0 -> 1
        core.goNextStep() // step 1 -> 2
        core.goNextStep() // no effect; already at final step.
        assertEquals(2, core.getCurrentStep())
        core.includeLineNumbers = true
        val final = core.getFinalOptions()
        assertTrue(final.includeLineNumbers)
    }

    @Test
    fun testNoUserInteraction() {
        val initial = ClipCraftOptions(includeLineNumbers = true, maxChunkSize = 1234)
        val core = ClipCraftSetupWizardCore(initial)
        val final = core.getFinalOptions()
        assertTrue(final.includeLineNumbers)
        assertEquals(1234, final.maxChunkSize)
    }
}
