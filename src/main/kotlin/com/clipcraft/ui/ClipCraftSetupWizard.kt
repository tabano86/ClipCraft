package com.clipcraft.ui

import com.clipcraft.model.ClipCraftOptions
import com.intellij.openapi.ui.DialogWrapper
import javax.swing.BoxLayout
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JComponent
import javax.swing.JTextField

/**
 * A simple wizard that walks the user through basic setup steps,
 * now with .gitignore usage, compression, etc.
 */
class ClipCraftSetupWizard(private val initialOptions: ClipCraftOptions) : DialogWrapper(true) {

    private val stepPanel = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }
    private var currentStep = 0
    private val stepLabels = listOf(
        JLabel("Welcome to ClipCraft! Let’s set up your preferences."),
        JLabel("Advanced Options"),
        JLabel("Review and Finish")
    )

    // Step 1
    private val lineNumbersCheckBox = JCheckBox("Include Line Numbers", initialOptions.includeLineNumbers)
    private val chunkingCheckBox = JCheckBox("Enable Chunking for GPT", initialOptions.enableChunkingForGPT)
    private val chunkSizeField = JTextField(initialOptions.maxChunkSize.toString(), 5)

    // Step 2
    private val directorySummaryCheckBox = JCheckBox("Include Directory Summary", initialOptions.includeDirectorySummary)
    private val removeCommentsCheckBox = JCheckBox("Remove Comments", initialOptions.removeComments)
    private val removeLeadingBlankLinesCheckBox = JCheckBox("Remove Leading Blank Lines", initialOptions.removeLeadingBlankLines)
    private val useGitIgnoreCheckBox = JCheckBox("Use .gitignore", initialOptions.useGitIgnore)

    // Possibly skip advanced
    private val skipAdvancedCheckBox = JCheckBox("Skip advanced step if unneeded", false)

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
                stepPanel.add(JLabel("GPT Chunking:"))
                stepPanel.add(chunkingCheckBox)
                stepPanel.add(JLabel("Max Chunk Size:"))
                stepPanel.add(chunkSizeField)
                stepPanel.add(skipAdvancedCheckBox)
            }
            1 -> {
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
            2 -> {
                stepPanel.add(JLabel("All set! Click OK to save your preferences."))
            }
        }

        stepPanel.revalidate()
        stepPanel.repaint()
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
