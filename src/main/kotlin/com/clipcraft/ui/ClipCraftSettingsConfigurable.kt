package com.clipcraft.ui

import com.clipcraft.icons.ClipCraftIcons
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
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.JTextField

class ClipCraftSettingsConfigurable : SearchableConfigurable, Configurable.NoScroll {
    private val globalState = ClipCraftSettingsState.getInstance()
    private val advancedOptions = globalState.advancedOptions
    private var modified: Boolean = false
    private val previewArea: JTextArea = JTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        text = "Preview will appear here..."
    }
    override fun getId(): String = "clipcraft.settings.child"
    override fun getDisplayName(): String = "ClipCraft Configuration"
    override fun createComponent(): JComponent {
        val mainPanel = panel {
            group("General Settings") {
                row("Max Copy Characters:") {
                    textField().columns(10).bindText(
                        getter = { globalState.maxCopyCharacters.toString() },
                        setter = { newVal ->
                            val num = newVal.toIntOrNull() ?: globalState.maxCopyCharacters
                            if (num != globalState.maxCopyCharacters) {
                                globalState.maxCopyCharacters = num
                                modified = true
                                updatePreview()
                            }
                        },
                    )
                }
                row {
                    button("Reset to Default") { resetToDefaults() }
                }
            }
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
                }
                row("Max Concurrent Tasks:") {
                    spinnerTextField(
                        initialValue = advancedOptions.maxConcurrentTasks,
                        min = 1,
                        max = 64,
                    ).whenValueChanged { newVal ->
                        if (newVal != advancedOptions.maxConcurrentTasks) {
                            advancedOptions.maxConcurrentTasks = newVal
                            modified = true
                            updatePreview()
                        }
                    }
                }
            }
            group("Metadata & Additional Options") {
                row {
                    checkBox("Include Metadata").bindSelected(
                        { advancedOptions.includeMetadata },
                        {
                            advancedOptions.includeMetadata = it
                            modified = true
                            updatePreview()
                        },
                    )
                }
                row {
                    checkBox("Include Image Files")
                        .bindSelected(
                            { advancedOptions.includeImageFiles },
                            {
                                advancedOptions.includeImageFiles = it
                                modified = true
                                updatePreview()
                            },
                        )
                }
                row("Metadata Template:") {
                    textField().columns(40).bindText(
                        getter = { advancedOptions.metadataTemplate.orEmpty() },
                        setter = { newVal ->
                            val value = if (newVal.isBlank()) null else newVal
                            if (value != advancedOptions.metadataTemplate) {
                                advancedOptions.metadataTemplate = value
                                modified = true
                                updatePreview()
                            }
                        },
                    )
                    label("").applyToComponent {
                        icon =
                            IconLoader.findIcon("/icons/help.svg", ClipCraftIcons::class.java) ?: AllIcons.General.ContextHelp
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
                    textField().columns(40).bindText(
                        getter = { advancedOptions.snippetHeaderText.orEmpty() },
                        setter = { newVal ->
                            val value = if (newVal.isBlank()) null else newVal
                            if (value != advancedOptions.snippetHeaderText) {
                                advancedOptions.snippetHeaderText = value
                                modified = true
                                updatePreview()
                            }
                        },
                    )
                }
                row("Snippet Footer:") {
                    textField().columns(40).bindText(
                        getter = { advancedOptions.snippetFooterText.orEmpty() },
                        setter = { newVal ->
                            val value = if (newVal.isBlank()) null else newVal
                            if (value != advancedOptions.snippetFooterText) {
                                advancedOptions.snippetFooterText = value
                                modified = true
                                updatePreview()
                            }
                        },
                    )
                }
            }
            group("Advanced Options") {
                row {
                    checkBox("Respect .gitignore").bindSelected(
                        { advancedOptions.useGitIgnore },
                        {
                            advancedOptions.useGitIgnore = it
                            modified = true
                            updatePreview()
                        },
                    )
                }
                row {
                    checkBox("Hierarchical Directory Summary").bindSelected(
                        { advancedOptions.hierarchicalDirectorySummary },
                        {
                            advancedOptions.hierarchicalDirectorySummary = it
                            modified = true
                            updatePreview()
                        },
                    )
                }
                row {
                    checkBox("Include IDE Problems").bindSelected(
                        { advancedOptions.includeIdeProblems },
                        {
                            advancedOptions.includeIdeProblems = it
                            modified = true
                            updatePreview()
                        },
                    )
                }
            }
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
                }
                row("Preview:") {
                    cell(
                        JBScrollPane(previewArea).apply {
                            preferredSize = Dimension(400, 160)
                        },
                    )
                }
                row {
                    button("Reset Defaults") { resetToDefaults() }
                    button("Export Settings") { exportSettings() }
                    button("Import Settings") { importSettings() }
                }
            }
        }
        val scrollPane = JBScrollPane(mainPanel)
        scrollPane.verticalScrollBar.unitIncrement = 16
        scrollPane.preferredSize = Dimension(700, 700)
        updatePreview()
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
    private fun updatePreview() {
        val sampleSnippet = Snippet(
            filePath = "/home/user/projects/demo/src/Sample.kt",
            relativePath = "demo/src/Sample.kt",
            fileName = "Sample.kt",
            fileSizeBytes = 123L,
            lastModified = 1678999999999L,
            content = """
package com.clipcraft.utils;


import java.io.PrintStream;


public class Utils {

private static final String TAG = "Utils";


public static void helloWorld(PrintStream out) {
out.println("Hello, world!");
}

}
            """.trimIndent(),
        )
        val resultBlocks = CodeFormatter.formatSnippets(listOf(sampleSnippet), advancedOptions)
        previewArea.text = resultBlocks.joinToString("\n---\n")
    }
    private fun resetToDefaults() {
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
        updatePreview()
    }
    private fun exportSettings() {
        JOptionPane.showMessageDialog(
            null,
            "Export functionality is not yet implemented.",
            "Export Settings",
            JOptionPane.INFORMATION_MESSAGE,
        )
    }
    private fun importSettings() {
        JOptionPane.showMessageDialog(
            null,
            "Import functionality is not yet implemented.",
            "Import Settings",
            JOptionPane.INFORMATION_MESSAGE,
        )
    }
    private fun Row.spinnerTextField(
        initialValue: Int,
        min: Int,
        max: Int,
    ): SpinnerTextField {
        val spinner = SpinnerTextField(initialValue, min, max)
        cell(spinner)
        return spinner
    }
}

class SpinnerTextField(
    initialValue: Int,
    private val min: Int,
    private val max: Int,
) : JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)) {
    private val textField: JTextField = JTextField(initialValue.toString(), 5)
    private val incrementButton: JButton = JButton("+")
    private val decrementButton: JButton = JButton("-")
    init {
        textField.maximumSize = Dimension(50, textField.preferredSize.height)
        add(decrementButton)
        add(textField)
        add(incrementButton)
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
        textField.addFocusListener(object : FocusAdapter() {
            override fun focusLost(e: FocusEvent?) {
                val current = textField.text.toIntOrNull() ?: min
                val clamped = current.coerceIn(min, max)
                if (clamped != current) {
                    val oldVal = current
                    textField.text = clamped.toString()
                    firePropertyChange("value", oldVal, clamped)
                }
            }
        })
    }
    fun whenValueChanged(listener: (Int) -> Unit) {
        addPropertyChangeListener("value") { evt ->
            (evt.newValue as? Int)?.let { listener(it) }
        }
    }
    fun getValue(): Int = textField.text.toIntOrNull() ?: min
}
