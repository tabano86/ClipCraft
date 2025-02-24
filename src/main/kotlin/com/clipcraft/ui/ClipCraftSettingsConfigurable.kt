package com.clipcraft.ui

import com.clipcraft.model.ChunkStrategy
import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.CompressionMode
import com.clipcraft.model.ConcurrencyMode
import com.clipcraft.model.OverlapStrategy
import com.clipcraft.model.Snippet
import com.clipcraft.model.ThemeMode
import com.clipcraft.services.ClipCraftProfileManager
import com.clipcraft.util.CodeFormatter
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.Timer
import javax.swing.UIManager
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class ClipCraftSettingsConfigurable : Configurable {
    private val manager = ClipCraftProfileManager()
    private val savedProfile = manager.currentProfile().copy()
    private val options = savedProfile.options

    // Sample code for live preview demo
    private val sampleCode = """
        package test
        fun main() {
            println("Hello, ClipCraft!")
        }
    """.trimIndent()

    private lateinit var mainPanel: JPanel
    private lateinit var previewEditor: com.intellij.ui.EditorTextField

    // UI Controls
    private lateinit var headerArea: JBTextArea
    private lateinit var footerArea: JBTextArea
    private lateinit var livePreviewCheck: JCheckBox

    // Basic options
    private lateinit var dirStructCheck: JCheckBox
    private lateinit var lineNumbersCheck: JCheckBox
    private lateinit var removeImportsCheck: JCheckBox
    private lateinit var removeCommentsCheck: JCheckBox
    private lateinit var trimWSCheck: JCheckBox
    private lateinit var removeEmptyCheck: JCheckBox
    private lateinit var singleLineCheck: JCheckBox

    // Chunking & formatting
    private lateinit var chunkCombo: JComboBox<ChunkStrategy>
    private lateinit var chunkSizeField: JBTextField
    private lateinit var overlapCombo: JComboBox<OverlapStrategy>
    private lateinit var compressionCombo: JComboBox<CompressionMode>
    private lateinit var chunkMsg: JLabel

    // Advanced options
    private lateinit var metaCheck: JCheckBox
    private lateinit var gitInfoCheck: JCheckBox
    private lateinit var autoLangCheck: JCheckBox
    private lateinit var themeCombo: JComboBox<ThemeMode>
    private lateinit var concurrencyCombo: JComboBox<ConcurrencyMode>
    private lateinit var maxTasksField: JBTextField
    private lateinit var gitIgnoreCheck: JCheckBox
    private lateinit var extraIgnoresField: JBTextField
    private lateinit var invertIgnoresCheck: JCheckBox
    private lateinit var dirPatternCheck: JCheckBox
    private lateinit var detectBinaryCheck: JCheckBox
    private lateinit var binaryThresholdField: JBTextField
    private lateinit var lintCheck: JCheckBox
    private lateinit var includeImageCheck: JCheckBox
    private lateinit var lintErrOnlyCheck: JCheckBox
    private lateinit var lintWarnOnlyCheck: JCheckBox
    private lateinit var addToQueueCheck: JCheckBox

    // Debounced preview update timer (300ms)
    private val previewTimer = Timer(300) { updatePreview() }.apply { isRepeats = false }

    // Helper methods for concise control creation
    private fun createCheckBox(
        text: String,
        init: Boolean,
        tooltip: String = "",
        action: (() -> Unit)? = null
    ): JCheckBox =
        JCheckBox(text, init).apply {
            toolTipText = tooltip
            action?.let { addActionListener { it() } }
        }

    private fun createTextField(
        text: String,
        columns: Int,
        tooltip: String = "",
        docAction: (() -> Unit)? = null
    ): JBTextField =
        JBTextField(text, columns).apply {
            toolTipText = tooltip
            docAction?.let {
                document.addDocumentListener(object : DocumentListener {
                    override fun insertUpdate(e: DocumentEvent?) = it()
                    override fun removeUpdate(e: DocumentEvent?) = it()
                    override fun changedUpdate(e: DocumentEvent?) = it()
                })
            }
        }

    private fun <T> createComboBox(
        items: Array<T>,
        selected: T,
        tooltip: String = "",
        action: (() -> Unit)? = null
    ): JComboBox<T> =
        JComboBox(items).apply {
            selectedItem = selected
            toolTipText = tooltip
            action?.let { addActionListener { it() } }
        }

    override fun getDisplayName() = "ClipCraft"

    override fun createComponent(): JComponent {
        mainPanel = JPanel(BorderLayout()).apply { border = JBUI.Borders.empty(16) }
        val formPanel = buildFormPanel()
        val formScroll = JBScrollPane(formPanel).apply {
            verticalScrollBar.unitIncrement = 16
            preferredSize = Dimension(480, 620)
        }
        val previewDoc = com.intellij.openapi.editor.EditorFactory.getInstance().createDocument("")
        previewEditor =
            com.intellij.ui.EditorTextField(previewDoc, ProjectManager.getInstance().defaultProject, null, true, false)
                .apply { isViewer = true; isOneLineMode = false; preferredSize = Dimension(480, 620) }
        val splitter = JBSplitter(true, 0.5f).apply {
            firstComponent = formScroll
            secondComponent = previewEditor
            dividerWidth = JBUI.scale(5)
        }
        mainPanel.add(splitter, BorderLayout.CENTER)
        updatePreview()
        return mainPanel
    }

    private fun buildFormPanel(): JPanel {
        // Basic Options
        headerArea = JBTextArea(options.snippetHeaderText.orEmpty(), 3, 20).apply {
            lineWrap = true; wrapStyleWord = true; toolTipText = "Text prepended to snippet output"
            document.addDocumentListener(SimpleDocListener { schedulePreview() })
        }
        footerArea = JBTextArea(options.snippetFooterText.orEmpty(), 3, 20).apply {
            lineWrap = true; wrapStyleWord = true; toolTipText = "Text appended to snippet output"
            document.addDocumentListener(SimpleDocListener { schedulePreview() })
        }
        dirStructCheck = createCheckBox(
            "Directory Structure",
            options.includeDirectorySummary,
            "Include directory summary",
            ::schedulePreview
        )
        lineNumbersCheck =
            createCheckBox("Line Numbers", options.includeLineNumbers, "Show line numbers", ::schedulePreview)
        removeImportsCheck =
            createCheckBox("Remove Imports", options.removeImports, "Strip import statements", ::schedulePreview)
        removeCommentsCheck =
            createCheckBox("Remove Comments", options.removeComments, "Strip comments", ::schedulePreview)
        trimWSCheck =
            createCheckBox("Trim Whitespace", options.trimWhitespace, "Trim extra whitespace", ::schedulePreview)
        removeEmptyCheck =
            createCheckBox("Remove Empty Lines", options.removeEmptyLines, "Eliminate blank lines", ::schedulePreview)
        singleLineCheck =
            createCheckBox("Single-line Output", options.singleLineOutput, "Produce single-line snippet", {
                schedulePreview(); updateChunkUI()
            })
        livePreviewCheck = createCheckBox("Live Preview", true, "Toggle live preview updates")

        // Chunking Controls
        chunkCombo =
            createComboBox(ChunkStrategy.entries.toTypedArray(), options.chunkStrategy, "Select chunk strategy", {
                schedulePreview(); updateChunkUI()
            })
        chunkSizeField =
            createTextField(options.chunkSize.toString(), 6, "Chunk size (if applicable)", ::schedulePreview)
        overlapCombo = createComboBox(
            OverlapStrategy.entries.toTypedArray(),
            options.overlapStrategy,
            "Overlap strategy",
            ::schedulePreview
        )
        compressionCombo = createComboBox(
            CompressionMode.entries.toTypedArray(),
            options.compressionMode,
            "Compression mode",
            ::schedulePreview
        )
        chunkMsg = JLabel("").apply { foreground = UIManager.getColor("Label.errorForeground") }

        // Advanced Options
        metaCheck =
            createCheckBox("Include Metadata", options.includeMetadata, "Include file metadata", ::schedulePreview)
        gitInfoCheck = createCheckBox("Git Info", options.includeGitInfo, "Include Git info", ::schedulePreview)
        autoLangCheck = createCheckBox(
            "Auto-Detect Language",
            options.autoDetectLanguage,
            "Detect language automatically",
            ::schedulePreview
        )
        themeCombo =
            createComboBox(ThemeMode.entries.toTypedArray(), options.themeMode, "Theme mode", ::schedulePreview)
        concurrencyCombo =
            createComboBox(ConcurrencyMode.entries.toTypedArray(), options.concurrencyMode, "Concurrency mode", {
                schedulePreview(); updateConcurrency()
            })
        maxTasksField =
            createTextField(options.maxConcurrentTasks.toString(), 4, "Max concurrent tasks", ::schedulePreview)
        gitIgnoreCheck =
            createCheckBox("Use .gitignore", options.useGitIgnore, "Apply .gitignore rules", ::schedulePreview)
        extraIgnoresField = createTextField(
            options.additionalIgnorePatterns.orEmpty(),
            15,
            "Additional ignore patterns",
            ::schedulePreview
        )
        invertIgnoresCheck =
            createCheckBox("Invert Patterns", options.invertIgnorePatterns, "Invert ignore patterns", ::schedulePreview)
        dirPatternCheck = createCheckBox(
            "Directory Matching",
            options.enableDirectoryPatternMatching,
            "Enable directory pattern matching",
            ::schedulePreview
        )
        detectBinaryCheck =
            createCheckBox("Detect Binary Files", options.detectBinary, "Detect binary files", ::schedulePreview)
        binaryThresholdField =
            createTextField(options.binaryCheckThreshold.toString(), 5, "Binary threshold", ::schedulePreview)
        lintCheck = createCheckBox("Show Lint Results", options.showLint, "Display lint info", ::schedulePreview)
        includeImageCheck =
            createCheckBox("Include Image Files", options.includeImageFiles, "Process image files", ::schedulePreview)
        lintErrOnlyCheck =
            createCheckBox("Lint Errors Only", options.lintErrorsOnly, "Only show lint errors", ::schedulePreview)
        lintWarnOnlyCheck =
            createCheckBox("Lint Warnings Only", options.lintWarningsOnly, "Only show lint warnings", ::schedulePreview)
        addToQueueCheck = createCheckBox(
            "Add Snippet to Queue",
            options.addSnippetToQueue,
            "Queue snippet for later",
            ::schedulePreview
        )

        // Buttons
        val copyPreviewBtn = JButton("Copy Preview").apply {
            toolTipText = "Copy preview text to clipboard"
            addActionListener {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(
                    StringSelection(previewEditor.text),
                    null
                )
            }
        }
        val copyRecursivelyBtn = JButton("Copy Recursively").apply {
            toolTipText = "Select files/directories (multi-select enabled) and copy formatted output without duplicates"
            addActionListener { copyRecursively() }
        }
        val testBtn = JButton("Test Formatting").apply {
            toolTipText = "Use real API to test code formatting on sample code"
            addActionListener {
                val snippet = Snippet(
                    content = sampleCode,
                    fileName = "Sample.kt",
                    relativePath = "src/Sample.kt",
                    filePath = "/fake/path/Sample.kt",
                    fileSizeBytes = sampleCode.length.toLong(),
                    lastModified = System.currentTimeMillis()
                )
                val output = try {
                    CodeFormatter.formatSnippets(listOf(snippet), options.copy()).joinToString("\n---\n")
                } catch (ex: Exception) {
                    "Error: ${ex.message}"
                }
                Messages.showInfoMessage(output, "Formatted Output")
            }
        }
        val exportBtn = JButton("Export Output").apply {
            toolTipText = "Save formatted output to a file"
            addActionListener { exportFormattedOutput() }
        }
        val openSettingsBtn = JButton("Open Settings").apply {
            toolTipText = "Open ClipCraft settings page"
            addActionListener {
                ShowSettingsUtil.getInstance()
                    .showSettingsDialog(ProjectManager.getInstance().defaultProject, "ClipCraft")
            }
        }
        val restoreBtn = JButton("Restore Defaults").apply {
            toolTipText = "Reset to default settings"
            addActionListener { restoreDefaults(); schedulePreview() }
        }

        // Build panels using FormBuilder
        val basicPanel = FormBuilder.createFormBuilder()
            .addComponent(livePreviewCheck)
            .addLabeledComponent("Header:", JBScrollPane(headerArea), 1, false)
            .addLabeledComponent("Footer:", JBScrollPane(footerArea), 1, false)
            .addComponent(dirStructCheck)
            .addComponent(lineNumbersCheck)
            .addComponent(removeImportsCheck)
            .addComponent(removeCommentsCheck)
            .addComponent(trimWSCheck)
            .addComponent(removeEmptyCheck)
            .addComponent(singleLineCheck)
            .panel.also { it.border = BorderFactory.createTitledBorder("Basic Options") }

        val chunkPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Chunk Strategy:", chunkCombo, 1, false)
            .addLabeledComponent("Chunk Size:", chunkSizeField, 1, false)
            .addLabeledComponent("Overlap:", overlapCombo, 1, false)
            .addLabeledComponent("Compression:", compressionCombo, 1, false)
            .addComponent(chunkMsg)
            .panel.also { it.border = BorderFactory.createTitledBorder("Chunking") }

        val advancedPanel = FormBuilder.createFormBuilder()
            .addComponent(metaCheck)
            .addComponent(gitInfoCheck)
            .addComponent(autoLangCheck)
            .addLabeledComponent("Theme:", themeCombo, 1, false)
            .addLabeledComponent("Concurrency:", concurrencyCombo, 1, false)
            .addLabeledComponent("Max Tasks:", maxTasksField, 1, false)
            .addComponent(gitIgnoreCheck)
            .addLabeledComponent("Extra Ignores:", extraIgnoresField, 1, false)
            .addComponent(invertIgnoresCheck)
            .addComponent(dirPatternCheck)
            .addComponent(detectBinaryCheck)
            .addLabeledComponent("Binary Threshold:", binaryThresholdField, 1, false)
            .addComponent(lintCheck)
            .addComponent(includeImageCheck)
            .addComponent(lintErrOnlyCheck)
            .addComponent(lintWarnOnlyCheck)
            .addComponent(addToQueueCheck)
            .panel.also { it.border = BorderFactory.createTitledBorder("Advanced Options") }

        val btnPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(copyPreviewBtn); add(Box.createHorizontalStrut(8))
            add(copyRecursivelyBtn); add(Box.createHorizontalStrut(8))
            add(testBtn); add(Box.createHorizontalStrut(8))
            add(exportBtn); add(Box.createHorizontalStrut(8))
            add(openSettingsBtn); add(Box.createHorizontalStrut(8))
            add(restoreBtn)
        }

        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(basicPanel)
            add(Box.createVerticalStrut(JBUI.scale(8)))
            add(chunkPanel)
            add(Box.createVerticalStrut(JBUI.scale(8)))
            add(advancedPanel)
            add(Box.createVerticalStrut(JBUI.scale(8)))
            add(btnPanel)
        }
    }

    private fun schedulePreview() {
        if (livePreviewCheck.isSelected) previewTimer.restart()
    }

    private fun updatePreview() {
        val tmp = options.copy().apply {
            snippetHeaderText = headerArea.text
            snippetFooterText = footerArea.text
            includeDirectorySummary = dirStructCheck.isSelected
            includeLineNumbers = lineNumbersCheck.isSelected
            removeImports = removeImportsCheck.isSelected
            removeComments = removeCommentsCheck.isSelected
            trimWhitespace = trimWSCheck.isSelected
            removeEmptyLines = removeEmptyCheck.isSelected
            singleLineOutput = singleLineCheck.isSelected
            chunkStrategy = chunkCombo.selectedItem as ChunkStrategy
            chunkSize = chunkSizeField.text.toIntOrNull() ?: chunkSize
            overlapStrategy = overlapCombo.selectedItem as OverlapStrategy
            compressionMode = compressionCombo.selectedItem as CompressionMode
            includeMetadata = metaCheck.isSelected
            includeGitInfo = gitInfoCheck.isSelected
            autoDetectLanguage = autoLangCheck.isSelected
            themeMode = themeCombo.selectedItem as ThemeMode
            concurrencyMode = concurrencyCombo.selectedItem as ConcurrencyMode
            maxConcurrentTasks = maxTasksField.text.toIntOrNull() ?: maxConcurrentTasks
            useGitIgnore = gitIgnoreCheck.isSelected
            additionalIgnorePatterns = extraIgnoresField.text.takeIf { it.isNotBlank() }
            invertIgnorePatterns = invertIgnoresCheck.isSelected
            enableDirectoryPatternMatching = dirPatternCheck.isSelected
            detectBinary = detectBinaryCheck.isSelected
            binaryCheckThreshold = binaryThresholdField.text.toIntOrNull() ?: binaryCheckThreshold
            showLint = lintCheck.isSelected
            includeImageFiles = includeImageCheck.isSelected
            lintErrorsOnly = lintErrOnlyCheck.isSelected
            lintWarningsOnly = lintWarnOnlyCheck.isSelected
            addSnippetToQueue = addToQueueCheck.isSelected
        }
        tmp.resolveConflicts()
        val snippet = Snippet(
            content = sampleCode,
            fileName = "Sample.kt",
            relativePath = "src/Sample.kt",
            filePath = "/fake/path/Sample.kt",
            fileSizeBytes = sampleCode.length.toLong(),
            lastModified = System.currentTimeMillis()
        )
        val formatted = try {
            CodeFormatter.formatSnippets(listOf(snippet), tmp).joinToString("\n---\n")
        } catch (ex: Exception) {
            "Formatting Error: ${ex.message}"
        }
        val previewText = buildString {
            if (headerArea.text.isNotBlank()) {
                appendLine(headerArea.text); appendLine()
            }
            if (dirStructCheck.isSelected) append("Directory Structure:\n  src/Sample.kt\n\n")
            append(formatted)
            if (footerArea.text.isNotBlank()) {
                appendLine(); appendLine(footerArea.text)
            }
            appendLine("\n---\n[Preview Info] Concurrency: ${tmp.concurrencyMode}, Chunk: ${tmp.chunkStrategy}")
        }
        previewEditor.text = previewText
        updateChunkUI()
    }

    private fun updateChunkUI() {
        val active = !singleLineCheck.isSelected && (chunkCombo.selectedItem as ChunkStrategy) != ChunkStrategy.NONE
        chunkSizeField.isEnabled = active && (chunkCombo.selectedItem == ChunkStrategy.BY_SIZE)
        overlapCombo.isEnabled = active
        compressionCombo.isEnabled = active
        chunkMsg.text = if (!active) "Chunking disabled" else ""
    }

    private fun updateConcurrency() {
        maxTasksField.isEnabled = (concurrencyCombo.selectedItem as ConcurrencyMode) != ConcurrencyMode.DISABLED
    }

    private fun restoreDefaults() {
        val d = ClipCraftOptions()
        headerArea.text = d.snippetHeaderText.orEmpty()
        footerArea.text = d.snippetFooterText.orEmpty()
        dirStructCheck.isSelected = d.includeDirectorySummary
        lineNumbersCheck.isSelected = d.includeLineNumbers
        removeImportsCheck.isSelected = d.removeImports
        removeCommentsCheck.isSelected = d.removeComments
        trimWSCheck.isSelected = d.trimWhitespace
        removeEmptyCheck.isSelected = d.removeEmptyLines
        singleLineCheck.isSelected = d.singleLineOutput
        chunkCombo.selectedItem = d.chunkStrategy
        chunkSizeField.text = d.chunkSize.toString()
        overlapCombo.selectedItem = d.overlapStrategy
        compressionCombo.selectedItem = d.compressionMode
        metaCheck.isSelected = d.includeMetadata
        gitInfoCheck.isSelected = d.includeGitInfo
        autoLangCheck.isSelected = d.autoDetectLanguage
        themeCombo.selectedItem = d.themeMode
        concurrencyCombo.selectedItem = d.concurrencyMode
        maxTasksField.text = d.maxConcurrentTasks.toString()
        gitIgnoreCheck.isSelected = d.useGitIgnore
        extraIgnoresField.text = d.additionalIgnorePatterns.orEmpty()
        invertIgnoresCheck.isSelected = d.invertIgnorePatterns
        dirPatternCheck.isSelected = d.enableDirectoryPatternMatching
        detectBinaryCheck.isSelected = d.detectBinary
        binaryThresholdField.text = d.binaryCheckThreshold.toString()
        lintCheck.isSelected = d.showLint
        includeImageCheck.isSelected = d.includeImageFiles
        lintErrOnlyCheck.isSelected = d.lintErrorsOnly
        lintWarnOnlyCheck.isSelected = d.lintWarningsOnly
        addToQueueCheck.isSelected = d.addSnippetToQueue
    }

    // Recursively collect snippets from a file/directory while avoiding duplicates.
    private fun collectSnippets(file: File, root: File, processed: MutableSet<String>): List<Snippet> {
        val snippets = mutableListOf<Snippet>()
        if (!processed.add(file.absolutePath)) return snippets // Skip duplicates
        if (file.isDirectory) {
            file.listFiles()?.forEach { snippets.addAll(collectSnippets(it, root, processed)) }
        } else {
            try {
                val content = file.readText()
                snippets.add(
                    Snippet(
                        content = content,
                        fileName = file.name,
                        relativePath = file.relativeTo(root).path,
                        filePath = file.absolutePath,
                        fileSizeBytes = file.length(),
                        lastModified = file.lastModified()
                    )
                )
            } catch (_: Exception) { /* Skip unreadable files */
            }
        }
        return snippets
    }

    // Allow multi-selection; process selected files/directories without duplicates.
    private fun copyRecursively() {
        val chooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.FILES_AND_DIRECTORIES
            isMultiSelectionEnabled = true
        }
        if (chooser.showOpenDialog(mainPanel) == JFileChooser.APPROVE_OPTION) {
            val selectedFiles = chooser.selectedFiles
            val allSnippets = mutableListOf<Snippet>()
            val processed = mutableSetOf<String>()
            for (file in selectedFiles) {
                allSnippets.addAll(collectSnippets(file, file, processed))
            }
            val formatted = try {
                CodeFormatter.formatSnippets(allSnippets, options.copy()).joinToString("\n---\n")
            } catch (ex: Exception) {
                "Error formatting snippets: ${ex.message}"
            }
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(formatted), null)
        }
    }

    // Export formatted output to a file.
    private fun exportFormattedOutput() {
        val chooser = JFileChooser().apply { fileSelectionMode = JFileChooser.FILES_ONLY }
        if (chooser.showSaveDialog(mainPanel) == JFileChooser.APPROVE_OPTION) {
            val file = chooser.selectedFile
            try {
                file.writeText(previewEditor.text)
                Messages.showInfoMessage("Export successful to ${file.absolutePath}", "Export")
            } catch (ex: Exception) {
                Messages.showErrorDialog("Failed to export: ${ex.message}", "Export Error")
            }
        }
    }

    override fun isModified(): Boolean = listOf(
        headerArea.text != options.snippetHeaderText.orEmpty(),
        footerArea.text != options.snippetFooterText.orEmpty(),
        dirStructCheck.isSelected != options.includeDirectorySummary,
        lineNumbersCheck.isSelected != options.includeLineNumbers,
        removeImportsCheck.isSelected != options.removeImports,
        removeCommentsCheck.isSelected != options.removeComments,
        trimWSCheck.isSelected != options.trimWhitespace,
        removeEmptyCheck.isSelected != options.removeEmptyLines,
        singleLineCheck.isSelected != options.singleLineOutput,
        chunkCombo.selectedItem != options.chunkStrategy,
        chunkSizeField.text != options.chunkSize.toString(),
        overlapCombo.selectedItem != options.overlapStrategy,
        compressionCombo.selectedItem != options.compressionMode,
        metaCheck.isSelected != options.includeMetadata,
        gitInfoCheck.isSelected != options.includeGitInfo,
        autoLangCheck.isSelected != options.autoDetectLanguage,
        themeCombo.selectedItem != options.themeMode,
        concurrencyCombo.selectedItem != options.concurrencyMode,
        maxTasksField.text != options.maxConcurrentTasks.toString(),
        gitIgnoreCheck.isSelected != options.useGitIgnore,
        extraIgnoresField.text != options.additionalIgnorePatterns.orEmpty(),
        invertIgnoresCheck.isSelected != options.invertIgnorePatterns,
        dirPatternCheck.isSelected != options.enableDirectoryPatternMatching,
        detectBinaryCheck.isSelected != options.detectBinary,
        binaryThresholdField.text != options.binaryCheckThreshold.toString(),
        lintCheck.isSelected != options.showLint,
        includeImageCheck.isSelected != options.includeImageFiles,
        lintErrOnlyCheck.isSelected != options.lintErrorsOnly,
        lintWarnOnlyCheck.isSelected != options.lintWarningsOnly,
        addToQueueCheck.isSelected != options.addSnippetToQueue
    ).any { it }

    @Throws(ConfigurationException::class)
    override fun apply() {
        val chunkSize = chunkSizeField.text.toIntOrNull() ?: throw ConfigurationException("Invalid Chunk Size")
        val maxTasks = maxTasksField.text.toIntOrNull() ?: throw ConfigurationException("Invalid Max Tasks")
        val binaryThreshold =
            binaryThresholdField.text.toIntOrNull() ?: throw ConfigurationException("Invalid Binary Threshold")
        with(options) {
            snippetHeaderText = headerArea.text.trim()
            snippetFooterText = footerArea.text.trim()
            includeDirectorySummary = dirStructCheck.isSelected
            includeLineNumbers = lineNumbersCheck.isSelected
            removeImports = removeImportsCheck.isSelected
            removeComments = removeCommentsCheck.isSelected
            trimWhitespace = trimWSCheck.isSelected
            removeEmptyLines = removeEmptyCheck.isSelected
            singleLineOutput = singleLineCheck.isSelected
            chunkStrategy = chunkCombo.selectedItem as ChunkStrategy
            this.chunkSize = chunkSize
            overlapStrategy = overlapCombo.selectedItem as OverlapStrategy
            compressionMode = compressionCombo.selectedItem as CompressionMode
            includeMetadata = metaCheck.isSelected
            includeGitInfo = gitInfoCheck.isSelected
            autoDetectLanguage = autoLangCheck.isSelected
            themeMode = themeCombo.selectedItem as ThemeMode
            concurrencyMode = concurrencyCombo.selectedItem as ConcurrencyMode
            maxConcurrentTasks = maxTasks
            useGitIgnore = gitIgnoreCheck.isSelected
            additionalIgnorePatterns = extraIgnoresField.text.takeIf { it.isNotBlank() }
            invertIgnorePatterns = invertIgnoresCheck.isSelected
            enableDirectoryPatternMatching = dirPatternCheck.isSelected
            detectBinary = detectBinaryCheck.isSelected
            binaryCheckThreshold = binaryThreshold
            showLint = lintCheck.isSelected
            includeImageFiles = includeImageCheck.isSelected
            lintErrorsOnly = lintErrOnlyCheck.isSelected
            lintWarningsOnly = lintWarnOnlyCheck.isSelected
            addSnippetToQueue = addToQueueCheck.isSelected
        }
        options.resolveConflicts()
        manager.addProfile(savedProfile.copy(options = options))
        manager.switchProfile(savedProfile.profileName)
        ApplicationManager.getApplication().saveAll()
    }

    override fun reset() {
        headerArea.text = options.snippetHeaderText.orEmpty()
        footerArea.text = options.snippetFooterText.orEmpty()
        dirStructCheck.isSelected = options.includeDirectorySummary
        lineNumbersCheck.isSelected = options.includeLineNumbers
        removeImportsCheck.isSelected = options.removeImports
        removeCommentsCheck.isSelected = options.removeComments
        trimWSCheck.isSelected = options.trimWhitespace
        removeEmptyCheck.isSelected = options.removeEmptyLines
        singleLineCheck.isSelected = options.singleLineOutput
        chunkCombo.selectedItem = options.chunkStrategy
        chunkSizeField.text = options.chunkSize.toString()
        overlapCombo.selectedItem = options.overlapStrategy
        compressionCombo.selectedItem = options.compressionMode
        metaCheck.isSelected = options.includeMetadata
        gitInfoCheck.isSelected = options.includeGitInfo
        autoLangCheck.isSelected = options.autoDetectLanguage
        themeCombo.selectedItem = options.themeMode
        concurrencyCombo.selectedItem = options.concurrencyMode
        maxTasksField.text = options.maxConcurrentTasks.toString()
        gitIgnoreCheck.isSelected = options.useGitIgnore
        extraIgnoresField.text = options.additionalIgnorePatterns.orEmpty()
        invertIgnoresCheck.isSelected = options.invertIgnorePatterns
        dirPatternCheck.isSelected = options.enableDirectoryPatternMatching
        detectBinaryCheck.isSelected = options.detectBinary
        binaryThresholdField.text = options.binaryCheckThreshold.toString()
        lintCheck.isSelected = options.showLint
        includeImageCheck.isSelected = options.includeImageFiles
        lintErrOnlyCheck.isSelected = options.lintErrorsOnly
        lintWarnOnlyCheck.isSelected = options.lintWarningsOnly
        addToQueueCheck.isSelected = options.addSnippetToQueue
        updatePreview()
        updateChunkUI()
        updateConcurrency()
    }

    override fun disposeUIResources() {}

    // Simple DocumentListener helper
    private class SimpleDocListener(val onChange: () -> Unit) : DocumentListener {
        override fun insertUpdate(e: DocumentEvent?) = onChange()
        override fun removeUpdate(e: DocumentEvent?) = onChange()
        override fun changedUpdate(e: DocumentEvent?) = onChange()
    }
}
