package com.clipcraft.ui

import com.clipcraft.model.ClipCraftOptions
import com.intellij.openapi.ui.DialogWrapper
import org.slf4j.LoggerFactory
import java.awt.GraphicsEnvironment
import javax.swing.*

/**
 * ClipCraft Setup Wizard.
 *
 * This wizard guides the user through configuration steps.
 * For production use, run with full UI (headless = false).
 * In headless environments (e.g. during tests) pass headless = true
 * to avoid AWT/IDE initialization errors.
 */
class ClipCraftSetupWizard(
    private val initialOptions: ClipCraftOptions,
    private val headless: Boolean = GraphicsEnvironment.isHeadless()
) : DialogWrapper(null, true) {

    private val log = LoggerFactory.getLogger(ClipCraftSetupWizard::class.java)
    private val stepPanel = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }
    private var currentStep = 0

    private val stepLabels = listOf(
        JLabel("Welcome to ClipCraft! Let’s set up your preferences."),
        JLabel("Advanced Options"),
        JLabel("Review & Finish")
    )

    // Step 1 controls
    val lineNumbersCheckBox = JCheckBox("Include Line Numbers", initialOptions.includeLineNumbers)
    val chunkingCheckBox = JCheckBox("Enable Chunking for GPT", initialOptions.enableChunkingForGPT)
    val chunkSizeField = JTextField(initialOptions.maxChunkSize.toString(), 5)
    val skipAdvancedCheckBox = JCheckBox("Skip Advanced Step If Unneeded", false)

    // Step 2 controls
    val directorySummaryCheckBox = JCheckBox("Include Directory Summary", initialOptions.includeDirectorySummary)
    val removeCommentsCheckBox = JCheckBox("Remove Comments", initialOptions.removeComments)
    val removeLeadingBlankLinesCheckBox = JCheckBox("Remove Leading Blank Lines", initialOptions.removeLeadingBlankLines)
    val useGitIgnoreCheckBox = JCheckBox("Use .gitignore", initialOptions.useGitIgnore)

    init {
        title = "ClipCraft Setup Wizard"
        if (!headless) {
            init()
            updateStep()
        } else {
            log.warn("Headless mode enabled; skipping UI peer initialization.")
        }
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
