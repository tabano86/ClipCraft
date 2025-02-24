package com.clipcraft.ui

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.ui.JBUI
import java.awt.CardLayout
import javax.swing.Action
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class ClipCraftSetupWizardUI {
    private val includeMetadataCheck = javax.swing.JCheckBox("Include metadata (file size, timestamp, etc.)", true)
    private val useGitIgnoreCheck = javax.swing.JCheckBox("Use .gitignore", true)
    private val concurrencyField = javax.swing.JTextField("4", 5)

    private val mainPanel: JPanel = com.intellij.ui.dsl.builder.panel {
        row("Include Metadata:") {
            cell(includeMetadataCheck)
        }
        row("Use .gitignore:") {
            cell(useGitIgnoreCheck)
        }
        row("Max Concurrent Tasks:") {
            cell(concurrencyField)
        }
    }

    fun getMainPanel(): JPanel = mainPanel
    fun isIncludeMetadata() = includeMetadataCheck.isSelected
    fun isUseGitIgnore() = useGitIgnoreCheck.isSelected
    fun getMaxConcurrentTasks() = concurrencyField.text.toIntOrNull() ?: 4
}

class ClipCraftSetupWizard(private val project: com.intellij.openapi.project.Project) : DialogWrapper(true) {
    private val wizardCore = ClipCraftSetupWizardCore(project)
    private val wizardUI = ClipCraftSetupWizardUI()
    private val cardLayout = CardLayout()
    private val wizardPanel = JPanel(cardLayout)

    private val welcomePanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = JBUI.Borders.empty(20)
        add(JLabel("Welcome to ClipCraft Setup Wizard!").apply { alignmentX = JComponent.CENTER_ALIGNMENT })
        add(Box.createVerticalStrut(JBUI.scale(20)))
        add(
            JLabel("This wizard helps configure basic ClipCraft settings:").apply {
                alignmentX = JComponent.CENTER_ALIGNMENT
            },
        )
        add(Box.createVerticalStrut(JBUI.scale(10)))
        add(JLabel("• Metadata Inclusion").apply { alignmentX = JComponent.CENTER_ALIGNMENT })
        add(JLabel("• GitIgnore usage").apply { alignmentX = JComponent.CENTER_ALIGNMENT })
        add(JLabel("• Concurrency").apply { alignmentX = JComponent.CENTER_ALIGNMENT })
        add(Box.createVerticalGlue())
    }

    private var stepIndex = 0

    private val backAction = object : DialogWrapperAction("Back") {
        override fun doAction(e: java.awt.event.ActionEvent?) {
            if (stepIndex > 0) {
                stepIndex--
                updateStepUI()
            }
        }
    }

    private val nextAction = object : DialogWrapperAction("Next") {
        override fun doAction(e: java.awt.event.ActionEvent?) {
            if (stepIndex < 1) {
                stepIndex++
                updateStepUI()
            }
        }
    }

    private val finishAction = object : DialogWrapperAction("Finish") {
        override fun doAction(e: java.awt.event.ActionEvent?) {
            applyWizardResults()
            close(OK_EXIT_CODE)
        }
    }

    init {
        title = "ClipCraft Setup Wizard"
        wizardPanel.add(welcomePanel, "Step0")
        wizardPanel.add(wizardUI.getMainPanel(), "Step1")
        init()
        updateStepUI()
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
