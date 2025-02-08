package com.clipcraft.integration

import com.intellij.openapi.project.Project
import git4idea.repo.GitRepositoryManager

object ClipCraftGitIntegration {

    /**
     * Returns a string with [Branch: branchName, Commit: revisionHash]
     * if the file is under a Git repository. Otherwise returns empty string.
     */
    fun getGitMetadata(project: Project, filePath: String): String {
        val repoManager = GitRepositoryManager.getInstance(project)
        val repo = repoManager.repositories.firstOrNull { filePath.startsWith(it.root.path) } ?: return ""
        val currentBranch = repo.currentBranchName ?: "unknown-branch"
        val lastCommitHash = repo.currentRevision ?: "unknown-hash"
        return "[Branch: $currentBranch, Commit: $lastCommitHash]"
    }
}
