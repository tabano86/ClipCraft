package com.clipcraft.services

import com.clipcraft.lint.LintIssue
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Holds the last run's lint results so we can display them in the lint tool window.
 */
@Service(Service.Level.PROJECT)
class LintResultsService(val project: Project) {
    private val lintIssues = CopyOnWriteArrayList<LintIssue>()

    fun storeResults(results: List<LintIssue>) {
        lintIssues.clear()
        lintIssues.addAll(results)
    }

    fun getAllResults(): List<LintIssue> = lintIssues.toList()
}
