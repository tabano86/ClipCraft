package com.clipcraft.settings

import com.clipcraft.model.ChunkStrategy
import com.clipcraft.model.OutputFormat
import com.clipcraft.model.SettingsState
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.rows
import com.intellij.ui.dsl.builder.bindIntText
import javax.swing.JComponent

class EnhancedClipCraftConfigurable : Configurable {

    private var rootPanel: DialogPanel? = null
    private var uiState: SettingsState? = null

    override fun getDisplayName(): String = "ClipCraft Settings"

    override fun createComponent(): JComponent {
        val persistentState = SettingsStateProvider.getInstance().state
        val newUiState = persistentState.copy()
        this.uiState = newUiState

        val newPanel = panel {
            group("File Filtering") {
                row { label("Include patterns (one per line):") }
                row {
                    textArea()
                        .bindText(newUiState::includeGlobs)
                        .rows(8)
                        .resizableColumn()
                }.comment("Uses glob syntax, e.g., 'src/**/*.java'")

                row { label("Exclude patterns (one per line):") }
                row {
                    textArea()
                        .bindText(newUiState::excludeGlobs)
                        .rows(6)
                        .resizableColumn()
                }

                row("Max file size (KB):") {
                    intTextField(0..50000)
                        .bindIntText(newUiState::maxFileSizeKb)
                }.comment("Files larger than this are skipped")
            }

            group("Output Format") {
                row("Default format:") {
                    comboBox(OutputFormat.values().toList())
                        .bindItem(
                            { try { OutputFormat.valueOf(newUiState.defaultOutputFormat) } catch (e: Exception) { OutputFormat.MARKDOWN } },
                            { newUiState.defaultOutputFormat = it?.name ?: "MARKDOWN" }
                        )
                }

                row {
                    checkBox("Include line numbers")
                        .bindSelected(newUiState::includeLineNumbers)
                }

                row {
                    checkBox("Strip comments from code")
                        .bindSelected(newUiState::stripComments)
                }

                row {
                    checkBox("Include metadata in output")
                        .bindSelected(newUiState::includeMetadata)
                }

                row {
                    checkBox("Include Git information")
                        .bindSelected(newUiState::includeGitInfo)
                }

                row {
                    checkBox("Include table of contents")
                        .bindSelected(newUiState::includeTableOfContents)
                }

                row {
                    checkBox("Group files by directory")
                        .bindSelected(newUiState::groupByDirectory)
                }
            }

            group("Security & Privacy") {
                row {
                    checkBox("Detect secrets and sensitive data")
                        .bindSelected(newUiState::detectSecrets)
                }

                row {
                    checkBox("Automatically mask detected secrets")
                        .bindSelected(newUiState::maskSecrets)
                }.comment("Replaces sensitive values with asterisks")

                row {
                    checkBox("Respect .gitignore files")
                        .bindSelected(newUiState::respectGitignore)
                }
            }

            group("Chunking (for large exports)") {
                row {
                    checkBox("Enable automatic chunking")
                        .bindSelected(newUiState::enableChunking)
                }.comment("Split large exports into manageable chunks")

                row("Max tokens per chunk:") {
                    intTextField(1000..1000000)
                        .bindIntText(newUiState::maxTokens)
                }.comment("Estimated token limit before splitting")

                row("Chunking strategy:") {
                    comboBox(ChunkStrategy.values().toList())
                        .bindItem(
                            { try { ChunkStrategy.valueOf(newUiState.chunkStrategy) } catch (e: Exception) { ChunkStrategy.BY_SIZE } },
                            { newUiState.chunkStrategy = it?.name ?: "BY_SIZE" }
                        )
                }
            }

            group("Quick Tips") {
                row {
                    text(
                        """
                        <ul>
                        <li><b>Quick Export Current File:</b> Right-click any file → ClipCraft → Quick Export Current File</li>
                        <li><b>Quick Export Project:</b> Tools → ClipCraft → Quick Export Project</li>
                        <li><b>Export with Presets:</b> Select files → ClipCraft → Export with Preset</li>
                        <li><b>Export to File:</b> Select files → ClipCraft → Export to File</li>
                        </ul>
                        """.trimIndent()
                    )
                }
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
