package com.clipcraft.ui

import com.clipcraft.model.ClipCraftOptions
import org.slf4j.LoggerFactory

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
        if (currentStepIndex == 0 && skipAdvanced) {
            currentStepIndex = 2
        } else if (currentStepIndex < totalSteps - 1) {
            currentStepIndex++
        } else {
            log.info("Already at final step.")
        }
    }

    fun getFinalOptions(): ClipCraftOptions {
        log.info("Final chunk size: $chunkSize")
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
