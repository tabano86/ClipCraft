package com.clipcraft.ui

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.services.ClipCraftMacroManager
import com.clipcraft.ClipCraftUtil
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.ui.EditorTextField
import java.awt.BorderLayout
import javax.swing.JPanel

class ClipCraftPreviewPanel : JPanel(BorderLayout()) {
    private val editorTextField: EditorTextField

    init {
        val defaultFileType = FileTypeManager.getInstance().findFileTypeByName("TEXT")!!
        editorTextField = EditorTextField("", project = null, fileType = defaultFileType)
        add(editorTextField, BorderLayout.CENTER)
    }

    fun updatePreview(content: String, opts: ClipCraftOptions, languageHint: String = "txt") {
        val processed = ClipCraftMacroManager.applyMacros(content, opts.macros)
        // Syntax highlighting (if we wanted to detect a known language and set filetype)
        val fileType = ClipCraftUtil.resolveFileType(languageHint)
        editorTextField.setNewDocumentAndFileType(fileType, processed)
        (editorTextField.editor as? EditorEx)?.isViewer = true
    }

    fun getText(): String = editorTextField.text
}
