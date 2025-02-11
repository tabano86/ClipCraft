package com.clipcraft.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import java.awt.CardLayout
import java.awt.event.ActionEvent
import javax.swing.*

/**
 * A multi-step setup wizard using DialogWrapper and CardLayout.
 */
class ClipCraftSetupWizard(private val project: Project) : DialogWrapper(true) {

    private val wizardCore = ClipCraftSetupWizardCore(project)
    private val wizardUI = ClipCraftSetupWizardUI()

    private val cardLayout = CardLayout()
    private val wizardPanel = JPanel(cardLayout)

    private val welcomePanel = JPanel().apply {
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
    private val settingsPanel = wizardUI.getMainPanel()

    private var stepIndex = 0

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
            if (stepIndex < 1) {
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
        wizardPanel.add(welcomePanel, "Step0")
        wizardPanel.add(settingsPanel, "Step1")
        init()
    }

    override fun createCenterPanel(): JComponent = wizardPanel
    override fun createActions(): Array<Action> = arrayOf(backAction, nextAction, finishAction, cancelAction)

    private fun updateStepUI() {
        cardLayout.show(wizardPanel, "Step$stepIndex")
        backAction.isEnabled = stepIndex > 0
        nextAction.isEnabled = stepIndex < 1
    }

    private fun applyWizardResults() {
        wizardCore.applyWizardResults(wizardUI)
    }

    override fun doOKAction() {
        applyWizardResults()
        super.doOKAction()
    }
}

