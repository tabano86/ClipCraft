package com.clipcraft.actions

import com.clipcraft.model.Snippet
import com.clipcraft.services.SnippetsManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/**
 * Copies the snippet queue plus a GPT prompt to the clipboard.
 * Demonstrates GPT integration stubs.
 */
class ClipCraftGPTAction : AnAction("ClipCraft: Copy & Prompt GPT") {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val snippetMgr = project.getService(SnippetsManager::class.java) ?: return
        val allSnippets: List<Snippet> = snippetMgr.getAllSnippets()
        if (allSnippets.isEmpty()) {
            Messages.showInfoMessage(project, "No snippets in the queue.", "ClipCraft")
            return
        }
        // Combine snippet content into a single prompt
        val combined = allSnippets.joinToString("\n\n") { it.content }
        val prompt = "Please analyze the following code:\n\n$combined"

        // Copy the prompt to the clipboard
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(StringSelection(prompt), null)

        Messages.showInfoMessage(
            project,
            "Snippets + GPT prompt copied to clipboard!",
            "ClipCraft"
        )
    }
}
