package com.clipcraft.services

import com.clipcraft.model.ExportOptions
import com.clipcraft.services.professional.ProfessionalFormatter
import com.clipcraft.services.professional.ProfessionalGitService
import com.clipcraft.services.professional.ProfessionalTokenEstimator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.springframework.util.AntPathMatcher
import java.io.IOException
import java.nio.file.Path
import java.util.ArrayDeque

/**
 * Streamlined file processing using professional services.
 * No more custom implementations - all using battle-tested libraries.
 */
object EnhancedFileProcessingService {

    private val pathMatcher = AntPathMatcher() // Spring's rock-solid path matcher

    fun processFiles(
        project: Project,
        initialSelection: List<VirtualFile>,
        options: ExportOptions,
        projectBasePath: Path,
        indicator: ProgressIndicator
    ): ProfessionalFormatter.FormattedOutput {

        indicator.text = "Collecting files..."
        val distinctFiles = collectDistinctFiles(initialSelection)

        indicator.text = "Applying filters..."
        val filteredFiles = applyFilters(distinctFiles, options, projectBasePath, indicator)

        indicator.text = "Processing file contents..."
        val fileInfos = processFileContents(filteredFiles, options, projectBasePath, indicator)

        val totalBytes = fileInfos.sumOf { it.byteSize }
        val totalTokens = fileInfos.sumOf { it.tokens ?: 0 }

        val gitInfo = if (options.includeGitInfo) {
            ProfessionalGitService.getGitInfo(project)
        } else null

        val metadata = ProfessionalFormatter.MetadataInfo(
            exportTime = ProfessionalFormatter.getCurrentTimestamp(),
            projectName = project.name,
            totalFiles = fileInfos.size,
            totalBytes = totalBytes,
            estimatedTokens = totalTokens,
            gitBranch = gitInfo?.branch,
            gitCommit = gitInfo?.commitHashShort
        )

        val exportData = ProfessionalFormatter.ExportData(
            metadata = metadata,
            files = fileInfos
        )

        indicator.text = "Formatting output..."

        // Use professional formatter based on output format
        val content = when (options.outputFormat) {
            com.clipcraft.model.OutputFormat.JSON -> ProfessionalFormatter.toJson(exportData)
            com.clipcraft.model.OutputFormat.XML -> ProfessionalFormatter.toXml(exportData)
            com.clipcraft.model.OutputFormat.HTML -> ProfessionalFormatter.toHtml(exportData, options.includeMetadata)
            com.clipcraft.model.OutputFormat.PLAIN_TEXT -> ProfessionalFormatter.toPlainText(exportData, options.includeMetadata)
            com.clipcraft.model.OutputFormat.MARKDOWN_WITH_TOC -> ProfessionalFormatter.toMarkdown(exportData, options.includeMetadata, includeToc = true)
            else -> ProfessionalFormatter.toMarkdown(exportData, options.includeMetadata, includeToc = false)
        }

        val formattedMetadata = ProfessionalFormatter.FormattedMetadata(
            filesProcessed = fileInfos.size,
            filesSkipped = distinctFiles.size - fileInfos.size,
            estimatedTokens = totalTokens
        )

        return ProfessionalFormatter.FormattedOutput(content, formattedMetadata)
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

        val includePatterns = options.includeGlobs.lines().filter { it.isNotBlank() }
        val excludePatterns = options.excludeGlobs.lines().filter { it.isNotBlank() }

        return files.filter { file ->
            indicator.checkCanceled()

            val nioPath = file.toNioPath()
            val relativePath = try {
                projectBasePath.relativize(nioPath).toString().replace("\\", "/")
            } catch (e: Exception) {
                return@filter false
            }

            // Use Spring's AntPathMatcher (battle-tested)
            if (excludePatterns.any { pathMatcher.match(it, relativePath) }) {
                return@filter false
            }

            if (includePatterns.isNotEmpty() && includePatterns.none { pathMatcher.match(it, relativePath) }) {
                return@filter false
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
    ): List<ProfessionalFormatter.FileInfo> {

        val fileInfos = mutableListOf<ProfessionalFormatter.FileInfo>()

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
                val content = String(file.contentsToByteArray(), file.charset)
                val lineCount = content.lines().size

                // Line count filtering
                if (lineCount < options.minLineCount || lineCount > options.maxLineCount) {
                    return@forEachIndexed
                }

                val relativePath = projectBasePath.relativize(file.toNioPath()).toString()
                val language = detectLanguage(file)

                // Use professional token estimator (tiktoken)
                val tokens = ProfessionalTokenEstimator.countTokens(content)

                fileInfos.add(
                    ProfessionalFormatter.FileInfo(
                        path = relativePath,
                        language = language,
                        content = content,
                        lineCount = lineCount,
                        byteSize = file.length,
                        tokens = tokens
                    )
                )

            } catch (e: IOException) {
                // Skip files that can't be read
            }
        }

        return fileInfos
    }

    private fun detectLanguage(file: VirtualFile): String {
        val extension = file.extension?.lowercase() ?: return "text"

        return when (extension) {
            "kt" -> "kotlin"
            "java" -> "java"
            "py" -> "python"
            "js" -> "javascript"
            "ts", "tsx" -> "typescript"
            "jsx" -> "javascript"
            "go" -> "go"
            "rs" -> "rust"
            "c" -> "c"
            "cpp", "cc", "cxx", "h", "hpp" -> "cpp"
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
            "scss", "sass" -> "scss"
            "json" -> "json"
            "yaml", "yml" -> "yaml"
            "toml" -> "toml"
            "md" -> "markdown"
            "tex" -> "latex"
            "docker" -> "dockerfile"
            else -> file.fileType.name.lowercase().replace(" ", "")
        }
    }
}
