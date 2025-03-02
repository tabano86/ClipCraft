package com.clipcraft.integration

import com.clipcraft.model.Snippet
import com.intellij.openapi.project.Project

object ClipCraftGitIntegration {
    fun enrichSnippetWithGitInfo(project: Project, snippet: Snippet): Snippet {
        val repoManager = ClipCraftGitRepositoryManager.getInstance(project)
        val commitHash = repoManager.repositories.firstOrNull()?.currentRevision
        return if (commitHash != null) {
            val info = "\n[Git Commit Hash: $commitHash]"
            snippet.copy(content = snippet.content + info, gitCommitHash = commitHash)
        } else {
            snippet
        }
    }
}
