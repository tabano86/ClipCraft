package com.clipcraft

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.ThemeMode
import com.clipcraft.ui.ClipCraftSetupWizard
import org.junit.Assert.assertEquals
import org.junit.Test

class ClipCraftWizardTest {

    @Test
    fun testWizardDefaultTheme() {
        val initial = ClipCraftOptions()
        val wizard = ClipCraftSetupWizard(initial)
        // We do not actually show the wizard in test, we just verify the default state
        val finalOpts = wizard.getConfiguredOptions()
        assertEquals(ThemeMode.SYSTEM_DEFAULT, finalOpts.themeMode)
    }
}
