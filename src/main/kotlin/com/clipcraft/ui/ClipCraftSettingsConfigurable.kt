package com.clipcraft.ui

import com.clipcraft.model.ConcurrencyMode
import com.clipcraft.model.OutputTarget
import com.clipcraft.model.Snippet
import com.clipcraft.services.ClipCraftSettingsState
import com.clipcraft.util.CodeFormatter
import com.intellij.icons.AllIcons
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.whenItemSelectedFromUi
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.JTextField

/**
 * A production–ready settings panel for ClipCraft.
 *
 * This class organizes settings into several logical sections:
 *  - General Settings
 *  - Concurrency Options (with a spinner for numeric input)
 *  - Metadata & Additional Options (with inline help and tooltips)
 *  - Advanced Options (like .gitignore, hierarchical summary, IDE problems)
 *  - Output & Preview (with live preview, and reset/import/export actions)
 *
 * It uses the IntelliJ UI DSL for layout and styling,
 * and follows modern accessibility, theming, and keyboard‐navigation guidelines.
 */
class ClipCraftSettingsConfigurable : SearchableConfigurable, Configurable.NoScroll {

    // Retrieve persisted state.
    private val globalState = ClipCraftSettingsState.getInstance()
    private val advancedOptions = globalState.advancedOptions
    private var modified: Boolean = false

    // Preview area – using a JTextArea (read-only) for now.
    private val previewArea: JTextArea = JTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        text = "Preview will appear here..."
    }

    // Main component will be constructed by combining several sections.
    override fun getId(): String = "clipcraft.settings.child"
    override fun getDisplayName(): String = "ClipCraft Configuration"

    override fun createComponent(): JComponent {
        // Use the IntelliJ DSL to build a form.
        val mainPanel = panel {
            // General settings section.
            group("General Settings") {
                row("Max Copy Characters:") {
                    textField()
                        .columns(10)
                        .bindText(
                            getter = { globalState.maxCopyCharacters.toString() },
                            setter = { newVal ->
                                val num = newVal.toIntOrNull() ?: globalState.maxCopyCharacters
                                if (num != globalState.maxCopyCharacters) {
                                    globalState.maxCopyCharacters = num
                                    modified = true
                                    updatePreview()
                                }
                            }
                        )
                        .comment("Total maximum characters to copy (numeric).")
                }
                row {
                    button("Reset to Default") { resetToDefaults() }
                        .comment("Resets general settings to their default values.")
                }
            }

            // Concurrency settings section.
            group("Concurrency Options") {
                row("Concurrency Mode:") {
                    comboBox(ConcurrencyMode.entries)
                        .applyToComponent { selectedItem = advancedOptions.concurrencyMode }
                        .whenItemSelectedFromUi { newVal ->
                            if (newVal != advancedOptions.concurrencyMode) {
                                advancedOptions.concurrencyMode = newVal
                                modified = true
                                updatePreview()
                            }
                        }
                        .comment("Select the concurrency strategy for processing tasks.")
                }
                row("Max Concurrent Tasks:") {
                    // Use a spinner control for numeric input.
                    spinnerTextField(
                        initialValue = advancedOptions.maxConcurrentTasks,
                        min = 1,
                        max = 64
                    ).whenValueChanged { newVal ->
                        if (newVal != advancedOptions.maxConcurrentTasks) {
                            advancedOptions.maxConcurrentTasks = newVal
                            modified = true
                            updatePreview()
                        }
                    }
                }.comment("Set the maximum number of concurrent tasks.")
            }

            // Metadata & Additional Options section.
            group("Metadata & Additional Options") {
                row {
                    checkBox("Include Metadata")
                        .bindSelected(
                            { advancedOptions.includeMetadata },
                            { advancedOptions.includeMetadata = it; modified = true; updatePreview() })
                        .comment("Include file metadata such as size, modified date, etc.")
                }
                row("Metadata Template:") {
                    textField()
                        .columns(40)
                        .bindText(
                            getter = { advancedOptions.metadataTemplate.orEmpty() },
                            setter = { newVal ->
                                val value = if (newVal.isBlank()) null else newVal
                                if (value != advancedOptions.metadataTemplate) {
                                    advancedOptions.metadataTemplate = value
                                    modified = true
                                    updatePreview()
                                }
                            }
                        )
                        .comment("Template using placeholders like {fileName}, {size}.")
                    // Help icon with tooltip.
                    label("")
                        .applyToComponent {
                            icon =
                                IconLoader.findIcon("/icons/help.svg", this::class.java) ?: AllIcons.General.ContextHelp
                            toolTipText = """
                                Use the following placeholders:
                                • {fileName} - the name of the file.
                                • {filePath} - the full path.
                                • {size} - file size in bytes.
                                • {modified} - last modified timestamp.
                                • {id} - unique identifier.
                                • {relativePath} - relative file path.
                            """.trimIndent()
                        }
                }
                row("Snippet Header:") {
                    textField()
                        .columns(40)
                        .bindText(
                            getter = { advancedOptions.snippetHeaderText.orEmpty() },
                            setter = { newVal ->
                                val value = if (newVal.isBlank()) null else newVal
                                if (value != advancedOptions.snippetHeaderText) {
                                    advancedOptions.snippetHeaderText = value
                                    modified = true
                                    updatePreview()
                                }
                            }
                        )
                        .comment("Text to prepend to each snippet.")
                }
                row("Snippet Footer:") {
                    textField()
                        .columns(40)
                        .bindText(
                            getter = { advancedOptions.snippetFooterText.orEmpty() },
                            setter = { newVal ->
                                val value = if (newVal.isBlank()) null else newVal
                                if (value != advancedOptions.snippetFooterText) {
                                    advancedOptions.snippetFooterText = value
                                    modified = true
                                    updatePreview()
                                }
                            }
                        )
                        .comment("Text to append to each snippet.")
                }
            }

            // Advanced Options section.
            group("Advanced Options") {
                row {
                    checkBox("Respect .gitignore")
                        .bindSelected(
                            { advancedOptions.useGitIgnore },
                            { advancedOptions.useGitIgnore = it; modified = true; updatePreview() })
                        .comment("If enabled, ignore files matched by .gitignore.")
                }
                row {
                    checkBox("Hierarchical Directory Summary")
                        .bindSelected(
                            { advancedOptions.hierarchicalDirectorySummary },
                            { advancedOptions.hierarchicalDirectorySummary = it; modified = true; updatePreview() })
                        .comment("Display directory structure as a hierarchical tree.")
                }
                row {
                    checkBox("Include IDE Problems")
                        .bindSelected(
                            { advancedOptions.includeIdeProblems },
                            { advancedOptions.includeIdeProblems = it; modified = true; updatePreview() })
                        .comment("If enabled, include IDE warnings/errors in the snippet output.")
                }
            }

            // Output & Preview section.
            group("Output Target & Preview") {
                row("Output Target:") {
                    comboBox(OutputTarget.values().toList())
                        .applyToComponent { selectedItem = advancedOptions.outputTarget }
                        .whenItemSelectedFromUi { newVal ->
                            if (newVal != advancedOptions.outputTarget) {
                                advancedOptions.outputTarget = newVal
                                modified = true
                                updatePreview()
                            }
                        }
                        .comment("Select where to send the output (Clipboard, Macro, or Both).")
                }
                row("Preview:") {
                    cell(
                        JBScrollPane(previewArea).apply {
                            preferredSize = Dimension(400, 160)
                        }
                    )
                }
                row {
                    button("Reset Defaults") { resetToDefaults() }
                        .comment("Restore all settings to their default values.")
                    button("Export Settings") { exportSettings() }
                        .comment("Export current settings to a file.")
                    button("Import Settings") { importSettings() }
                        .comment("Import settings from a file.")
                }
            }
        } // end DSL panel

        // Wrap entire form in a scroll pane to accommodate many sections.
        val scrollPane = JBScrollPane(mainPanel)
        scrollPane.verticalScrollBar.unitIncrement = 16
        scrollPane.preferredSize = Dimension(700, 700)
        updatePreview() // initialize preview based on current settings
        return scrollPane
    }

    override fun isModified(): Boolean = modified

    @Throws(ConfigurationException::class)
    override fun apply() {
        // Persist any unsaved changes.
        advancedOptions.resolveConflicts()
        // (Optionally) persist changes to a PersistentStateComponent here.
        modified = false
    }

    override fun reset() {
        // Revert UI fields to the values stored in advancedOptions.
        modified = false
        // In a full implementation, rebind each field.
        updatePreview()
    }

    /**
     * Updates the live preview area with a sample snippet processed via CodeFormatter.
     */
    private fun updatePreview() {
        val sampleSnippet = Snippet(
            filePath = "/home/user/projects/demo/src/Sample.kt",
            relativePath = "demo/src/Sample.kt",
            fileName = "Sample.kt",
            fileSizeBytes = 123L,
            lastModified = 1678999999999L,
            content = """
                package com.clipcraft.utils;
                
                
                import java.io.PrintStream;  // comment
                
                
                /**
                 * Utility class
                 */
                public class Utils {
                
                    private static final String TAG = "Utils"; // comment
                
                    // TODO: a todo...
                
                    /**
                     * Prints greeting.
                     *
                     * @param out the PrintStream to which the greeting will be printed
                     */
                    public static void helloWorld(PrintStream out) {
                        out.println("Hello, world!");
                    }
                
                }
            """.trimIndent()
        )
        val resultBlocks = CodeFormatter.formatSnippets(listOf(sampleSnippet), advancedOptions)
        previewArea.text = resultBlocks.joinToString("\n---\n")
    }

    /**
     * Resets all settings to default values.
     */
    private fun resetToDefaults() {
        // In a production plugin, these defaults would come from a defined default state.
        globalState.maxCopyCharacters = 1000000
        advancedOptions.concurrencyMode = ConcurrencyMode.DISABLED
        advancedOptions.maxConcurrentTasks = 4
        advancedOptions.includeMetadata = false
        advancedOptions.metadataTemplate = null
        advancedOptions.snippetHeaderText = null
        advancedOptions.snippetFooterText = null
        advancedOptions.useGitIgnore = false
        advancedOptions.hierarchicalDirectorySummary = false
        advancedOptions.includeIdeProblems = false
        advancedOptions.outputTarget = OutputTarget.CLIPBOARD
        modified = true
        // Refresh UI fields as needed (in a full implementation, update each field)
        updatePreview()
    }

    /**
     * Exports current settings to a file.
     */
    private fun exportSettings() {
        // This is a stub – in a production plugin you would serialize settings to XML or JSON.
        JOptionPane.showMessageDialog(
            null,
            "Export functionality is not yet implemented.",
            "Export Settings",
            JOptionPane.INFORMATION_MESSAGE
        )
    }

    /**
     * Imports settings from a file.
     */
    private fun importSettings() {
        // This is a stub – in a production plugin you would open a file chooser,
        // read and validate the file, then update the settings accordingly.
        JOptionPane.showMessageDialog(
            null,
            "Import functionality is not yet implemented.",
            "Import Settings",
            JOptionPane.INFORMATION_MESSAGE
        )
    }

    // -------------------------------
    // Additional helper functions below.
    // -------------------------------

    /**
     * Binds a spinner (numeric text field with up/down arrows) with a value change listener.
     * This is a helper extension for the DSL.
     */
    private fun Row.spinnerTextField(
        initialValue: Int,
        min: Int,
        max: Int
    ): SpinnerTextField {
        val spinner = SpinnerTextField(initialValue, min, max)
        cell(spinner)
        return spinner
    }
}

/**
 * A simple spinner text field that shows a number and allows increment/decrement.
 * In production, consider using IntelliJ’s built-in numeric editor if available.
 */
class SpinnerTextField(
    initialValue: Int,
    private val min: Int,
    private val max: Int
) : JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)) {

    private val textField: JTextField = JTextField(initialValue.toString(), 5)
    private val incrementButton: JButton = JButton("+")
    private val decrementButton: JButton = JButton("-")

    init {
        // Set preferred size for consistency.
        textField.maximumSize = Dimension(50, textField.preferredSize.height)
        add(decrementButton)
        add(textField)
        add(incrementButton)
        // Add action listeners.
        incrementButton.addActionListener {
            val current = textField.text.toIntOrNull() ?: initialValue
            if (current < max) {
                textField.text = (current + 1).toString()
                firePropertyChange("value", current, current + 1)
            }
        }
        decrementButton.addActionListener {
            val current = textField.text.toIntOrNull() ?: initialValue
            if (current > min) {
                textField.text = (current - 1).toString()
                firePropertyChange("value", current, current - 1)
            }
        }
    }

    /**
     * Adds a listener that is invoked whenever the numeric value changes.
     */
    fun whenValueChanged(listener: (Int) -> Unit) {
        addPropertyChangeListener("value") { evt ->
            (evt.newValue as? Int)?.let { listener(it) }
        }
    }

    /**
     * Returns the current value of the spinner.
     */
    fun getValue(): Int = textField.text.toIntOrNull() ?: min
}
