package com.clipcraft

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager

/**
 * Demonstrates how we might unify detection of file types in a single place.
 */
object ClipCraftUtil {
    fun resolveFileType(languageHint: String): FileType {
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
