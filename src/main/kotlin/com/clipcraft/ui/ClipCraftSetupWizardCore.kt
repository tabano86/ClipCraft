package com.clipcraft.ui

import com.clipcraft.model.ClipCraftOptions
import org.slf4j.LoggerFactory

/**
 * ClipCraftSetupWizardCore is a pure, non-UI class that holds
 * the wizard logic (steps, user choices, etc.). This class is ideal
 * for headless testing and can be used to drive the UI.
 */
class ClipCraftSetupWizardCore(private val initial: ClipCraftOptions) {

    private val log = LoggerFactory.getLogger(ClipCraftSetupWizardCore::class.java)
    private var currentStepIndex: Int = 0

    val totalSteps: Int = 3

    var skipAdvanced: Boolean = false

    // Step 1 options
    var includeLineNumbers: Boolean = initial.includeLineNumbers
    var enableChunkingForGPT: Boolean = initial.enableChunkingForGPT
    var chunkSize: Int = initial.maxChunkSize

    // Step 2 options
    var includeDirectorySummary: Boolean = initial.includeDirectorySummary
    var removeComments: Boolean = initial.removeComments
    var removeLeadingBlankLines: Boolean = initial.removeLeadingBlankLines
    var useGitIgnore: Boolean = initial.useGitIgnore

    fun getCurrentStep(): Int = currentStepIndex

    fun goNextStep() {
        // If user chose to skip advanced step, jump directly from step 0 to step 2.
        if (currentStepIndex == 0 && skipAdvanced) {
            currentStepIndex = 2
        } else if (currentStepIndex < totalSteps - 1) {
            currentStepIndex++
        } else {
            log.info("Already at final step; no further step change.")
        }
    }

    fun getFinalOptions(): ClipCraftOptions {
        log.info("Final chunk size input: $chunkSize")
        // Enforce default if the input chunk size is non-positive.
        val finalChunkSize = if (chunkSize <= 0) 3000 else chunkSize
        return initial.copy(
            includeLineNumbers = includeLineNumbers,
            enableChunkingForGPT = enableChunkingForGPT,
            maxChunkSize = finalChunkSize,
            includeDirectorySummary = includeDirectorySummary,
            removeComments = removeComments,
            removeLeadingBlankLines = removeLeadingBlankLines,
            useGitIgnore = useGitIgnore
        )
    }
}
