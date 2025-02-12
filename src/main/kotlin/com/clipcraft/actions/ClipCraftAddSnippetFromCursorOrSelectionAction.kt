package com.clipcraft.actions

import com.clipcraft.services.ClipCraftSettings
import com.clipcraft.services.SnippetsManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import org.jetbrains.uast.UastFacade
import org.jetbrains.uast.UMethod

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
        SnippetsManager.getInstance(event.project!!).addSnippet(
            com.clipcraft.model.Snippet(
                filePath = "InMemory",
                relativePath = null,
                fileName = "Snippet",
                fileSizeBytes = finalSnippet.length.toLong(),
                lastModified = System.currentTimeMillis(),
                content = finalSnippet
            )
        )
        notify("Snippet added successfully.", NotificationType.INFORMATION)
    }

    private fun Editor.extractSnippetOrMethod(psiFile: PsiFile): String {
        return selectionModel.selectedText ?: caretModel.offset.let { offset ->
            psiFile.findElementAt(offset)?.let { element ->
                (UastFacade.convertElementWithParent(element, UMethod::class.java) as? UMethod)
                    ?.sourcePsi?.text
            }
        }.orEmpty()
    }

    private fun String.withClipCraftHeaders(): String {
        val settings = ClipCraftSettings.getInstance()
        val prefix = settings.getSnippetPrefix().trim()
        val suffix = settings.getSnippetSuffix().trim()
        return buildString {
            if (prefix.isNotEmpty()) appendLine(prefix).appendLine()
            append(this@withClipCraftHeaders)
            if (suffix.isNotEmpty()) appendLine().appendLine(suffix)
        }
    }

    private fun notify(message: String, type: NotificationType) {
        Notifications.Bus.notify(Notification("ClipCraft", "ClipCraft Snippet Extraction", message, type))
    }
}
