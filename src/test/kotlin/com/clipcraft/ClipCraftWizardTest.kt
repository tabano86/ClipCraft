package com.clipcraft

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.ThemeMode
import com.clipcraft.ui.ClipCraftWizard
import org.junit.Assert.assertEquals
import org.junit.Test

class ClipCraftWizardTest {

    @Test
    fun testWizardDefaultTheme() {
        val initial = ClipCraftOptions()
        val wizard = ClipCraftWizard(initial)
        // Normally you'd show the wizard, user changes something, then getConfiguredOptions.
        // Here we simulate direct access for test.
        val finalOpts = wizard.getConfiguredOptions()
        assertEquals(ThemeMode.SYSTEM_DEFAULT, finalOpts.themeMode)
    }
}
