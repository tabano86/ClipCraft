package com.clipcraft

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager

object ClipCraftUtil {
    fun resolveFileType(languageHint: String): FileType {
        // Rough mapping for demonstration
        return when (languageHint.lowercase()) {
            "java" -> FileTypeManager.getInstance().findFileTypeByName("JAVA")!!
            "kotlin" -> FileTypeManager.getInstance().findFileTypeByName("Kotlin")!!
            "python" -> FileTypeManager.getInstance().findFileTypeByName("Python")!!
            "typescript" -> FileTypeManager.getInstance().findFileTypeByName("TypeScript")!!
            "javascript" -> FileTypeManager.getInstance().findFileTypeByName("JavaScript")!!
            "html" -> FileTypeManager.getInstance().findFileTypeByName("HTML")!!
            "xml" -> FileTypeManager.getInstance().findFileTypeByName("XML")!!
            "markdown" -> FileTypeManager.getInstance().findFileTypeByName("Markdown")!!
            else -> FileTypeManager.getInstance().findFileTypeByName("TEXT")!!
        }
    }
}
