package com.clipcraft.ui

import com.clipcraft.model.ClipCraftOptions
import com.intellij.openapi.ui.DialogWrapper
import org.slf4j.LoggerFactory
import javax.swing.BoxLayout
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

/**
 * ClipCraftSetupWizardUI wraps a ClipCraftSetupWizardCore instance into a Swing-based dialog.
 * It maps UI fields to the underlying core logic.
 */
class ClipCraftSetupWizardUI(
    private val core: ClipCraftSetupWizardCore
) : DialogWrapper(true) {

    private val log = LoggerFactory.getLogger(ClipCraftSetupWizardUI::class.java)
    private val stepPanel = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }

    private val stepLabels = listOf(
        JLabel("Welcome to ClipCraft! Let’s set up your preferences."),
        JLabel("Advanced Options"),
        JLabel("Review & Finish")
    )

    // Step 1 UI components
    private val lineNumbersCheckBox = JCheckBox("Include Line Numbers", core.includeLineNumbers)
    private val chunkingCheckBox = JCheckBox("Enable Chunking for GPT", core.enableChunkingForGPT)
    private val chunkSizeField = JTextField(core.chunkSize.toString(), 5)
    private val skipAdvancedCheckBox = JCheckBox("Skip Advanced Step If Unneeded", core.skipAdvanced)

    // Step 2 UI components
    private val directorySummaryCheckBox = JCheckBox("Include Directory Summary", core.includeDirectorySummary)
    private val removeCommentsCheckBox = JCheckBox("Remove Comments", core.removeComments)
    private val removeLeadingBlankLinesCheckBox = JCheckBox("Remove Leading Blank Lines", core.removeLeadingBlankLines)
    private val useGitIgnoreCheckBox = JCheckBox("Use .gitignore", core.useGitIgnore)

    init {
        title = "ClipCraft Setup Wizard"
        init() // Calls DialogWrapper.init() to build the dialog peer.
        updateStep()
    }

    override fun createCenterPanel(): JComponent = stepPanel

    override fun getPreferredFocusedComponent(): JComponent = stepPanel

    private fun updateStep() {
        stepPanel.removeAll()
        stepPanel.add(stepLabels[core.getCurrentStep()])

        when (core.getCurrentStep()) {
            0 -> buildBasicStep()
            1 -> buildAdvancedStepOrSkip()
            2 -> buildReviewStep()
        }
        stepPanel.revalidate()
        stepPanel.repaint()
    }

    private fun buildBasicStep() {
        stepPanel.add(lineNumbersCheckBox)
        stepPanel.add(JLabel("GPT Chunking:"))
        stepPanel.add(chunkingCheckBox)
        stepPanel.add(JLabel("Max Chunk Size:"))
        stepPanel.add(chunkSizeField)
        stepPanel.add(skipAdvancedCheckBox)
    }

    private fun buildAdvancedStepOrSkip() {
        if (skipAdvancedCheckBox.isSelected) {
            core.skipAdvanced = true
            core.goNextStep()
            updateStep()
            return
        }
        stepPanel.add(directorySummaryCheckBox)
        stepPanel.add(removeCommentsCheckBox)
        stepPanel.add(removeLeadingBlankLinesCheckBox)
        stepPanel.add(useGitIgnoreCheckBox)
    }

    private fun buildReviewStep() {
        stepPanel.add(JLabel("All set! Click OK to save your preferences."))
    }

    /**
     * Transfers current UI field values into the core.
     */
    private fun applyUiToCore() {
        core.includeLineNumbers = lineNumbersCheckBox.isSelected
        core.enableChunkingForGPT = chunkingCheckBox.isSelected
        core.chunkSize = chunkSizeField.text.toIntOrNull() ?: core.chunkSize
        core.skipAdvanced = skipAdvancedCheckBox.isSelected
        core.includeDirectorySummary = directorySummaryCheckBox.isSelected
        core.removeComments = removeCommentsCheckBox.isSelected
        core.removeLeadingBlankLines = removeLeadingBlankLinesCheckBox.isSelected
        core.useGitIgnore = useGitIgnoreCheckBox.isSelected
    }

    /**
     * Should be called from the "Next" button.
     */
    fun doNextAction() {
        applyUiToCore()
        if (core.getCurrentStep() < core.totalSteps - 1) {
            core.goNextStep()
            updateStep()
        } else {
            doOKAction()
        }
    }

    /**
     * Returns the final configured options by applying UI values to the core.
     */
    fun getConfiguredOptions(): ClipCraftOptions {
        applyUiToCore()
        val finalOpts = core.getFinalOptions()
        log.info("Final wizard options: $finalOpts")
        return finalOpts
    }
}
