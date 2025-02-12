package com.clipcraft.integration

import com.clipcraft.model.Snippet
import com.intellij.openapi.project.Project
import git4idea.repo.GitRepositoryManager

object ClipCraftGitIntegration {
    fun enrichSnippetWithGitInfo(project: Project, snippet: Snippet): Snippet {
        // Retrieve the GitRepositoryManager instance for the project.
        val repositoryManager = GitRepositoryManager.getInstance(project)
        val repositories = repositoryManager.repositories

        // Attempt to retrieve the current revision (commit hash) from the first repository.
        val commitHash = repositories.firstOrNull()?.currentRevision

        return if (commitHash != null) {
            val gitInfo = "\n[Git Commit Hash: $commitHash]"
            snippet.copy(content = snippet.content + gitInfo, gitCommitHash = commitHash)
        } else {
            // If no Git repository is found or commit hash is unavailable, return the snippet unchanged.
            snippet
        }
    }
}