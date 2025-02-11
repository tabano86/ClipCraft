package com.clipcraft.actions

import com.clipcraft.model.Snippet
import com.clipcraft.services.SnippetsManager
import com.clipcraft.services.ClipCraftSettings
import com.clipcraft.util.CodeFormatter
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Document
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange

/**
 * Adds a snippet from the current editor selection or, if none, from the line at the caret.
 */
class ClipCraftAddSnippetFromCursorOrSelectionAction : AnAction("ClipCraft: Add Snippet from Selection") {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val snippetManager = project.getService(SnippetsManager::class.java) ?: return

        val activeProfile = ClipCraftSettings.getInstance().getCurrentProfile()
        val selectionModel = editor.selectionModel
        val document: Document = editor.document

        // Use selected text if available; otherwise, use the entire line at the caret
        val text = if (selectionModel.hasSelection()) {
            selectionModel.selectedText
        } else {
            val caretOffset = editor.caretModel.offset
            val lineNumber = document.getLineNumber(caretOffset)
            val startOffset = document.getLineStartOffset(lineNumber)
            val endOffset = document.getLineEndOffset(lineNumber)
            document.getText(TextRange(startOffset, endOffset))
        } ?: return

        // Clean up the code snippet as per profile settings
        val cleaned = CodeFormatter.formatSnippets(
            listOf(Snippet(content = text, fileName = "EditorSnippet")),
            activeProfile.options
        ).joinToString("\n") // Typically, 1 chunk

        snippetManager.addSnippet(Snippet(content = cleaned, fileName = "EditorSnippet"))
        selectionModel.removeSelection()

        Messages.showInfoMessage(project, "Snippet from editor added to the queue.", "ClipCraft")
    }
}
