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
 * A simple wizard that walks the user through basic setup steps.
 * Now extended with more settings.
 */
class ClipCraftSetupWizard(private val initialOptions: ClipCraftOptions) : DialogWrapper(true) {

    private val stepPanel = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }
    private var currentStep = 0
    private val stepLabels = listOf(
        JLabel("Welcome to ClipCraft! Let’s set up your preferences."),
        JLabel("Advanced Options"),
        JLabel("Review and Finish")
    )

    // Basic step controls
    private val lineNumbersCheckBox = JCheckBox("Include Line Numbers", initialOptions.includeLineNumbers)
    private val chunkingCheckBox = JCheckBox("Enable Chunking for GPT", initialOptions.enableChunkingForGPT)
    private val chunkSizeField = JTextField(initialOptions.maxChunkSize.toString(), 5)

    // More advanced step controls
    private val directorySummaryCheckBox = JCheckBox("Include Directory Summary", initialOptions.includeDirectorySummary)
    private val removeCommentsCheckBox = JCheckBox("Remove Comments", initialOptions.removeComments)
    private val removeLeadingBlankLinesCheckBox = JCheckBox("Remove Leading Blank Lines", initialOptions.removeLeadingBlankLines)

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
            }
            1 -> {
                stepPanel.add(directorySummaryCheckBox)
                stepPanel.add(removeCommentsCheckBox)
                stepPanel.add(removeLeadingBlankLinesCheckBox)
            }
            2 -> stepPanel.add(JLabel("All set! Click OK to save your preferences."))
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
        return initialOptions.copy(
            includeLineNumbers = lineNumbersCheckBox.isSelected,
            enableChunkingForGPT = chunkingCheckBox.isSelected,
            maxChunkSize = chunkSizeField.text.toIntOrNull() ?: initialOptions.maxChunkSize,
            includeDirectorySummary = directorySummaryCheckBox.isSelected,
            removeComments = removeCommentsCheckBox.isSelected,
            removeLeadingBlankLines = removeLeadingBlankLinesCheckBox.isSelected
        )
    }
}
