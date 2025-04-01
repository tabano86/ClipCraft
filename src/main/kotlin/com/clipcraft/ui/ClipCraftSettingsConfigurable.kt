package com.clipcraft.ui

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.ConcurrencyMode
import com.clipcraft.model.OutputTarget
import com.clipcraft.model.Snippet
import com.clipcraft.services.ClipCraftSettingsService
import com.clipcraft.util.CodeFormatter
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.observable.util.whenTextChangedFromUi
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.whenItemSelectedFromUi
import com.intellij.util.ui.UIUtil
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.JComponent
import javax.swing.JOptionPane
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.ScrollPaneConstants
import javax.swing.event.DocumentEvent as SwingDocumentEvent
import javax.swing.event.DocumentListener as SwingDocumentListener

class ClipCraftSettingsConfigurable : SearchableConfigurable, Configurable.NoScroll {

    private val LOG = Logger.getInstance(ClipCraftSettingsConfigurable::class.java)

    // Persistent settings service
    private val settingsService = ClipCraftSettingsService.getInstance()

    // Snapshot of persisted state (last saved values)
    private var snapshotOptions: ClipCraftOptions = settingsService.state.advancedOptions.deepCopy()
    private var snapshotMaxCopyChars: String = settingsService.state.maxCopyCharacters.toString()

    // Working copies that the UI edits
    private var workingOptions: ClipCraftOptions = snapshotOptions.deepCopy()
    private var workingMaxCopyChars: String = snapshotMaxCopyChars

    // UI components
    private lateinit var snippetHeaderField: TextFieldWithAutoCompletion<String>
    private lateinit var maxCopyField: JTextField
    private lateinit var snippetFooterField: JTextField
    private val previewArea: JTextArea = JTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        text = "Preview will appear here..."
    }

    companion object {
        // Default header for display (only used if user leaves header field blank)
        private const val DEFAULT_SNIPPET_HEADER = "File: {fileName} | Path: {filePath}"
    }

    override fun getId(): String = "clipcraft.settings.child"
    override fun getDisplayName(): String = "ClipCraft Configuration"

    override fun createComponent(): JComponent {
        log("createComponent() called")
        val settingsPanel = panel {
            group("General Settings") {
                row("Max Copy Characters:") {
                    textField()
                        .comment("Maximum characters to copy.")
                        .columns(10)
                        .bindText(
                            getter = {
                                log("Getter: workingMaxCopyChars = $workingMaxCopyChars")
                                workingMaxCopyChars
                            },
                            setter = { newVal ->
                                log("Setter: workingMaxCopyChars = $newVal")
                                workingMaxCopyChars = newVal
                                updatePreview()
                            },
                        )
                        .applyToComponent {
                            maxCopyField = this
                            document.addDocumentListener(object : SwingDocumentListener {
                                override fun insertUpdate(e: SwingDocumentEvent?) {
                                    log("Max Copy Characters changed to: $text")
                                }

                                override fun removeUpdate(e: SwingDocumentEvent?) {
                                    log("Max Copy Characters changed to: $text")
                                }

                                override fun changedUpdate(e: SwingDocumentEvent?) {
                                    log("Max Copy Characters changed to: $text")
                                }
                            })
                        }
                }.comment("Limit the length of copied text.")
            }

            group("Concurrency Options") {
                row("Concurrency Mode:") {
                    comboBox(ConcurrencyMode.entries.toList())
                        .comment("Select the concurrency mode.")
                        .applyToComponent {
                            selectedItem = workingOptions.concurrencyMode
                            log("Initial ConcurrencyMode: ${workingOptions.concurrencyMode}")
                        }
                        .whenItemSelectedFromUi { newVal ->
                            if (newVal != workingOptions.concurrencyMode) {
                                log("ConcurrencyMode changed: ${workingOptions.concurrencyMode} -> $newVal")
                                workingOptions.concurrencyMode = newVal
                                updatePreview()
                            }
                        }
                }.comment("Processing mode for files.")
                row("Max Concurrent Tasks:") {
                    val spinnerField = spinnerTextField(workingOptions.maxConcurrentTasks, 1, 64)
                    cell(spinnerField)
                        .comment("Maximum parallel tasks.")
                    spinnerField.whenTextChangedFromUi {
                        val newVal = spinnerField.getIntValue()
                        if (newVal != workingOptions.maxConcurrentTasks) {
                            log("MaxConcurrentTasks changed: ${workingOptions.maxConcurrentTasks} -> $newVal")
                            workingOptions.maxConcurrentTasks = newVal
                            updatePreview()
                        }
                    }
                }.comment("Limit for parallel tasks.")
            }

            group("File Options") {
                row {
                    checkBox("Include Image Files")
                        .comment("Process image files as well.")
                        .applyToComponent {
                            isSelected = workingOptions.includeImageFiles
                            addActionListener {
                                log("Include Image Files changed: $isSelected")
                                workingOptions.includeImageFiles = isSelected
                                updatePreview()
                            }
                        }
                }.comment("Toggle image file processing.")
            }

            group("Header & Footer") {
                row("Snippet Header:") {
                    val placeholders =
                        listOf("{fileName}", "{filePath}", "{size}", "{modified}", "{id}", "{relativePath}")
                    snippetHeaderField = TextFieldWithAutoCompletion.create(
                        /* project = */ null,
                        placeholders,
                        true,
                        workingOptions.snippetHeaderText.orEmpty(),
                    ).apply {
                        preferredSize = Dimension(400, 32)
                        toolTipText = "Header text. You can include metadata tokens like {fileName}."
                        document.addDocumentListener(object : DocumentListener {
                            override fun documentChanged(event: DocumentEvent) {
                                val typedHeader = text
                                log("SnippetHeader changed to: '$typedHeader'")
                                workingOptions.snippetHeaderText = typedHeader
                                updatePreview()
                            }
                        })
                    }
                    cell(snippetHeaderField)
                    button("Insert Placeholder") {
                        log("Insert Placeholder clicked")
                        val selected = JOptionPane.showInputDialog(
                            null,
                            "Select Placeholder",
                            "Insert Placeholder",
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            placeholders.toTypedArray(),
                            placeholders.firstOrNull(),
                        ) as? String
                        selected?.let { placeholder ->
                            val tf: JTextField? = UIUtil.findComponentOfType(snippetHeaderField, JTextField::class.java)
                            val pos = tf?.caretPosition ?: snippetHeaderField.text.length
                            val newText = snippetHeaderField.text.substring(0, pos) +
                                placeholder +
                                snippetHeaderField.text.substring(pos)
                            log("Inserted placeholder '$placeholder' at pos $pos => $newText")
                            snippetHeaderField.text = newText
                        }
                    }
                }.comment("Header (metadata will be prepended if enabled).")
                row("Snippet Footer:") {
                    textField()
                        .comment("Footer text to appear after snippet content.")
                        .columns(40)
                        .bindText(
                            getter = {
                                val f = workingOptions.snippetFooterText.orEmpty()
                                log("Getter: snippetFooter = '$f'")
                                f
                            },
                            setter = { newVal ->
                                val trimmed = newVal.trim()
                                log("Setter: snippetFooter = '$trimmed'")
                                workingOptions.snippetFooterText = trimmed
                                updatePreview()
                            },
                        )
                        .applyToComponent {
                            snippetFooterField = this
                            document.addDocumentListener(object : SwingDocumentListener {
                                override fun insertUpdate(e: SwingDocumentEvent?) {
                                    log("SnippetFooter changed to: $text")
                                }

                                override fun removeUpdate(e: SwingDocumentEvent?) {
                                    log("SnippetFooter changed to: $text")
                                }

                                override fun changedUpdate(e: SwingDocumentEvent?) {
                                    log("SnippetFooter changed to: $text")
                                }
                            })
                        }
                }.comment("Footer text.")
            }

            group("Advanced Options") {
                row {
                    checkBox("Respect .gitignore")
                        .comment("Skip files ignored by Git.")
                        .applyToComponent {
                            isSelected = workingOptions.useGitIgnore
                            addActionListener {
                                log("Respect .gitignore changed: $isSelected")
                                workingOptions.useGitIgnore = isSelected
                                updatePreview()
                            }
                        }
                }.comment("Skip Git-ignored files.")
                row {
                    checkBox("Hierarchical Directory Summary")
                        .comment("Display directory summary hierarchically.")
                        .applyToComponent {
                            isSelected = workingOptions.hierarchicalDirectorySummary
                            addActionListener {
                                log("Hierarchical Directory Summary changed: $isSelected")
                                workingOptions.hierarchicalDirectorySummary = isSelected
                                updatePreview()
                            }
                        }
                }.comment("Hierarchical view.")
                row {
                    checkBox("Include IDE Problems")
                        .comment("Include IDE issues in output.")
                        .applyToComponent {
                            isSelected = workingOptions.includeIdeProblems
                            addActionListener {
                                log("Include IDE Problems changed: $isSelected")
                                workingOptions.includeIdeProblems = isSelected
                                updatePreview()
                            }
                        }
                }.comment("Show IDE issues.")
            }

            group("Output Target & Preview") {
                row("Output Target:") {
                    comboBox(OutputTarget.entries)
                        .comment("Choose the destination for output.")
                        .applyToComponent {
                            selectedItem = workingOptions.outputTarget
                            log("Initial OutputTarget: ${workingOptions.outputTarget}")
                        }
                        .whenItemSelectedFromUi { newVal ->
                            if (newVal != workingOptions.outputTarget) {
                                log("OutputTarget changed: ${workingOptions.outputTarget} -> $newVal")
                                workingOptions.outputTarget = newVal
                                updatePreview()
                            }
                        }
                }.comment("Select output destination.")
                row("Preview:") {
                    cell(
                        JBScrollPane(previewArea).apply {
                            preferredSize = Dimension(400, 160)
                        },
                    )
                }.comment("Live preview (updates on change).")
                row {
                    button("Copy Preview to Clipboard") {
                        log("Copy Preview button clicked")
                        Toolkit.getDefaultToolkit().systemClipboard.setContents(
                            StringSelection(previewArea.text),
                            null,
                        )
                        JOptionPane.showMessageDialog(null, "Preview copied!", "Copy", JOptionPane.INFORMATION_MESSAGE)
                    }
                }
                row {
                    button("Reset Defaults") {
                        log("Reset Defaults clicked")
                        resetToDefaults()
                    }
                    button("Export Settings") {
                        log("Export Settings clicked")
                        JOptionPane.showMessageDialog(
                            null,
                            "Export not implemented.",
                            "Export",
                            JOptionPane.INFORMATION_MESSAGE,
                        )
                    }
                    button("Import Settings") {
                        log("Import Settings clicked")
                        JOptionPane.showMessageDialog(
                            null,
                            "Import not implemented.",
                            "Import",
                            JOptionPane.INFORMATION_MESSAGE,
                        )
                    }
                }.comment("Reset, export, or import settings.")
            }
        }

        val scrollPane = JBScrollPane(settingsPanel).apply {
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
            preferredSize = Dimension(700, 700)
        }
        log("createComponent() finished")
        return scrollPane
    }

    override fun reset() {
        log("reset() called - loading from persistent state")
        val persistent = settingsService.state
        log("Current persistent state: $persistent")
        snapshotOptions = persistent.advancedOptions.deepCopy()
        snapshotMaxCopyChars = persistent.maxCopyCharacters.toString()
        workingOptions = snapshotOptions.deepCopy()
        workingMaxCopyChars = snapshotMaxCopyChars
        if (::snippetHeaderField.isInitialized) {
            snippetHeaderField.text = workingOptions.snippetHeaderText.orEmpty()
            updatePreview()
        }
        log("reset() completed - working copies reloaded from persistent")
    }

    override fun isModified(): Boolean {
        val changed = (workingMaxCopyChars != snapshotMaxCopyChars) ||
            (workingOptions != snapshotOptions)
        log("isModified() => $changed")
        return changed
    }

    @Throws(ConfigurationException::class)
    override fun apply() {
        log("apply() called")
        if (workingOptions.concurrencyMode != ConcurrencyMode.DISABLED &&
            workingOptions.maxConcurrentTasks < 1
        ) {
            log("ERROR: Invalid concurrency settings")
            throw ConfigurationException("Max Concurrent Tasks must be >= 1 if concurrency is enabled.")
        }
        val newMaxCopy = workingMaxCopyChars.toIntOrNull() ?: settingsService.state.maxCopyCharacters
        val newState = settingsService.state.copy(
            maxCopyCharacters = newMaxCopy,
            advancedOptions = workingOptions.deepCopy(),
        )
        log("New state computed => loading: $newState")
        settingsService.loadState(newState)
        snapshotOptions = workingOptions.deepCopy()
        snapshotMaxCopyChars = workingMaxCopyChars
        log("apply() completed. New persistent state => ${settingsService.state}")
    }

    /**
     * Updates the preview by constructing a sample snippet.
     *
     * NOTE: We now remove any hard-coded header/footer from the snippet content,
     * letting the formatter insert the metadata (if enabled), header, code block, and footer.
     */
    private fun updatePreview() {
        if (!::snippetHeaderField.isInitialized) {
            log("updatePreview() called before snippetHeaderField is initialized; skipping")
            return
        }
        // The workingOptions values for snippetHeaderText and snippetFooterText are already updated by the listeners.
        // We construct a sample snippet with a basic code content.
        val sampleSnippet = Snippet(
            filePath = "/home/user/projects/demo/src/ComplexSample.kt",
            relativePath = "demo/src/ComplexSample.kt",
            fileName = "ComplexSample.kt",
            fileSizeBytes = 456L,
            lastModified = 1680000000000L,
            content = """
                fun main() {
                    println("Hello from ComplexSample!")
                }
            """.trimIndent(),
        )
        val resultText = CodeFormatter.formatSnippets(listOf(sampleSnippet), workingOptions)
        previewArea.text = resultText
        log("updatePreview() completed")
    }

    private fun resetToDefaults() {
        log("resetToDefaults() called")
        workingOptions = ClipCraftOptions()
        workingMaxCopyChars = "100048576"
        if (::snippetHeaderField.isInitialized) {
            snippetHeaderField.text = workingOptions.snippetHeaderText.orEmpty()
        }
        updatePreview()
        log("resetToDefaults() completed")
    }

    private fun spinnerTextField(initialValue: Int, min: Int, max: Int): SpinnerTextField {
        log("spinnerTextField() => initialValue=$initialValue, range=[$min..$max]")
        return SpinnerTextField(initialValue, min, max)
    }

    private fun log(message: String) {
        LOG.debug(message)
        // Uncomment below to also print to console:
        // println("[DEBUG] $message")
    }
}

/**
 * Extension function for deep copying a ClipCraftOptions instance.
 */
private fun ClipCraftOptions.deepCopy() = ClipCraftOptions(
    concurrencyMode = this.concurrencyMode,
    maxConcurrentTasks = this.maxConcurrentTasks,
    chunkStrategy = this.chunkStrategy,
    chunkSize = this.chunkSize,
    overlapStrategy = this.overlapStrategy,
    compressionMode = this.compressionMode,
    selectiveCompression = this.selectiveCompression,
    outputFormat = this.outputFormat,
    includeLineNumbers = this.includeLineNumbers,
    removeImports = this.removeImports,
    removeComments = this.removeComments,
    trimWhitespace = this.trimWhitespace,
    removeEmptyLines = this.removeEmptyLines,
    singleLineOutput = this.singleLineOutput,
    includeDirectorySummary = this.includeDirectorySummary,
    hierarchicalDirectorySummary = this.hierarchicalDirectorySummary,
    includeMetadata = this.includeMetadata,
    metadataTemplate = this.metadataTemplate,
    snippetHeaderText = this.snippetHeaderText,
    snippetFooterText = this.snippetFooterText,
    showLint = this.showLint,
    lintErrorsOnly = this.lintErrorsOnly,
    lintWarningsOnly = this.lintWarningsOnly,
    includeLintInOutput = this.includeLintInOutput,
    includeGitInfo = this.includeGitInfo,
    useGitIgnore = this.useGitIgnore,
    enableDirectoryPatternMatching = this.enableDirectoryPatternMatching,
    additionalIgnorePatterns = this.additionalIgnorePatterns,
    invertIgnorePatterns = this.invertIgnorePatterns,
    ignorePatterns = this.ignorePatterns.toMutableList(),
    ignoreFiles = this.ignoreFiles?.toList(),
    ignoreFolders = this.ignoreFolders?.toList(),
    detectBinary = this.detectBinary,
    binaryCheckThreshold = this.binaryCheckThreshold,
    addSnippetToQueue = this.addSnippetToQueue,
    includeImageFiles = this.includeImageFiles,
    autoDetectLanguage = this.autoDetectLanguage,
    themeMode = this.themeMode,
    collapseBlankLines = this.collapseBlankLines,
    removeLeadingBlankLines = this.removeLeadingBlankLines,
    outputMacroTemplate = this.outputMacroTemplate,
    outputTarget = this.outputTarget,
    includeIdeProblems = this.includeIdeProblems,
    maxCopyCharacters = this.maxCopyCharacters,
)
