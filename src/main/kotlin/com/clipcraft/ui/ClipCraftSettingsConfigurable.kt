package com.clipcraft.ui

import com.clipcraft.model.ChunkStrategy
import com.clipcraft.model.CompressionMode
import com.clipcraft.model.ConcurrencyMode
import com.clipcraft.model.OutputFormat
import com.clipcraft.model.OverlapStrategy
import com.clipcraft.services.ClipCraftSettingsState
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import java.awt.Dimension
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.JComponent

class ClipCraftSettingsConfigurable : SearchableConfigurable, Configurable.NoScroll {
    private val globalState = ClipCraftSettingsState.getInstance()
    private val advancedOptions = globalState.advancedOptions
    private var modified = false

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
                                }
                            }
                        })
                    }
                }
            }
            group("Chunking & Compression") {
                row("Chunk Strategy:") {
                    comboBox(ChunkStrategy.entries).applyToComponent {
                        selectedItem = advancedOptions.chunkStrategy
                        addActionListener {
                            val newVal = selectedItem as ChunkStrategy
                            if (newVal != advancedOptions.chunkStrategy) {
                                advancedOptions.chunkStrategy = newVal
                                modified = true
                            }
                        }
                    }
                }
                row("Chunk Size:") {
                    textField().columns(6).applyToComponent {
                        text = advancedOptions.chunkSize.toString()
                        addFocusListener(object : FocusAdapter() {
                            override fun focusLost(e: FocusEvent?) {
                                val newVal = text.toIntOrNull() ?: advancedOptions.chunkSize
                                if (newVal != advancedOptions.chunkSize) {
                                    advancedOptions.chunkSize = newVal
                                    modified = true
                                }
                            }
                        })
                    }
                }
                row("Overlap Strategy:") {
                    comboBox(OverlapStrategy.entries).applyToComponent {
                        selectedItem = advancedOptions.overlapStrategy
                        addActionListener {
                            val newVal = selectedItem as OverlapStrategy
                            if (newVal != advancedOptions.overlapStrategy) {
                                advancedOptions.overlapStrategy = newVal
                                modified = true
                            }
                        }
                    }
                }
                row("Compression Mode:") {
                    comboBox(CompressionMode.entries).applyToComponent {
                        selectedItem = advancedOptions.compressionMode
                        addActionListener {
                            val newVal = selectedItem as CompressionMode
                            if (newVal != advancedOptions.compressionMode) {
                                advancedOptions.compressionMode = newVal
                                modified = true
                            }
                        }
                    }
                }
                row {
                    checkBox("Selective Compression (ignore lines w/ TODO, DEBUG, etc.)").applyToComponent {
                        isSelected = advancedOptions.selectiveCompression
                        addActionListener {
                            advancedOptions.selectiveCompression = isSelected
                            modified = true
                        }
                    }
                }
            }
            group("Output Formatting") {
                row("Output Format:") {
                    comboBox(OutputFormat.entries).applyToComponent {
                        selectedItem = advancedOptions.outputFormat
                        addActionListener {
                            val newVal = selectedItem as OutputFormat
                            if (newVal != advancedOptions.outputFormat) {
                                advancedOptions.outputFormat = newVal
                                modified = true
                            }
                        }
                    }
                }
                row {
                    checkBox("Include Line Numbers").applyToComponent {
                        isSelected = advancedOptions.includeLineNumbers
                        addActionListener {
                            advancedOptions.includeLineNumbers = isSelected
                            modified = true
                        }
                    }
                }
                row {
                    checkBox("Remove Imports").applyToComponent {
                        isSelected = advancedOptions.removeImports
                        addActionListener {
                            advancedOptions.removeImports = isSelected
                            modified = true
                        }
                    }
                }
                row {
                    checkBox("Remove Comments").applyToComponent {
                        isSelected = advancedOptions.removeComments
                        addActionListener {
                            advancedOptions.removeComments = isSelected
                            modified = true
                        }
                    }
                }
                row {
                    checkBox("Trim Whitespace").applyToComponent {
                        isSelected = advancedOptions.trimWhitespace
                        addActionListener {
                            advancedOptions.trimWhitespace = isSelected
                            modified = true
                        }
                    }
                }
                row {
                    checkBox("Remove Empty Lines").applyToComponent {
                        isSelected = advancedOptions.removeEmptyLines
                        addActionListener {
                            advancedOptions.removeEmptyLines = isSelected
                            modified = true
                        }
                    }
                }
                row {
                    checkBox("Single-line Output").applyToComponent {
                        isSelected = advancedOptions.singleLineOutput
                        addActionListener {
                            advancedOptions.singleLineOutput = isSelected
                            modified = true
                        }
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
                        }
                    }
                }
                row("Metadata Template:") {
                    textField().columns(40).applyToComponent {
                        text = advancedOptions.metadataTemplate.orEmpty()
                        addFocusListener(object : FocusAdapter() {
                            override fun focusLost(e: FocusEvent?) {
                                val newVal = if (text.isBlank()) null else text
                                if (newVal != advancedOptions.metadataTemplate) {
                                    advancedOptions.metadataTemplate = newVal
                                    modified = true
                                }
                            }
                        })
                    }.comment("Placeholders: {fileName}, {size}, {modified}, etc.")
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
                        }
                    }.comment("If enabled, code from ignored files/folders won't be included.")
                }
            }
        }
        val scrollPane = JBScrollPane(formPanel)
        scrollPane.verticalScrollBar.unitIncrement = 16
        scrollPane.preferredSize = Dimension(600, 600)
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
    }
}
