package com.clipcraft.integration

import com.clipcraft.model.Snippet
import com.intellij.openapi.project.Project

object ClipCraftGitIntegration {

    /**
     * Fills snippet's Git commit info using IntelliJ's built-in Git APIs, if available.
     * This is a mock example that just sets stub values.
     */
    fun enrichSnippetWithGitInfo(project: Project, snippet: Snippet): Snippet {
        // Real usage: GitRepositoryManager, GitHistoryUtils, etc.
        val commitHash = "abc1234" // mock
        val authorName = "Jane Developer"
        return snippet.copy(gitCommitHash = commitHash, gitAuthor = authorName)
    }
}
