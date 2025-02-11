package com.clipcraft.actions

import com.clipcraft.services.ClipCraftSettings
import com.clipcraft.services.ClipCraftSnippetManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UastFacade

class ClipCraftAddSnippetFromCursorOrSelectionAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        // Retrieve the editor and file from the event context.
        val editor: Editor? = event.getData(CommonDataKeys.EDITOR)
        val psiFile: PsiFile? = event.getData(CommonDataKeys.PSI_FILE)
        if (editor == null || psiFile == null) {
            notify("No editor or file found.", NotificationType.WARNING)
            return
        }

        val snippetText = extractSnippet(editor, psiFile)
        if (snippetText == null || snippetText.trim().isEmpty()) {
            notify("No valid snippet could be extracted.", NotificationType.WARNING)
            return
        }

        val formattedSnippet = formatSnippet(snippetText)
        ClipCraftSnippetManager.getInstance().addSnippet(formattedSnippet)
        notify("Snippet added successfully.", NotificationType.INFORMATION)
    }

    /**
     * Extracts a snippet either from the current selection or, if absent,
     * finds the enclosing method using UAST.
     */
    private fun extractSnippet(editor: Editor, psiFile: PsiFile): String? {
        if (editor.selectionModel.hasSelection()) {
            return editor.selectionModel.selectedText
        }

        val caretOffset = editor.caretModel.offset
        val elementAtCaret = psiFile.findElementAt(caretOffset) ?: return null

        // Use UastFacade to convert the PSI element to a UAST method.
        val uMethod: UMethod? = UastFacade.convertElementWithParent(elementAtCaret, UMethod::class.java) as? UMethod
        return uMethod?.sourcePsi?.text
    }

    /**
     * Applies user-defined header and footer to the snippet.
     */
    private fun formatSnippet(snippet: String): String {
        val settings = ClipCraftSettings.getInstance()
        val header: String = settings.getHeader()
        val footer: String = settings.getFooter()
        val sb = StringBuilder()
        if (header.isNotBlank()) {
            sb.append(header.trim()).append("\n\n")
        }
        sb.append(snippet)
        if (footer.isNotBlank()) {
            sb.append("\n\n").append(footer.trim())
        }
        return sb.toString()
    }

    /**
     * Displays a notification to the user.
     */
    private fun notify(message: String, type: NotificationType) {
        val notification = Notification("ClipCraft", "ClipCraft Snippet Extraction", message, type)
        Notifications.Bus.notify(notification)
    }
}
