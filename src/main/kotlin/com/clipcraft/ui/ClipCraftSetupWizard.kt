package com.clipcraft.ui

import com.clipcraft.model.ClipCraftOptions
import com.intellij.openapi.ui.DialogWrapper
import org.slf4j.LoggerFactory
import javax.swing.*

/**
 * ClipCraft Setup Wizard with improved structure and pure utility methods for steps.
 */
class ClipCraftSetupWizard(private val initialOptions: ClipCraftOptions) : DialogWrapper(true) {

    private val log = LoggerFactory.getLogger(ClipCraftSetupWizard::class.java)

    private val stepPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }
    private var currentStep = 0

    private val stepLabels = listOf(
        JLabel("Welcome to ClipCraft! Let’s set up your preferences."),
        JLabel("Advanced Options"),
        JLabel("Review & Finish")
    )

    // Step 1 controls
    private val lineNumbersCheckBox = JCheckBox("Include Line Numbers", initialOptions.includeLineNumbers)
    private val chunkingCheckBox = JCheckBox("Enable Chunking for GPT", initialOptions.enableChunkingForGPT)
    private val chunkSizeField = JTextField(initialOptions.maxChunkSize.toString(), 5)
    private val skipAdvancedCheckBox = JCheckBox("Skip Advanced Step If Unneeded", false)

    // Step 2 controls
    private val directorySummaryCheckBox = JCheckBox("Include Directory Summary", initialOptions.includeDirectorySummary)
    private val removeCommentsCheckBox = JCheckBox("Remove Comments", initialOptions.removeComments)
    private val removeLeadingBlankLinesCheckBox = JCheckBox("Remove Leading Blank Lines", initialOptions.removeLeadingBlankLines)
    private val useGitIgnoreCheckBox = JCheckBox("Use .gitignore", initialOptions.useGitIgnore)

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
            currentStep = 2
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

    fun doNextAction() {
        if (currentStep < stepLabels.size - 1) {
            currentStep++
            updateStep()
        } else {
            doOKAction()
        }
    }

    fun getConfiguredOptions(): ClipCraftOptions {
        val chunkSize = chunkSizeField.text.toIntOrNull() ?: initialOptions.maxChunkSize
        // Logging for observability
        log.info("Wizard final chunk size input: $chunkSize")
        return initialOptions.copy(
            includeLineNumbers = lineNumbersCheckBox.isSelected,
            enableChunkingForGPT = chunkingCheckBox.isSelected,
            maxChunkSize = if (chunkSize <= 0) 3000 else chunkSize,
            includeDirectorySummary = directorySummaryCheckBox.isSelected,
            removeComments = removeCommentsCheckBox.isSelected,
            removeLeadingBlankLines = removeLeadingBlankLinesCheckBox.isSelected,
            useGitIgnore = useGitIgnoreCheckBox.isSelected
        )
    }
}
