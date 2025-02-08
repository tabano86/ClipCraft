package com.clipcraft.integration

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.vcs.log.Hash
import git4idea.repo.GitRepositoryManager
import git4idea.repo.GitRepository

object ClipCraftGitIntegration {
    fun getGitMetadata(project: Project, filePath: String): String {
        val repoManager = GitRepositoryManager.getInstance(project)
        val repo = repoManager.repositories.firstOrNull { filePath.startsWith(it.root.path) }
            ?: return ""

        // Example: returning the current branch & last commit hash
        val currentBranch = repo.currentBranchName ?: "unknown-branch"
        val lastCommitHash = getLastCommitHash(repo) ?: "unknown-hash"
        return "[Branch: $currentBranch, Commit: $lastCommitHash]"
    }

    private fun getLastCommitHash(repo: GitRepository): String? {
        val commits = repo.info.additionalInfo.commits // depends on how you fetch commits
        return commits.firstOrNull()?.id?.asString() // or use git4idea's log APIs
    }
}
