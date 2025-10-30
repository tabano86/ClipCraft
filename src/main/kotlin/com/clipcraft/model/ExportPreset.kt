package com.clipcraft.model

data class ExportPreset(
    val name: String,
    val description: String,
    val includeGlobs: String,
    val excludeGlobs: String,
    val outputFormat: OutputFormat,
    val maxFileSizeKb: Int = 2048,
    val includeLineNumbers: Boolean = false,
    val stripComments: Boolean = false,
    val includeMetadata: Boolean = true
) {
    companion object {
        val AI_LLM_PRESET = ExportPreset(
            name = "AI/LLM Context",
            description = "Optimized for AI agents with token limits",
            includeGlobs = """
                **/*.kt
                **/*.java
                **/*.py
                **/*.js
                **/*.ts
                **/*.tsx
                **/*.jsx
                **/*.go
                **/*.rs
                **/*.md
                **/README*
                **/package.json
                **/pom.xml
                **/build.gradle*
                **/Cargo.toml
            """.trimIndent(),
            excludeGlobs = """
                **/build/**
                **/dist/**
                **/node_modules/**
                **/.gradle/**
                **/.git/**
                **/target/**
                **/*.min.js
                **/*.bundle.js
                **/*.lock
            """.trimIndent(),
            outputFormat = OutputFormat.CLAUDE_OPTIMIZED,
            maxFileSizeKb = 512,
            stripComments = false,
            includeMetadata = true
        )

        val DOCUMENTATION_PRESET = ExportPreset(
            name = "Documentation",
            description = "Export documentation and markdown files",
            includeGlobs = """
                **/*.md
                **/*.txt
                **/*.adoc
                **/*.rst
                **/README*
                **/CHANGELOG*
                **/LICENSE*
                **/docs/**
            """.trimIndent(),
            excludeGlobs = """
                **/node_modules/**
                **/build/**
                **/dist/**
            """.trimIndent(),
            outputFormat = OutputFormat.MARKDOWN_WITH_TOC,
            maxFileSizeKb = 4096
        )

        val CODE_REVIEW_PRESET = ExportPreset(
            name = "Code Review",
            description = "Export code files for review",
            includeGlobs = """
                **/*.kt
                **/*.java
                **/*.py
                **/*.js
                **/*.ts
                **/*.tsx
                **/*.jsx
                **/*.go
                **/*.rs
                **/*.c
                **/*.cpp
                **/*.h
                **/*.cs
            """.trimIndent(),
            excludeGlobs = """
                **/build/**
                **/dist/**
                **/node_modules/**
                **/.gradle/**
                **/target/**
                **/*Test.kt
                **/*Test.java
                **/*_test.py
                **/*.test.ts
                **/*.test.js
                **/*.spec.ts
                **/*.spec.js
            """.trimIndent(),
            outputFormat = OutputFormat.MARKDOWN,
            maxFileSizeKb = 2048,
            includeLineNumbers = true
        )

        val FULL_PROJECT_PRESET = ExportPreset(
            name = "Full Project",
            description = "Export entire project structure",
            includeGlobs = "**/*",
            excludeGlobs = """
                **/build/**
                **/.gradle/**
                **/.idea/**
                **/.git/**
                **/node_modules/**
                **/.venv/**
                **/dist/**
                **/*.class
                **/*.jar
                **/*.exe
                **/*.dll
                **/*.so
            """.trimIndent(),
            outputFormat = OutputFormat.MARKDOWN_WITH_TOC,
            maxFileSizeKb = 4096
        )

        val TEST_FILES_PRESET = ExportPreset(
            name = "Test Files",
            description = "Export test files only",
            includeGlobs = """
                **/*Test.kt
                **/*Test.java
                **/*_test.py
                **/*.test.ts
                **/*.test.js
                **/*.spec.ts
                **/*.spec.js
                **/test/**
                **/tests/**
            """.trimIndent(),
            excludeGlobs = """
                **/build/**
                **/node_modules/**
            """.trimIndent(),
            outputFormat = OutputFormat.MARKDOWN,
            maxFileSizeKb = 2048
        )

        fun getAllPresets() = listOf(
            AI_LLM_PRESET,
            DOCUMENTATION_PRESET,
            CODE_REVIEW_PRESET,
            FULL_PROJECT_PRESET,
            TEST_FILES_PRESET
        )
    }
}
