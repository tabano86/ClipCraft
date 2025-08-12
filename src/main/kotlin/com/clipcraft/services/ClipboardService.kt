package com.clipcraft.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ide.CopyPasteManager
import java.awt.datatransfer.StringSelection

object ClipboardService {
    fun copyToClipboard(content: String) {
        ApplicationManager.getApplication().invokeLater {
            CopyPasteManager.getInstance().setContents(StringSelection(content))
        }
    }
}