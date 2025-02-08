package com.clipcraft.ui

import com.clipcraft.ClipCraftUtil
import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.services.ClipCraftMacroManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.ui.EditorTextField
import java.awt.BorderLayout
import javax.swing.JPanel

/**
 * A live preview panel that uses IntelliJ's EditorTextField for syntax highlighting.
 */
class ClipCraftPreviewPanel : JPanel(BorderLayout()) {

    private val editorTextField: EditorTextField

    init {
        val defaultFileType = FileTypeManager.getInstance().findFileTypeByName("TEXT")
            ?: error("Default TEXT file type not found")
        editorTextField = EditorTextField("", null, defaultFileType)
        add(editorTextField, BorderLayout.CENTER)
    }

    fun updatePreview(content: String, opts: ClipCraftOptions, languageHint: String = "txt") {
        val processed = ClipCraftMacroManager.applyMacros(content, opts.macros)
        val fileType = ClipCraftUtil.resolveFileType(languageHint)
        val document = EditorFactory.getInstance().createDocument(processed)
        editorTextField.setNewDocumentAndFileType(fileType, document)
        (editorTextField.editor as? EditorEx)?.isViewer = true
    }

    fun getText(): String = editorTextField.text
}
