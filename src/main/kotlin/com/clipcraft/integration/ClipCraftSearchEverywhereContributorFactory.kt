package com.clipcraft.integration

import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributor
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributorFactory
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.ProjectManager

/**
 * Factory for creating the ClipCraft search contributor.
 * This contributor lets users search through ClipCraft profiles.
 *
 * NOTE: Register this factory in plugin.xml via the
 * "com.intellij.searchEverywhereContributorFactory" extension point.
 */
class ClipCraftSearchEverywhereContributorFactory : SearchEverywhereContributorFactory<Any> {
    override fun createContributor(initEvent: AnActionEvent): SearchEverywhereContributor<Any> {
        val contributor = ClipCraftSearchEverywhereContributor()
        val project = initEvent.project ?: ProjectManager.getInstance().openProjects.firstOrNull()
        contributor.setProject(project)
        return contributor
    }
}
