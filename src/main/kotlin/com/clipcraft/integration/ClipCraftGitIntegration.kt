package com.clipcraft.integration

import com.intellij.openapi.project.Project
import git4idea.repo.GitRepositoryManager
import org.slf4j.LoggerFactory

object ClipCraftGitIntegration {
    private val log = LoggerFactory.getLogger(ClipCraftGitIntegration::class.java)

    fun getGitMetadata(project: Project, filePath: String): String {
        val repoManager = GitRepositoryManager.getInstance(project)
        val repo = repoManager.repositories.firstOrNull { filePath.startsWith(it.root.path) }
        if (repo == null) {
            log.debug("No Git repo found for path $filePath")
            return ""
        }
        val currentBranch = repo.currentBranchName ?: "unknown-branch"
        val lastCommitHash = repo.currentRevision ?: "unknown-hash"
        return "[Branch: $currentBranch, Commit: $lastCommitHash]"
    }
}
