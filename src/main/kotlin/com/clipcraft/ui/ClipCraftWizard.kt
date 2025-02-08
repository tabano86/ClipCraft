package com.clipcraft.ui

import com.clipcraft.model.ClipCraftOptions
import com.intellij.openapi.ui.DialogWrapper
import javax.swing.*

class ClipCraftWizard(private val initialOptions: ClipCraftOptions) : DialogWrapper(true) {

    private val stepPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }

    // Example: three steps, but you can expand.
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

    // ... more components per step ...

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
                // Step 1 items
                stepPanel.add(lineNumbersCheck)
                stepPanel.add(themeCombo)
            }
            1 -> {
                // Step 2 items
                // e.g., advanced filtering, macros, etc.
            }
            2 -> {
                // Step 3 summary
                stepPanel.add(JLabel("All set!"))
            }
        }
        stepPanel.revalidate()
        stepPanel.repaint()
    }

    override fun doNextAction() {
        if (currentStep < stepLabels.size - 1) {
            currentStep++
            updateStep()
        } else {
            // final step -> OK
            doOKAction()
        }
    }

    fun getConfiguredOptions(): ClipCraftOptions {
        // gather final options from wizard UI
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
