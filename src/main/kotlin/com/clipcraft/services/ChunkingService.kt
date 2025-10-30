package com.clipcraft.services

import com.clipcraft.model.ChunkStrategy
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Path

data class FileChunk(
    val files: List<VirtualFile>,
    val estimatedTokens: Int,
    val estimatedBytes: Long,
    val chunkIndex: Int,
    val totalChunks: Int
)

object ChunkingService {

    fun chunkFiles(
        files: List<VirtualFile>,
        strategy: ChunkStrategy,
        maxTokens: Int,
        projectBasePath: Path
    ): List<FileChunk> {
        return when (strategy) {
            ChunkStrategy.BY_SIZE -> chunkBySize(files, maxTokens)
            ChunkStrategy.BY_FILE_COUNT -> chunkByFileCount(files, 50)
            ChunkStrategy.BY_DIRECTORY -> chunkByDirectory(files, projectBasePath)
            ChunkStrategy.BY_FILE_TYPE -> chunkByFileType(files)
            ChunkStrategy.SMART -> smartChunk(files, maxTokens, projectBasePath)
        }
    }

    private fun chunkBySize(files: List<VirtualFile>, maxTokens: Int): List<FileChunk> {
        val chunks = mutableListOf<FileChunk>()
        var currentChunk = mutableListOf<VirtualFile>()
        var currentTokens = 0

        for (file in files) {
            if (file.fileType.isBinary) continue

            val content = try {
                String(file.contentsToByteArray(), file.charset)
            } catch (e: Exception) {
                continue
            }

            val fileTokens = TokenEstimator.estimateTokens(content)

            if (currentTokens + fileTokens > maxTokens && currentChunk.isNotEmpty()) {
                chunks.add(
                    FileChunk(
                        currentChunk.toList(),
                        currentTokens,
                        currentChunk.sumOf { it.length },
                        chunks.size,
                        0
                    )
                )
                currentChunk = mutableListOf()
                currentTokens = 0
            }

            currentChunk.add(file)
            currentTokens += fileTokens
        }

        if (currentChunk.isNotEmpty()) {
            chunks.add(
                FileChunk(
                    currentChunk,
                    currentTokens,
                    currentChunk.sumOf { it.length },
                    chunks.size,
                    0
                )
            )
        }

        // Update totalChunks
        return chunks.mapIndexed { index, chunk ->
            chunk.copy(chunkIndex = index, totalChunks = chunks.size)
        }
    }

    private fun chunkByFileCount(files: List<VirtualFile>, filesPerChunk: Int): List<FileChunk> {
        return files.chunked(filesPerChunk).mapIndexed { index, chunkFiles ->
            FileChunk(
                chunkFiles,
                chunkFiles.sumOf { estimateFileTokens(it) },
                chunkFiles.sumOf { it.length },
                index,
                (files.size + filesPerChunk - 1) / filesPerChunk
            )
        }
    }

    private fun chunkByDirectory(files: List<VirtualFile>, projectBasePath: Path): List<FileChunk> {
        val groupedByDir = files.groupBy { file ->
            file.parent?.path ?: ""
        }

        return groupedByDir.values.mapIndexed { index, dirFiles ->
            FileChunk(
                dirFiles,
                dirFiles.sumOf { estimateFileTokens(it) },
                dirFiles.sumOf { it.length },
                index,
                groupedByDir.size
            )
        }
    }

    private fun chunkByFileType(files: List<VirtualFile>): List<FileChunk> {
        val groupedByType = files.groupBy { it.extension ?: "no-extension" }

        return groupedByType.values.mapIndexed { index, typeFiles ->
            FileChunk(
                typeFiles,
                typeFiles.sumOf { estimateFileTokens(it) },
                typeFiles.sumOf { it.length },
                index,
                groupedByType.size
            )
        }
    }

    private fun smartChunk(
        files: List<VirtualFile>,
        maxTokens: Int,
        projectBasePath: Path
    ): List<FileChunk> {
        // Smart chunking: Group related files together (same directory, similar names)
        // while respecting token limits
        val chunks = mutableListOf<FileChunk>()
        val processed = mutableSetOf<VirtualFile>()

        for (file in files) {
            if (file in processed) continue

            val relatedFiles = mutableListOf(file)
            var currentTokens = estimateFileTokens(file)
            processed.add(file)

            // Find related files in the same directory
            val sameDir = files.filter {
                it !in processed &&
                        it.parent?.path == file.parent?.path &&
                        currentTokens + estimateFileTokens(it) <= maxTokens
            }

            for (related in sameDir) {
                relatedFiles.add(related)
                currentTokens += estimateFileTokens(related)
                processed.add(related)
            }

            chunks.add(
                FileChunk(
                    relatedFiles,
                    currentTokens,
                    relatedFiles.sumOf { it.length },
                    chunks.size,
                    0
                )
            )
        }

        return chunks.mapIndexed { index, chunk ->
            chunk.copy(chunkIndex = index, totalChunks = chunks.size)
        }
    }

    private fun estimateFileTokens(file: VirtualFile): Int {
        if (file.fileType.isBinary) return 0

        return try {
            val content = String(file.contentsToByteArray(), file.charset)
            TokenEstimator.estimateTokens(content)
        } catch (e: Exception) {
            0
        }
    }
}
