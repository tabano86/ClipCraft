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
        val editor = event.getData(CommonDataKeys.EDITOR)
        val psiFile = event.getData(CommonDataKeys.PSI_FILE)
        if (editor == null || psiFile == null) {
            notify("No editor or file found.", NotificationType.WARNING)
            return
        }
        val snippetText = editor.extractSnippetOrMethod(psiFile)
        if (snippetText.isBlank()) {
            notify("No valid snippet could be extracted.", NotificationType.WARNING)
            return
        }
        val finalSnippet = snippetText.withClipCraftHeaders()
        ClipCraftSnippetManager.getInstance().addSnippet(finalSnippet)
        notify("Snippet added successfully.", NotificationType.INFORMATION)
    }

    /**
     * Simplified function that returns either the selected text
     * or the source of the enclosing method if no selection exists.
     */
    private fun Editor.extractSnippetOrMethod(psiFile: PsiFile): String {
        return selectionModel.selectedText
            ?.takeIf { it.isNotBlank() }
            ?: caretModel.offset.let { offset ->
                psiFile.findElementAt(offset)
                    ?.let { element ->
                        (UastFacade.convertElementWithParent(element, UMethod::class.java) as? UMethod)
                            ?.sourcePsi
                            ?.text
                    }
            }.orEmpty()
    }

    /**
     * Inserts user-defined ClipCraft header and footer around snippet text.
     */
    private fun String.withClipCraftHeaders(): String {
        val settings = ClipCraftSettings.getInstance()
        val header = settings.getHeader().trim()
        val footer = settings.getFooter().trim()
        return buildString {
            if (header.isNotEmpty()) appendLine(header).appendLine()
            append(this@withClipCraftHeaders)
            if (footer.isNotEmpty()) {
                appendLine().appendLine(footer)
            }
        }
    }

    private fun notify(message: String, type: NotificationType) {
        Notifications.Bus.notify(
            Notification("ClipCraft", "ClipCraft Snippet Extraction", message, type),
        )
    }
}
