package com.clipcraft.model

data class SettingsState(
    // Basic filtering
    var includeGlobs: String = """
        **/*.kt
        **/*.java
        **/*.go
        **/*.py
        **/*.js
        **/*.ts
        **/*.tsx
        **/*.jsx
        **/*.html
        **/*.css
        **/*.scss
        **/*.svelte
        **/*.vue
        **/*.md
        **/*.xml
        **/*.json
        **/*.yml
        **/*.yaml
        **/*.toml
        **/*.sh
        **/*.properties
        **/*.sql
        **/Dockerfile*
        **/*.Dockerfile*
        **/*docker-compose*.yml
        **/Makefile
        **/*.gradle.kts
        **/pom.xml
        **/pyproject.toml
        **/package.json
        **/tsconfig.json
    """.trimIndent(),
    var excludeGlobs: String = """
        **/build/**
        **/.gradle/**
        **/.idea/**
        **/.git/**
        **/node_modules/**
        **/.venv/**
        **/dist/**
        **/.next/**
        **/.svelte-kit/**
        **/.output/**
        **/.turbo/**
        **/target/**
        **/*__pycache__/**
        **/.pytest_cache/**
        **/*.class
        **/*.jar
        **/*.bin
        **/poetry.lock
        **/pnpm-lock.yaml
        **/yarn.lock
        **/package-lock.json
        **/*.pyc
        **/*.log*
        **/*.so
        **/*.dll
        **/*.exe
        **/*.dmg
        **/*.iso
        **/*.zip
        **/*.tar.gz
    """.trimIndent(),
    var maxFileSizeKb: Int = 2048,

    // Output options
    var defaultOutputFormat: String = "MARKDOWN",
    var includeLineNumbers: Boolean = false,
    var stripComments: Boolean = false,
    var includeMetadata: Boolean = true,
    var includeGitInfo: Boolean = false,
    var includeTableOfContents: Boolean = false,

    // Advanced options
    var respectGitignore: Boolean = true,
    var detectSecrets: Boolean = true,
    var maskSecrets: Boolean = true,
    var groupByDirectory: Boolean = true,

    // Chunking options
    var enableChunking: Boolean = false,
    var maxTokens: Int = 100000,
    var chunkStrategy: String = "BY_SIZE"
) {
    fun toExportOptions(): ExportOptions {
        return ExportOptions(
            includeGlobs = includeGlobs,
            excludeGlobs = excludeGlobs,
            maxFileSizeKb = maxFileSizeKb,
            outputFormat = try {
                OutputFormat.valueOf(defaultOutputFormat)
            } catch (e: Exception) {
                OutputFormat.MARKDOWN
            },
            includeLineNumbers = includeLineNumbers,
            stripComments = stripComments,
            includeMetadata = includeMetadata,
            includeGitInfo = includeGitInfo,
            includeTableOfContents = includeTableOfContents,
            respectGitignore = respectGitignore,
            detectSecrets = detectSecrets,
            maskSecrets = maskSecrets,
            groupByDirectory = groupByDirectory,
            enableChunking = enableChunking,
            maxTokens = maxTokens,
            chunkStrategy = try {
                ChunkStrategy.valueOf(chunkStrategy)
            } catch (e: Exception) {
                ChunkStrategy.BY_SIZE
            }
        )
    }
}