package com.clipcraft.ui

import com.clipcraft.model.ClipCraftOptions
import com.intellij.openapi.ui.DialogWrapper
import javax.swing.BoxLayout
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JComponent

/**
 * A simple setup wizard that lets the user configure a few basic options.
 * (Theme selection has been removed since we rely on the IDE’s theme.)
 */
class ClipCraftSetupWizard(private val initialOptions: ClipCraftOptions) : DialogWrapper(true) {

    private val stepPanel = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }
    private var currentStep = 0
    private val stepLabels = listOf(
        JLabel("Welcome to ClipCraft! Let’s set up your preferences."),
        JLabel("Configure Output and Filtering"),
        JLabel("Review and Finish")
    )
    private val lineNumbersCheckBox = JCheckBox("Include Line Numbers", initialOptions.includeLineNumbers)

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
            0 -> stepPanel.add(lineNumbersCheckBox)
            1 -> {
                // Future steps (e.g. filtering options) can be added here.
            }
            2 -> stepPanel.add(JLabel("All set!"))
        }
        stepPanel.revalidate()
        stepPanel.repaint()
    }

    /**
     * Manually advances the wizard. (Note: DialogWrapper does not provide built-in next/prev; this is custom.)
     */
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
            includeLineNumbers = lineNumbersCheckBox.isSelected
        )
    }
}
