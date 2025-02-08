package com.clipcraft.ui

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.services.ClipCraftMacroManager
import com.clipcraft.ClipCraftUtil
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.ui.EditorTextField
import java.awt.BorderLayout
import javax.swing.JPanel

class ClipCraftPreviewPanel : JPanel(BorderLayout()) {
    private val editorTextField: EditorTextField

    init {
        // Retrieve the default file type for fallback
        val defaultFileType = FileTypeManager.getInstance().findFileTypeByName("TEXT")
            ?: throw IllegalStateException("Default file type TEXT not found")
        // Create an EditorTextField using the basic constructor
        editorTextField = EditorTextField("")
        // Optionally, you can set an initial file type if needed.
        add(editorTextField, BorderLayout.CENTER)
    }

    fun updatePreview(content: String, opts: ClipCraftOptions, languageHint: String = "txt") {
        val processed = ClipCraftMacroManager.applyMacros(content, opts.macros)
        val fileType = ClipCraftUtil.resolveFileType(languageHint)
        // Create a Document from the processed string.
        val document = EditorFactory.getInstance().createDocument(processed)
        editorTextField.setNewDocumentAndFileType(fileType, document)
        (editorTextField.editor as? EditorEx)?.isViewer = true
    }

    fun getText(): String = editorTextField.text
}