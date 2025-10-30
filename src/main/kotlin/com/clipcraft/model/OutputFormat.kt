package com.clipcraft.model

enum class OutputFormat(val displayName: String, val fileExtension: String) {
    MARKDOWN("Markdown", "md"),
    MARKDOWN_WITH_TOC("Markdown with Table of Contents", "md"),
    XML("XML", "xml"),
    JSON("JSON", "json"),
    PLAIN_TEXT("Plain Text", "txt"),
    HTML("HTML", "html"),
    CLAUDE_OPTIMIZED("Claude-Optimized", "md"),
    CHATGPT_OPTIMIZED("ChatGPT-Optimized", "md"),
    GEMINI_OPTIMIZED("Gemini-Optimized", "md"),
}
