package com.clipcraft.ui

import com.clipcraft.model.ConcurrencyMode
import com.clipcraft.model.OutputTarget
import com.clipcraft.services.ClipCraftSettingsState
import com.clipcraft.util.CodeFormatter
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import java.awt.Dimension
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.Icon
import javax.swing.ImageIcon
import javax.swing.JComponent
import javax.swing.JTextArea

class ClipCraftSettingsConfigurable : SearchableConfigurable, Configurable.NoScroll {

    private val globalState = ClipCraftSettingsState.getInstance()
    private val advancedOptions = globalState.advancedOptions
    private var modified = false

    // For the preview area
    private val previewArea = JTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        text = "Preview will appear here..."
    }

    override fun getId(): String = "clipcraft.settings"
    override fun getDisplayName(): String = "ClipCraft"

    override fun createComponent(): JComponent {
        val formPanel = panel {
            group("Global Copy Settings") {
                row("Max Copy Characters:") {
                    textField().columns(10).applyToComponent {
                        text = globalState.maxCopyCharacters.toString()
                        addFocusListener(object : FocusAdapter() {
                            override fun focusLost(e: FocusEvent?) {
                                val newVal = text.toIntOrNull() ?: globalState.maxCopyCharacters
                                if (newVal != globalState.maxCopyCharacters) {
                                    globalState.maxCopyCharacters = newVal
                                    modified = true
                                    updatePreview()
                                }
                            }
                        })
                    }
                }
            }
            group("Concurrency") {
                row("Concurrency Mode:") {
                    comboBox(ConcurrencyMode.entries).applyToComponent {
                        selectedItem = advancedOptions.concurrencyMode
                        addActionListener {
                            val newVal = selectedItem as ConcurrencyMode
                            if (newVal != advancedOptions.concurrencyMode) {
                                advancedOptions.concurrencyMode = newVal
                                modified = true
                                updatePreview()
                            }
                        }
                    }
                }
                row("Max Concurrent Tasks:") {
                    textField().columns(4).applyToComponent {
                        text = advancedOptions.maxConcurrentTasks.toString()
                        addFocusListener(object : FocusAdapter() {
                            override fun focusLost(e: FocusEvent?) {
                                val newVal = text.toIntOrNull() ?: advancedOptions.maxConcurrentTasks
                                if (newVal != advancedOptions.maxConcurrentTasks) {
                                    advancedOptions.maxConcurrentTasks = newVal
                                    modified = true
                                    updatePreview()
                                }
                            }
                        })
                    }
                }
            }
            group("Metadata & Additional") {
                row {
                    checkBox("Include Metadata").applyToComponent {
                        isSelected = advancedOptions.includeMetadata
                        addActionListener {
                            advancedOptions.includeMetadata = isSelected
                            modified = true
                            updatePreview()
                        }
                    }
                }
                // Small label + icon for placeholders
                row("Metadata Template:") {
                    textField().columns(40).applyToComponent {
                        text = advancedOptions.metadataTemplate.orEmpty()
                        addFocusListener(object : FocusAdapter() {
                            override fun focusLost(e: FocusEvent?) {
                                val newVal = if (text.isBlank()) null else text
                                if (newVal != advancedOptions.metadataTemplate) {
                                    advancedOptions.metadataTemplate = newVal
                                    modified = true
                                    updatePreview()
                                }
                            }
                        })
                    }
                    // Provide a help icon or label with example placeholders
                    val helpIcon: Icon = ImageIcon(javaClass.getResource("/icons/help.svg")) // or any icon
                    label("").applyToComponent {
                        icon = helpIcon
                        toolTipText = """
                            Common placeholders:
                            {fileName}, {filePath}, {size}, {modified}, {id}, {relativePath}
                        """.trimIndent()
                    }
                }
                row("Snippet Header:") {
                    textField().columns(40).applyToComponent {
                        text = advancedOptions.snippetHeaderText.orEmpty()
                        addFocusListener(object : FocusAdapter() {
                            override fun focusLost(e: FocusEvent?) {
                                val newVal = if (text.isBlank()) null else text
                                if (newVal != advancedOptions.snippetHeaderText) {
                                    advancedOptions.snippetHeaderText = newVal
                                    modified = true
                                    updatePreview()
                                }
                            }
                        })
                    }
                }
                row("Snippet Footer:") {
                    textField().columns(40).applyToComponent {
                        text = advancedOptions.snippetFooterText.orEmpty()
                        addFocusListener(object : FocusAdapter() {
                            override fun focusLost(e: FocusEvent?) {
                                val newVal = if (text.isBlank()) null else text
                                if (newVal != advancedOptions.snippetFooterText) {
                                    advancedOptions.snippetFooterText = newVal
                                    modified = true
                                    updatePreview()
                                }
                            }
                        })
                    }
                }
                row {
                    checkBox("Respect .gitignore").applyToComponent {
                        isSelected = advancedOptions.useGitIgnore
                        addActionListener {
                            advancedOptions.useGitIgnore = isSelected
                            modified = true
                            updatePreview()
                        }
                    }.comment("If enabled, code from ignored files/folders won't be included.")
                }
                row {
                    checkBox("Hierarchical Directory Summary").applyToComponent {
                        isSelected = advancedOptions.hierarchicalDirectorySummary
                        addActionListener {
                            advancedOptions.hierarchicalDirectorySummary = isSelected
                            modified = true
                            updatePreview()
                        }
                    }
                }
                row {
                    checkBox("Include IDE Problems").applyToComponent {
                        isSelected = advancedOptions.includeIdeProblems
                        addActionListener {
                            advancedOptions.includeIdeProblems = isSelected
                            modified = true
                            updatePreview()
                        }
                    }.comment("If enabled, attempts to gather IntelliJ highlights/warnings.")
                }
            }
            group("Output Target & Preview") {
                row("Output Target:") {
                    comboBox(OutputTarget.values().toList()).applyToComponent {
                        selectedItem = advancedOptions.outputTarget
                        addActionListener {
                            val newVal = selectedItem as OutputTarget
                            if (newVal != advancedOptions.outputTarget) {
                                advancedOptions.outputTarget = newVal
                                modified = true
                                updatePreview()
                            }
                        }
                    }
                }
                row("Preview:") {
                    cell(
                        JBScrollPane(previewArea).apply {
                            preferredSize = Dimension(400, 160)
                        },
                    )
                }
            }
        }

        val scrollPane = JBScrollPane(formPanel)
        scrollPane.verticalScrollBar.unitIncrement = 16
        scrollPane.preferredSize = Dimension(700, 700)
        updatePreview() // initialize
        return scrollPane
    }

    override fun isModified(): Boolean = modified

    @Throws(ConfigurationException::class)
    override fun apply() {
        advancedOptions.resolveConflicts()
        modified = false
    }

    override fun reset() {
        modified = false
        updatePreview()
    }

    /**
     * Generate a quick example snippet to show how settings will look.
     */
    private fun updatePreview() {
        // Construct a small snippet
        val sampleSnippet = com.clipcraft.model.Snippet(
            filePath = "/home/user/projects/demo/src/Sample.kt",
            relativePath = "demo/src/Sample.kt",
            fileName = "Sample.kt",
            fileSizeBytes = 123L,
            lastModified = 1678999999999L,
            content = "fun helloWorld() {\n    println(\"Hello, world!\")\n}\n",
        )
        val sampleList = listOf(sampleSnippet)
        val resultBlocks = CodeFormatter.formatSnippets(sampleList, advancedOptions)
        previewArea.text = resultBlocks.joinToString("\n---\n")
    }
}
