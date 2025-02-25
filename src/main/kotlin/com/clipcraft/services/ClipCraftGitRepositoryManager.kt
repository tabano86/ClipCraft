package com.clipcraft.services

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

data class GitRepository(val root: VirtualFile, val currentRevision: String?)

class ClipCraftGitRepositoryManager private constructor(private val project: Project) {
    private val logger = Logger.getInstance(ClipCraftGitRepositoryManager::class.java)
    val repositories: List<GitRepository> by lazy { discoverRepositories() }

    private fun discoverRepositories(): List<GitRepository> {
        val repos = mutableListOf<GitRepository>()
        val projectBaseDir = project.basePath?.let { projectBase ->
            com.intellij.openapi.vfs.LocalFileSystem.getInstance().findFileByPath(projectBase)
        }
        if (projectBaseDir == null) {
            logger.warn("Project base directory is null.")
            return repos
        }
        VfsUtilCore.iterateChildrenRecursively(
            projectBaseDir,
            { true },
            { file ->
                if (file.isDirectory && file.name == ".git") {
                    val gitRoot = file.parent
                    val commitHash = retrieveCurrentRevision(gitRoot)
                    repos.add(GitRepository(gitRoot, commitHash))
                    false
                } else {
                    true
                }
            },
        )
        if (repos.isEmpty()) {
            logger.warn("No Git repositories found in project.")
        }
        return repos
    }

    private fun retrieveCurrentRevision(root: VirtualFile): String? {
        return try {
            val process = ProcessBuilder("git", "rev-parse", "HEAD")
                .directory(File(root.path))
                .start()
            val output = process.inputStream.bufferedReader().readLine()?.trim()
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                logger.warn("Git command exited with code $exitCode for repository at ${root.path}")
                null
            } else {
                output
            }
        } catch (e: Exception) {
            logger.warn("Failed to retrieve current revision for repository at ${root.path}", e)
            null
        }
    }

    companion object {
        fun getInstance(project: Project): ClipCraftGitRepositoryManager = ClipCraftGitRepositoryManager(project)
    }
}
