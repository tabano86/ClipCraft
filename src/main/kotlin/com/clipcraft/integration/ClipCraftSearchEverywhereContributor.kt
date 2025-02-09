package com.clipcraft.integration

import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.Processor
import javax.swing.JLabel
import javax.swing.ListCellRenderer

class ClipCraftSearchEverywhereContributor : SearchEverywhereContributor<String> {
    override fun getSearchProviderId(): String = "ClipCraftSearch"
    override fun getGroupName(): String = "ClipCraft"
    override fun getSortWeight(): Int = 1000
    override fun showInFindResults(): Boolean = false
    override fun getDataForItem(element: String, dataId: String): Any? = null

    override fun fetchElements(pattern: String, progressIndicator: ProgressIndicator, consumer: Processor<in String>) {
        // Could list macros, profile names, etc.  For now, do nothing.
    }

    override fun processSelectedItem(selected: String, modifiers: Int, searchText: String): Boolean = true

    override fun getElementsRenderer(): ListCellRenderer<in String> {
        return ListCellRenderer { _, value, _, _, _ ->
            JLabel(value ?: "")
        }
    }
}
