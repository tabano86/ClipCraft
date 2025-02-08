package com.clipcraft.ui

import com.clipcraft.model.ClipCraftOptions
import com.intellij.openapi.ui.DialogWrapper
import javax.swing.*

class ClipCraftWizard(private val initialOptions: ClipCraftOptions) : DialogWrapper(true) {

    private val stepPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }

    private var currentStep = 0

    private val stepLabels = listOf(
        JLabel("Welcome to ClipCraft! Let’s set up your preferences."),
        JLabel("Configure Output and Filtering"),
        JLabel("Review and Finish")
    )

    private val lineNumbersCheck = JCheckBox("Include Line Numbers", initialOptions.includeLineNumbers)
    private val themeCombo = JComboBox(arrayOf("System Default", "Light", "Dark")).apply {
        selectedIndex = initialOptions.themeMode.ordinal
    }

    init {
        title = "ClipCraft Setup Wizard"
        init()
        updateStep()
    }

    override fun createCenterPanel(): JComponent = stepPanel

    override fun getPreferredFocusedComponent(): JComponent = stepPanel

    private fun updateStep() {
        stepPanel.removeAll()
        stepPanel.add(stepLabels[currentStep])
        when (currentStep) {
            0 -> {
                stepPanel.add(lineNumbersCheck)
                stepPanel.add(themeCombo)
            }
            1 -> {
                // Future step: add controls for filtering and output options
            }
            2 -> {
                stepPanel.add(JLabel("All set!"))
            }
        }
        stepPanel.revalidate()
        stepPanel.repaint()
    }

    fun getConfiguredOptions(): ClipCraftOptions {
        val newOpts = initialOptions.copy(
            includeLineNumbers = lineNumbersCheck.isSelected,
            themeMode = when (themeCombo.selectedIndex) {
                1 -> com.clipcraft.model.ThemeMode.LIGHT
                2 -> com.clipcraft.model.ThemeMode.DARK
                else -> com.clipcraft.model.ThemeMode.SYSTEM_DEFAULT
            }
        )
        return newOpts
    }
}
