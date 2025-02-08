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
        val finalOpts = wizard.getConfiguredOptions()
        assertEquals(ThemeMode.SYSTEM_DEFAULT, finalOpts.themeMode)
    }
}
