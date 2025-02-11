package com.clipcraft.integration

import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributor
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributorFactory
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.ProjectManager

/**
 * Factory for creating instances of [ClipCraftSearchEverywhereContributor].
 *
 * The factory obtains the project from the provided event (or falls back to the first open project)
 * and sets it on the contributor instance.
 */
class ClipCraftSearchEverywhereContributorFactory : SearchEverywhereContributorFactory<Any> {
    override fun createContributor(initEvent: AnActionEvent): SearchEverywhereContributor<Any> {
        val contributor = ClipCraftSearchEverywhereContributor()
        // Get the project from the event; if null, use the first open project as fallback.
        val project = initEvent.project ?: ProjectManager.getInstance().openProjects.firstOrNull()
        contributor.setProject(project)
        return contributor
    }
}
