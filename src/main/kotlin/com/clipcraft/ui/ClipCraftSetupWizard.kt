package com.clipcraft.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import java.awt.CardLayout
import java.awt.event.ActionEvent
import javax.swing.*

/**
 * A multi-step setup wizard using DialogWrapper, with Next/Back/Finish.
 * This implementation uses a CardLayout to switch between steps.
 */
class ClipCraftSetupWizard(private val project: Project) : DialogWrapper(true) {

    private val wizardCore = ClipCraftSetupWizardCore(project)
    private val wizardUI = ClipCraftSetupWizardUI()  // Existing UI fields

    // Create a panel with a CardLayout for the wizard steps.
    private val cardLayout = CardLayout()
    private val wizardPanel = JPanel(cardLayout)

    // Step 0: Welcome panel.
    private val welcomePanel: JPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        add(Box.createVerticalStrut(20))
        val label = JLabel("Welcome to ClipCraft Setup Wizard!")
        label.alignmentX = 0.5f
        add(label)
        add(Box.createVerticalStrut(20))
        add(JLabel("This wizard helps configure basic ClipCraft settings:"))
        add(Box.createVerticalStrut(10))
        add(JLabel("• Metadata Inclusion"))
        add(JLabel("• GitIgnore usage"))
        add(JLabel("• Concurrency"))
        add(Box.createVerticalGlue())
    }

    // Step 1: Existing settings UI panel.
    private val settingsPanel: JPanel = wizardUI.getMainPanel()

    // Current step index (0-based)
    private var stepIndex = 0

    // Custom actions stored as properties so we can enable/disable them.
    private val backAction = object : DialogWrapperAction("Back") {
        override fun doAction(e: ActionEvent?) {
            if (stepIndex > 0) {
                stepIndex--
                updateStepUI()
            }
        }
    }

    private val nextAction = object : DialogWrapperAction("Next") {
        override fun doAction(e: ActionEvent?) {
            if (stepIndex < 1) { // Only 2 steps in this example
                stepIndex++
                updateStepUI()
            }
        }
    }

    private val finishAction = object : DialogWrapperAction("Finish") {
        override fun doAction(e: ActionEvent?) {
            applyWizardResults()
            close(OK_EXIT_CODE)
        }
    }

    init {
        title = "ClipCraft Setup Wizard"
        // Add steps to the card panel.
        wizardPanel.add(welcomePanel, "Step0")
        wizardPanel.add(settingsPanel, "Step1")
        init() // Must call init() from DialogWrapper.
    }

    override fun createCenterPanel(): JComponent = wizardPanel

    override fun createActions(): Array<Action> {
        // Return our custom actions plus the cancel action.
        return arrayOf(backAction, nextAction, finishAction, cancelAction)
    }

    /**
     * Updates the displayed step and enables/disables buttons accordingly.
     */
    private fun updateStepUI() {
        cardLayout.show(wizardPanel, "Step$stepIndex")
        backAction.isEnabled = stepIndex > 0
        nextAction.isEnabled = stepIndex < 1
    }

    /**
     * Applies the wizard results using the existing wizard core.
     */
    private fun applyWizardResults() {
        wizardCore.applyWizardResults(wizardUI)
    }

    /**
     * If the user triggers the default OK action, we apply wizard results.
     */
    override fun doOKAction() {
        applyWizardResults()
        super.doOKAction()
    }
}
