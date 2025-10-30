package com.clipcraft.services

import com.clipcraft.model.ExportOptions
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.time.LocalDateTime
import java.util.ArrayDeque

object EnhancedFileProcessingService {

    fun processFiles(
        project: Project,
        initialSelection: List<VirtualFile>,
        options: ExportOptions,
        projectBasePath: Path,
        indicator: ProgressIndicator
    ): FormattedOutput {
        indicator.text = "Collecting files..."
        val distinctFiles = collectDistinctFiles(initialSelection)

        indicator.text = "Applying filters..."
        val filteredFiles = applyFilters(distinctFiles, options, projectBasePath, indicator)

        indicator.text = "Processing file contents..."
        val entries = processFileContents(filteredFiles, options, projectBasePath, indicator)

        val totalBytes = entries.sumOf { it.byteSize }
        val totalTokens = entries.sumOf { TokenEstimator.estimateTokens(it.content) }

        val gitInfo = if (options.includeGitInfo) {
            GitInfoExtractor.extractGitInfo(project)
        } else null

        val metadata = ExportMetadata(
            filesProcessed = entries.size,
            filesSkipped = filteredFiles.size - entries.size,
            totalBytes = totalBytes,
            estimatedTokens = totalTokens,
            exportTime = LocalDateTime.now(),
            projectName = project.name,
            gitBranch = gitInfo?.branch,
            gitCommit = gitInfo?.commitShort
        )

        indicator.text = "Formatting output..."

        if (options.enableChunking && totalTokens > options.maxTokens) {
            return handleChunkedOutput(entries, options, metadata, projectBasePath, indicator)
        }

        return OutputFormatter.format(entries, options, metadata)
    }

    private fun collectDistinctFiles(initialSelection: List<VirtualFile>): List<VirtualFile> {
        val collectedFiles = mutableSetOf<VirtualFile>()
        val filesToProcess = ArrayDeque(initialSelection.filterNotNull())

        while (filesToProcess.isNotEmpty()) {
            val file = filesToProcess.pop()
            if (file.isDirectory) {
                file.children?.let { filesToProcess.addAll(it) }
            } else {
                collectedFiles.add(file)
            }
        }

        return collectedFiles.toList()
    }

    private fun applyFilters(
        files: List<VirtualFile>,
        options: ExportOptions,
        projectBasePath: Path,
        indicator: ProgressIndicator
    ): List<VirtualFile> {
        val includeMatchers = createMatchers(options.includeGlobs)
        val excludeMatchers = createMatchers(options.excludeGlobs)
        val gitignorePatterns = if (options.respectGitignore) {
            loadGitignorePatterns(projectBasePath)
        } else emptyList()

        return files.filter { file ->
            indicator.checkCanceled()

            val nioPath = file.toNioPath()
            val relativePath = try {
                projectBasePath.relativize(nioPath)
            } catch (e: Exception) {
                return@filter false
            }

            // Glob filtering
            if (!isMatch(relativePath, includeMatchers, excludeMatchers)) {
                return@filter false
            }

            // Gitignore filtering
            if (gitignorePatterns.isNotEmpty() && matchesGitignore(relativePath, gitignorePatterns)) {
                return@filter false
            }

            // Regex filtering
            if (options.useRegexFiltering && options.regexPattern.isNotEmpty()) {
                try {
                    val regex = Regex(options.regexPattern)
                    if (!regex.containsMatchIn(relativePath.toString())) {
                        return@filter false
                    }
                } catch (e: Exception) {
                    // Invalid regex, skip this filter
                }
            }

            // Size filtering
            if (file.length < options.minFileSizeBytes || file.length > options.maxFileSizeBytes) {
                return@filter false
            }

            // Date filtering
            options.includeOnlyModifiedAfter?.let { afterTimestamp ->
                if (file.timeStamp < afterTimestamp) {
                    return@filter false
                }
            }

            true
        }
    }

    private fun processFileContents(
        files: List<VirtualFile>,
        options: ExportOptions,
        projectBasePath: Path,
        indicator: ProgressIndicator
    ): List<FileEntry> {
        val entries = mutableListOf<FileEntry>()

        files.forEachIndexed { index, file ->
            indicator.checkCanceled()
            indicator.fraction = (index + 1).toDouble() / files.size
            indicator.text2 = file.name

            if (file.fileType.isBinary) {
                return@forEachIndexed
            }

            val fileSizeKb = file.length / 1024
            if (fileSizeKb > options.maxFileSizeKb) {
                return@forEachIndexed
            }

            try {
                var content = String(file.contentsToByteArray(), file.charset)
                val lineCount = content.lines().size

                // Line count filtering
                if (lineCount < options.minLineCount || lineCount > options.maxLineCount) {
                    return@forEachIndexed
                }

                // Secret detection and masking
                if (options.detectSecrets) {
                    val secrets = SecretDetector.detectSecrets(content)
                    if (secrets.isNotEmpty() && options.maskSecrets) {
                        content = SecretDetector.maskSecrets(content)
                    }
                }

                // Content extraction
                if (options.extractTodos) {
                    content = enhanceWithTodos(content)
                }

                if (options.extractDocumentation) {
                    content = enhanceWithDocumentation(content)
                }

                val relativePath = projectBasePath.relativize(file.toNioPath())
                val language = detectLanguage(file)

                entries.add(
                    FileEntry(
                        file = file,
                        relativePath = relativePath,
                        content = content,
                        language = language,
                        lineCount = lineCount,
                        byteSize = file.length
                    )
                )

            } catch (e: IOException) {
                // Skip files that can't be read
            }
        }

        return entries
    }

    private fun handleChunkedOutput(
        entries: List<FileEntry>,
        options: ExportOptions,
        metadata: ExportMetadata,
        projectBasePath: Path,
        indicator: ProgressIndicator
    ): FormattedOutput {
        indicator.text = "Creating chunks..."

        val chunks = ChunkingService.chunkFiles(
            entries.map { it.file },
            options.chunkStrategy,
            options.maxTokens,
            projectBasePath
        )

        val chunkOutputs = chunks.mapIndexed { index, chunk ->
            indicator.text = "Formatting chunk ${index + 1}/${chunks.size}..."

            val chunkEntries = entries.filter { it.file in chunk.files }
            val chunkMetadata = metadata.copy(
                filesProcessed = chunkEntries.size,
                estimatedTokens = chunk.estimatedTokens
            )

            val chunkHeader = "# Chunk ${chunk.chunkIndex + 1} of ${chunk.totalChunks}\n\n"
            val formatted = OutputFormatter.format(chunkEntries, options, chunkMetadata)

            chunkHeader + formatted.content
        }

        val combinedContent = chunkOutputs.joinToString("\n\n${"=".repeat(80)}\n\n")

        return FormattedOutput(
            content = "# Multi-Chunk Export\n\n" +
                    "This export has been split into ${chunks.size} chunks due to size constraints.\n\n" +
                    combinedContent,
            metadata = metadata.copy(
                filesProcessed = entries.size
            )
        )
    }

    private fun createMatchers(globs: String): List<PathMatcher> {
        return globs.lines()
            .filter { it.isNotBlank() }
            .map { FileSystems.getDefault().getPathMatcher("glob:$it") }
    }

    private fun isMatch(path: Path, includes: List<PathMatcher>, excludes: List<PathMatcher>): Boolean {
        if (excludes.any { it.matches(path) }) return false
        return includes.isEmpty() || includes.any { it.matches(path) }
    }

    private fun loadGitignorePatterns(projectBasePath: Path): List<Regex> {
        val gitignoreFile = projectBasePath.resolve(".gitignore").toFile()
        if (!gitignoreFile.exists()) return emptyList()

        return try {
            gitignoreFile.readLines()
                .filter { it.isNotBlank() && !it.startsWith("#") }
                .mapNotNull { pattern ->
                    try {
                        val regexPattern = pattern
                            .replace(".", "\\.")
                            .replace("*", ".*")
                            .replace("?", ".")
                        Regex(regexPattern)
                    } catch (e: Exception) {
                        null
                    }
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun matchesGitignore(path: Path, patterns: List<Regex>): Boolean {
        val pathString = path.toString().replace("\\", "/")
        return patterns.any { it.matches(pathString) }
    }

    private fun detectLanguage(file: VirtualFile): String {
        val extension = file.extension?.lowercase() ?: return "text"

        return when (extension) {
            "kt" -> "kotlin"
            "java" -> "java"
            "py" -> "python"
            "js" -> "javascript"
            "ts" -> "typescript"
            "tsx" -> "typescript"
            "jsx" -> "javascript"
            "go" -> "go"
            "rs" -> "rust"
            "c" -> "c"
            "cpp", "cc", "cxx" -> "cpp"
            "h", "hpp" -> "cpp"
            "cs" -> "csharp"
            "rb" -> "ruby"
            "php" -> "php"
            "swift" -> "swift"
            "scala" -> "scala"
            "r" -> "r"
            "sql" -> "sql"
            "sh", "bash" -> "bash"
            "ps1" -> "powershell"
            "xml" -> "xml"
            "html" -> "html"
            "css" -> "css"
            "scss" -> "scss"
            "sass" -> "sass"
            "json" -> "json"
            "yaml", "yml" -> "yaml"
            "toml" -> "toml"
            "md" -> "markdown"
            "tex" -> "latex"
            "docker" -> "dockerfile"
            else -> file.fileType.name.lowercase().replace(" ", "")
        }
    }

    private fun enhanceWithTodos(content: String): String {
        val todos = mutableListOf<String>()
        content.lines().forEachIndexed { index, line ->
            if (line.contains(Regex("(TODO|FIXME|XXX|HACK|NOTE):"))) {
                todos.add("Line ${index + 1}: ${line.trim()}")
            }
        }

        return if (todos.isNotEmpty()) {
            "// Extracted TODOs:\n${todos.joinToString("\n")}\n\n$content"
        } else content
    }

    private fun enhanceWithDocumentation(content: String): String {
        // Extract KDoc, JavaDoc, or similar documentation
        val docPattern = Regex("""/\*\*[\s\S]*?\*/""")
        val docs = docPattern.findAll(content).map { it.value }.toList()

        return if (docs.isNotEmpty()) {
            "// Extracted Documentation:\n${docs.joinToString("\n\n")}\n\n$content"
        } else content
    }
}
