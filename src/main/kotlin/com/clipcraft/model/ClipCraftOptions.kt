package com.clipcraft.model

data class ClipCraftOptions(
    var includeLineNumbers: Boolean = false,
    var outputFormat: OutputFormat = OutputFormat.MARKDOWN,
    var chunkSize: Int = 4000,
    var compressionMode: CompressionMode = CompressionMode.NONE,
    var removeImports: Boolean = false,
    var removeComments: Boolean = false,
    var trimWhitespace: Boolean = true,
    var useGitIgnore: Boolean = false,
    var themeMode: ThemeMode = ThemeMode.LIGHT,
    var gptTemplates: MutableList<GPTPromptTemplate> = mutableListOf(
        GPTPromptTemplate("ExplainThisCode", "Explain this code"),
        GPTPromptTemplate("OptimizeThisSnippet", "Optimize this snippet")
    )
)
