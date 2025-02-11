package com.clipcraft.actions

import com.clipcraft.model.Snippet
import com.clipcraft.services.SnippetsManager
import com.clipcraft.services.ClipCraftSettings
import com.clipcraft.util.CodeFormatter
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil

/**
 * Adds a snippet from the current editor selection, or if no selection, tries
 * to find the method at the caret. If none, falls back to the entire line.
 */
class ClipCraftAddSnippetFromCursorOrSelectionAction : AnAction("Add Snippet from Cursor/Method") {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        val snippetManager = project.getService(SnippetsManager::class.java) ?: return

        val activeProfile = ClipCraftSettings.getInstance().getCurrentProfile()
        val doc: Document = editor.document

        // Attempt selection first
        val selectionModel = editor.selectionModel
        val text = when {
            selectionModel.hasSelection() -> selectionModel.selectedText
            else -> {
                val methodText = findMethodAtCaret(psiFile, editor)
                if (methodText != null) methodText else {
                    // fallback to entire line
                    val caretOffset = editor.caretModel.offset
                    val lineNumber = doc.getLineNumber(caretOffset)
                    val startOffset = doc.getLineStartOffset(lineNumber)
                    val endOffset = doc.getLineEndOffset(lineNumber)
                    doc.getText(TextRange(startOffset, endOffset))
                }
            }
        } ?: return

        // Clean up the code snippet as per profile settings
        val cleaned = CodeFormatter.formatSnippets(
            listOf(Snippet(content = text, fileName = "EditorSnippet")),
            activeProfile.options
        ).joinToString("\n")

        snippetManager.addSnippet(Snippet(content = cleaned, fileName = "EditorSnippet"))
        selectionModel.removeSelection()

        Messages.showInfoMessage(project, "Snippet (method/line/selection) added to the queue.", "ClipCraft")
    }

    /**
     * Tries to find a PSI method at the caret offset. Returns the method's text if found,
     * or null if no method is found.
     */
    private fun findMethodAtCaret(psiFile: PsiFile?, editor: Editor): String? {
        if (psiFile == null) return null
        val caretOffset = editor.caretModel.offset
        val psiDocManager = PsiDocumentManager.getInstance(psiFile.project)
        psiDocManager.commitDocument(editor.document) // Ensure PSI is in sync

        val elementAtCaret = psiFile.findElementAt(caretOffset) ?: return null
        val method = PsiTreeUtil.getParentOfType<PsiMethod>(elementAtCaret, PsiMethod::class.java, false)
        return method?.text
    }
}
