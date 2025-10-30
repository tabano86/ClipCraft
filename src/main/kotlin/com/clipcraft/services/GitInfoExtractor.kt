package com.clipcraft.services

import com.intellij.openapi.project.Project
import java.io.File

object GitInfoExtractor {

    data class GitInfo(
        val branch: String?,
        val commit: String?,
        val commitShort: String?,
        val author: String?,
        val commitMessage: String?,
        val isDirty: Boolean
    )

    fun extractGitInfo(project: Project): GitInfo? {
        val basePath = project.basePath ?: return null
        val gitDir = File(basePath, ".git")

        if (!gitDir.exists() || !gitDir.isDirectory) {
            return null
        }

        return try {
            GitInfo(
                branch = getCurrentBranch(gitDir),
                commit = getCurrentCommit(gitDir),
                commitShort = getCurrentCommit(gitDir)?.take(7),
                author = getLastCommitAuthor(basePath),
                commitMessage = getLastCommitMessage(basePath),
                isDirty = isWorkingTreeDirty(basePath)
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun getCurrentBranch(gitDir: File): String? {
        val headFile = File(gitDir, "HEAD")
        if (!headFile.exists()) return null

        val headContent = headFile.readText().trim()
        return if (headContent.startsWith("ref: refs/heads/")) {
            headContent.removePrefix("ref: refs/heads/")
        } else {
            "detached HEAD"
        }
    }

    private fun getCurrentCommit(gitDir: File): String? {
        val headFile = File(gitDir, "HEAD")
        if (!headFile.exists()) return null

        val headContent = headFile.readText().trim()

        return if (headContent.startsWith("ref: ")) {
            val refPath = headContent.removePrefix("ref: ")
            val refFile = File(gitDir, refPath)
            if (refFile.exists()) {
                refFile.readText().trim()
            } else {
                // Check packed-refs
                val packedRefs = File(gitDir, "packed-refs")
                if (packedRefs.exists()) {
                    packedRefs.readLines().find { it.endsWith(refPath) }?.split(" ")?.firstOrNull()
                } else {
                    null
                }
            }
        } else {
            headContent
        }
    }

    private fun getLastCommitAuthor(basePath: String): String? {
        return try {
            val process = ProcessBuilder("git", "log", "-1", "--pretty=format:%an")
                .directory(File(basePath))
                .start()

            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()

            output.ifEmpty { null }
        } catch (e: Exception) {
            null
        }
    }

    private fun getLastCommitMessage(basePath: String): String? {
        return try {
            val process = ProcessBuilder("git", "log", "-1", "--pretty=format:%s")
                .directory(File(basePath))
                .start()

            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()

            output.ifEmpty { null }
        } catch (e: Exception) {
            null
        }
    }

    private fun isWorkingTreeDirty(basePath: String): Boolean {
        return try {
            val process = ProcessBuilder("git", "status", "--porcelain")
                .directory(File(basePath))
                .start()

            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            output.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    fun formatGitInfo(gitInfo: GitInfo): String {
        val sb = StringBuilder()
        gitInfo.branch?.let { sb.append("Branch: $it") }
        gitInfo.commitShort?.let {
            if (sb.isNotEmpty()) sb.append(" | ")
            sb.append("Commit: $it")
        }
        if (gitInfo.isDirty) {
            sb.append(" (dirty)")
        }
        return sb.toString()
    }
}
