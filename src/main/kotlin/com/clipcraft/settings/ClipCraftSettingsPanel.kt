package com.clipcraft.settings

import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.clipcraft.model.SettingsState
import javax.swing.JComponent
import javax.swing.text.JTextComponent

// The pure UI part of the settings, built with the Kotlin UI DSL.
class ClipCraftSettingsPanel(private val currentState: SettingsState) {

    // These hold the UI state. We'll check for modifications.
    val includeTextArea = JBTextArea(currentState.includeGlobs, 10, 80)
    val excludeTextArea = JBTextArea(currentState.excludeGlobs, 10, 80)

    // The root UI component
    val panel: JComponent = panel {
        group("File Filtering") {
            row {
                label("Include Glob Patterns (one per line):")
            }
            row {
                scrollCell(includeTextArea).bindText(currentState::includeGlobs)
            }
            row {
                label("Exclude Glob Patterns (one per line):")
            }
            row {
                scrollCell(excludeTextArea).bindText(currentState::excludeGlobs)
            }
        }
        group("Content Reading") {
            row("Max file size to read (in KB):") {
                intTextField(0..100000).bindIntText(currentState::maxFileSizeKb)
            }.comment("Files larger than this will be listed by name instead of content.")
        }
    }
}