package com.clipcraft.integration

import com.clipcraft.model.Snippet
import com.intellij.openapi.project.Project

object ClipCraftGitIntegration {

    /**
     * Fills snippet's Git commit info using IntelliJ's built-in Git APIs, if available.
     */
    fun enrichSnippetWithGitInfo(project: Project, snippet: Snippet): Snippet {
        // IntelliJ's Git APIs can retrieve commit hash, author, etc.
        // This is a simplified example, actual usage requires using
        // GitRepositoryManager or GitHistoryUtils, etc.

        val commitHash = "abc1234" // mock
        return snippet.copy(gitCommitHash = commitHash, gitAuthor = "Jane Developer")
    }
}
