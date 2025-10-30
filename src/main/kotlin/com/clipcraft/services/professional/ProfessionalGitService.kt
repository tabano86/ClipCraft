package com.clipcraft.services.professional

import com.intellij.openapi.project.Project
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File

/**
 * Professional Git integration using JGit (Eclipse's battle-tested git library).
 * Don't parse .git manually - use the library everyone uses.
 */
object ProfessionalGitService {

    data class GitInfo(
        val branch: String,
        val commitHash: String,
        val commitHashShort: String,
        val authorName: String,
        val authorEmail: String,
        val commitMessage: String,
        val commitDate: String,
        val isDirty: Boolean,
        val remoteUrl: String?,
        val tags: List<String>,
    )

    /**
     * Get comprehensive git info using JGit.
     */
    fun getGitInfo(project: Project): GitInfo? {
        val basePath = project.basePath ?: return null
        val gitDir = File(basePath, ".git")

        if (!gitDir.exists()) return null

        return try {
            val repository = FileRepositoryBuilder()
                .setGitDir(gitDir)
                .readEnvironment()
                .findGitDir()
                .build()

            Git(repository).use { git ->
                val head = repository.resolve("HEAD") ?: return null
                val revWalk = RevWalk(repository)
                val commit = revWalk.parseCommit(head)

                val branch = repository.branch ?: "detached HEAD"
                val commitHash = commit.name
                val commitHashShort = commit.name.substring(0, 7)
                val authorName = commit.authorIdent.name
                val authorEmail = commit.authorIdent.emailAddress
                val commitMessage = commit.fullMessage
                val commitDate = commit.authorIdent.`when`.toString()

                val status = git.status().call()
                val isDirty = !status.isClean

                val remoteUrl = try {
                    repository.config.getString("remote", "origin", "url")
                } catch (e: Exception) {
                    null
                }

                val tags = git.tagList().call().map { it.name.removePrefix("refs/tags/") }

                revWalk.dispose()

                GitInfo(
                    branch = branch,
                    commitHash = commitHash,
                    commitHashShort = commitHashShort,
                    authorName = authorName,
                    authorEmail = authorEmail,
                    commitMessage = commitMessage,
                    commitDate = commitDate,
                    isDirty = isDirty,
                    remoteUrl = remoteUrl,
                    tags = tags,
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if a file should be ignored by .gitignore.
     */
    fun isIgnored(repository: Repository, filePath: String): Boolean {
        return try {
            Git(repository).use { git ->
                val status = git.status().addPath(filePath).call()
                status.ignoredNotInIndex.contains(filePath)
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get file's git blame information.
     */
    fun getBlameInfo(repository: Repository, filePath: String): Map<Int, String> {
        return try {
            Git(repository).use { git ->
                val blameResult = git.blame().setFilePath(filePath).call()
                val blameMap = mutableMapOf<Int, String>()

                for (i in 0 until blameResult.resultContents.size()) {
                    val commit = blameResult.getSourceCommit(i)
                    if (commit != null) {
                        blameMap[i + 1] = "${commit.authorIdent.name} (${commit.name.substring(0, 7)})"
                    }
                }

                blameMap
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * Get list of changed files in working directory.
     */
    fun getModifiedFiles(repository: Repository): Set<String> {
        return try {
            Git(repository).use { git ->
                val status = git.status().call()
                (status.modified + status.added + status.changed + status.untracked).toSet()
            }
        } catch (e: Exception) {
            emptySet()
        }
    }

    /**
     * Format git info for display.
     */
    fun formatGitInfo(gitInfo: GitInfo): String {
        return buildString {
            append("${gitInfo.branch}@${gitInfo.commitHashShort}")
            if (gitInfo.isDirty) append(" (dirty)")
        }
    }

    /**
     * Get detailed git report.
     */
    fun getDetailedReport(gitInfo: GitInfo): String {
        return buildString {
            appendLine("Branch: ${gitInfo.branch}")
            appendLine("Commit: ${gitInfo.commitHashShort} (${gitInfo.commitHash})")
            appendLine("Author: ${gitInfo.authorName} <${gitInfo.authorEmail}>")
            appendLine("Date: ${gitInfo.commitDate}")
            appendLine("Message: ${gitInfo.commitMessage.lines().first()}")
            if (gitInfo.isDirty) appendLine("Status: Working directory has uncommitted changes")
            gitInfo.remoteUrl?.let { appendLine("Remote: $it") }
            if (gitInfo.tags.isNotEmpty()) {
                appendLine("Tags: ${gitInfo.tags.joinToString(", ")}")
            }
        }
    }
}
