package com.clipcraft.util

import com.clipcraft.model.Snippet

object MetadataFormatter {
    /**
     * Formats a metadata block using a template and a snippet's details.
     */
    fun formatMetadata(template: String, snippet: Snippet): String {
        return template
            .replace("{fileName}", snippet.fileName)
            .replace("{filePath}", snippet.filePath)
            .replace("{size}", snippet.fileSizeBytes.toString())
            .replace("{modified}", snippet.lastModified.toString())
            .replace("{id}", snippet.id)
            .replace("{relativePath}", snippet.relativePath ?: snippet.fileName)
    }
}
