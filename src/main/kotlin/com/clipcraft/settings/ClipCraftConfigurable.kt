package com.clipcraft.settings

import com.clipcraft.model.SettingsState
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import javax.swing.JComponent

class ClipCraftConfigurable : Configurable {

    private var rootPanel: DialogPanel? = null
    private var uiState: SettingsState? = null

    override fun getDisplayName(): String = "ClipCraft Settings"

    override fun createComponent(): JComponent {
        val persistentState = SettingsStateProvider.getInstance().state
        val newUiState = persistentState.copy()
        this.uiState = newUiState

        val newPanel = panel {
            group("File Filtering Globs") {
                row { label("Include patterns (one per line):") }
                row {
                    textArea()
                        .bindText(newUiState::includeGlobs)
                        .rows(10)
                        .resizableColumn()
                }.comment("Uses glob syntax, e.g., 'src/**/*.java'.")

                row { label("Exclude patterns (one per line):") }
                row {
                    textArea()
                        .bindText(newUiState::excludeGlobs)
                        .rows(6)
                        .resizableColumn()
                }
            }
            group("Content Reading") {
                row("Max text file size (in KB):") {
                    intTextField(0..50000)
                        .bindIntText(newUiState::maxFileSizeKb)
                }.comment("Files larger than this are listed by name only.", 100)
            }
        }
        this.rootPanel = newPanel
        return newPanel
    }

    override fun isModified(): Boolean {
        return rootPanel?.isModified() ?: false
    }

    override fun apply() {
        rootPanel?.apply()
        uiState?.let { SettingsStateProvider.getInstance().loadState(it) }
    }

    override fun reset() {
        rootPanel?.reset()
    }

    override fun disposeUIResources() {
        rootPanel = null
        uiState = null
    }
}