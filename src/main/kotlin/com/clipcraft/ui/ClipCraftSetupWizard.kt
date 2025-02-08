package com.clipcraft.ui

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.ThemeMode
import com.intellij.openapi.ui.DialogWrapper
import javax.swing.*

class ClipCraftSetupWizard(private val initialOptions: ClipCraftOptions) : DialogWrapper(true) {

    private val stepPanel = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }
    private var currentStep = 0
    private val stepLabels = listOf(
        JLabel("Welcome to ClipCraft! Let’s set up your preferences."),
        JLabel("Configure Output and Filtering"),
        JLabel("Review and Finish")
    )
    private val lineNumbersCheckBox = JCheckBox("Include Line Numbers", initialOptions.includeLineNumbers)
    private val themeComboBox = JComboBox(arrayOf("System Default", "Light", "Dark")).apply {
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
                stepPanel.add(lineNumbersCheckBox)
                stepPanel.add(themeComboBox)
            }

            1 -> {
                // Future controls can be added here.
            }

            2 -> {
                stepPanel.add(JLabel("All set!"))
            }
        }
        stepPanel.revalidate()
        stepPanel.repaint()
    }

    // Custom next action (not an override)
    fun doNextAction() {
        if (currentStep < stepLabels.size - 1) {
            currentStep++
            updateStep()
        } else {
            doOKAction()
        }
    }

    fun getConfiguredOptions(): ClipCraftOptions {
        return initialOptions.copy(
            includeLineNumbers = lineNumbersCheckBox.isSelected,
            themeMode = when (themeComboBox.selectedIndex) {
                1 -> ThemeMode.LIGHT
                2 -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM_DEFAULT
            }
        )
    }
}
