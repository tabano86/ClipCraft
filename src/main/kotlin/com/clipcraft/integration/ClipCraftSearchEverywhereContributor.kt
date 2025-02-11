package com.clipcraft.integration

import com.clipcraft.services.ClipCraftProjectProfileManager
import com.intellij.ide.actions.SearchEverywherePsiRenderer
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.Processor
import javax.swing.ListCellRenderer

/**
 * Search Everywhere contributor for ClipCraft.
 *
 * This class uses a no-argument constructor. The project is set later
 * via [setProject]. This satisfies the requirement that extensions must
 * have a no-arg constructor.
 */
class ClipCraftSearchEverywhereContributor : SearchEverywhereContributor<Any> {

    private var project: Project? = null

    /**
     * Injects the project instance for this contributor.
     */
    fun setProject(project: Project?) {
        this.project = project
    }

    override fun getSearchProviderId(): String = "ClipCraftSearch"

    override fun getGroupName(): String = "ClipCraft"

    override fun getSortWeight(): Int = 50

    override fun showInFindResults(): Boolean = false

    override fun fetchElements(
        pattern: String,
        progressIndicator: ProgressIndicator,
        consumer: Processor<in Any>
    ) {
        val proj = project ?: return
        val manager = proj.getService(ClipCraftProjectProfileManager::class.java)
        val matches = manager?.getProfiles()?.filter {
            StringUtil.containsIgnoreCase(it.profileName, pattern)
        } ?: emptyList()
        for (profile in matches) {
            if (!consumer.process(profile)) return
        }
    }

    override fun processSelectedItem(selected: Any, modifiers: Int, searchText: String): Boolean = true

    override fun getElementPriority(element: Any, pattern: String): Int = 0

    override fun getElementsRenderer(): ListCellRenderer<Any> =
        SearchEverywherePsiRenderer(DisposableHolder.disposable)

    override fun getDataForItem(element: Any, dataId: String): Any? = null
}

object DisposableHolder {
    val disposable = com.intellij.openapi.util.Disposer.newDisposable("ClipCraftContributorDisposable")
}
