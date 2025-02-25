package com.clipcraft.services

import com.clipcraft.lint.LintIssue
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.util.concurrent.CopyOnWriteArrayList

@Service(Service.Level.PROJECT)
class LintResultsService(project: Project) {
    private val lintIssues = CopyOnWriteArrayList<LintIssue>()
    fun storeResults(results: List<LintIssue>) {
        lintIssues.clear()
        lintIssues.addAll(results)
    }
    fun getAllResults(): List<LintIssue> = lintIssues.toList()
}
