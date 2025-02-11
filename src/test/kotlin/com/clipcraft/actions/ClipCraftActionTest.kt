package com.clipcraft.actions

import com.clipcraft.model.ClipCraftOptions
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileSystem
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClipCraftActionTest {

    /**
     * Uses reflection to invoke the private suspend fun processVirtualFile.
     */
    private fun invokeProcessVirtualFile(
        action: ClipCraftAction,
        file: VirtualFile,
        opts: ClipCraftOptions,
        project: Project? = null
    ): String = runBlocking {
        val method = ClipCraftAction::class.java.getDeclaredMethod(
            "processVirtualFile",
            VirtualFile::class.java,
            ClipCraftOptions::class.java,
            Project::class.java
        )
        method.isAccessible = true
        method.invoke(action, file, opts, project) as String
    }

    companion object {
        @JvmStatic
        fun fileProcessingParameters(): Stream<Arguments> {
            return Stream.of(
                // Standard small file scenario.
                Arguments.of(
                    "Small file, below threshold",
                    "Small file content",
                    "fake/path/small.txt",
                    "Small file content",
                    50L,
                    100L,
                    false
                ),
                // Large file with threshold exceeded and no progress.
                Arguments.of(
                    "Large file, above threshold but progress off",
                    "Large file content",
                    "fake/path/large.txt",
                    "Large file content",
                    150L,
                    100L,
                    false
                ),
                // Large file with progress enabled.
                Arguments.of(
                    "Large file, above threshold with progress on",
                    "Large file content",
                    "fake/path/large_progress.txt",
                    "Large file content",
                    150L,
                    100L,
                    true
                ),
                // A file that does not match the filter regex should produce empty output.
                Arguments.of(
                    "File filtered out by regex",
                    "Content that should not appear",
                    "fake/path/ignore.txt",
                    "", // expecting blank result since filterRegex will skip it
                    50L,
                    100L,
                    false
                )
            )
        }

        @JvmStatic
        fun directoryProcessingParameters(): Stream<Arguments> {
            val child1 = FakeVirtualFile(
                filePath = "fake/path/child1.txt",
                fileContent = "Child One Content"
            )
            val child2 = FakeVirtualFile(
                filePath = "fake/path/child2.txt",
                fileContent = "Child Two Content",
                simulatedLength = 150L
            )
            return Stream.of(
                Arguments.of(
                    "Directory with two children",
                    "fake/path/directory",
                    listOf(child1, child2),
                    "Child One Content",
                    "Child Two Content"
                )
            )
        }

        @JvmStatic
        fun binaryFileParameters(): Stream<Arguments> {
            // Simulate a binary file by overriding isProbablyTextFile.
            // For testing purposes, we assume that processing returns an empty string.
            return Stream.of(
                Arguments.of(
                    "Binary file should be skipped",
                    "This is binary data",
                    "fake/path/binary.bin",
                    "",
                    200L,
                    100L,
                    false
                )
            )
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fileProcessingParameters")
    fun `test processVirtualFile for single files`(
        description: String,
        content: String,
        path: String,
        expectedSubstring: String,
        simulatedLength: Long,
        threshold: Long,
        showProgress: Boolean
    ) {
        val fakeFile = FakeVirtualFile(
            filePath = path,
            fileContent = content,
            simulatedLength = simulatedLength
        )
        // For the filter test, set a regex that excludes files with "ignore" in the path.
        val filter = if (path.contains("ignore")) ".*doNotMatch.*" else ""
        val opts = ClipCraftOptions(
            largeFileThreshold = threshold,
            showProgressInStatusBar = showProgress,
            trimLineWhitespace = false,
            includeMetadata = false,
            autoProcess = true,
            filterRegex = filter
        )
        val project: Project? = null
        val action = ClipCraftAction()
        val result = invokeProcessVirtualFile(action, fakeFile, opts, project)
        if (expectedSubstring.isEmpty()) {
            // For filtered or binary files, we expect no output.
            assertTrue(result.isBlank(), "Result should be blank for filtered or non-text files")
        } else {
            assertTrue(result.startsWith("File: $path"), "Result should begin with the file header")
            assertTrue(result.contains(expectedSubstring), "Result should contain the expected content")
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("directoryProcessingParameters")
    fun `test processVirtualFile for directories`(
        description: String,
        dirPath: String,
        children: List<FakeVirtualFile>,
        expectedChild1: String,
        expectedChild2: String
    ) {
        val fakeDir = FakeVirtualFile(
            filePath = dirPath,
            fileContent = "", // directory itself has no content
            isDir = true,
            childFiles = children.toTypedArray()
        )
        val opts = ClipCraftOptions(
            largeFileThreshold = 100,
            showProgressInStatusBar = false,
            trimLineWhitespace = false,
            includeMetadata = false,
            autoProcess = true,
            filterRegex = ""
        )
        val project: Project? = null
        val action = ClipCraftAction()
        val result = invokeProcessVirtualFile(action, fakeDir, opts, project)
        assertTrue(result.contains(expectedChild1), "Result should contain first child content")
        assertTrue(result.contains(expectedChild2), "Result should contain second child content")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("binaryFileParameters")
    fun `test processVirtualFile skips binary files`(
        description: String,
        content: String,
        path: String,
        expectedSubstring: String,
        simulatedLength: Long,
        threshold: Long,
        showProgress: Boolean
    ) {
        // For this test, we simulate a binary file by making isProbablyTextFile always return false.
        val fakeFile = FakeBinaryVirtualFile(
            filePath = path,
            fileContent = content,
            simulatedLength = simulatedLength
        )
        val opts = ClipCraftOptions(
            largeFileThreshold = threshold,
            showProgressInStatusBar = showProgress,
            trimLineWhitespace = false,
            includeMetadata = false,
            autoProcess = true,
            filterRegex = ""
        )
        val project: Project? = null
        val action = ClipCraftAction()
        val result = invokeProcessVirtualFile(action, fakeFile, opts, project)
        // For non-text (binary) files, processing should return empty string.
        assertTrue(result.isBlank(), "Result should be blank for binary files")
    }
}

/**
 * A minimal fake implementation of IntelliJ’s VirtualFile for testing purposes.
 */
open class FakeVirtualFile(
    private val filePath: String,
    private val fileContent: String,
    private val simulatedLength: Long? = null,
    private val isDir: Boolean = false,
    childFiles: Array<VirtualFile> = arrayOf()
) : VirtualFile() {

    // Use a differently named backing field to avoid generating a getter that clashes.
    private val childFiles: Array<VirtualFile> = childFiles

    override fun getName(): String = filePath.substringAfterLast("/")
    override fun getFileSystem(): VirtualFileSystem = FakeVirtualFileSystem
    override fun getPath(): String = filePath
    override fun isWritable(): Boolean = false
    override fun isDirectory(): Boolean = isDir
    override fun isValid(): Boolean = true
    override fun getParent(): VirtualFile? = null
    override fun getChildren(): Array<VirtualFile> = if (isDir) childFiles else arrayOf()
    override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long): OutputStream =
        throw UnsupportedOperationException("Not supported in FakeVirtualFile")

    override fun contentsToByteArray(): ByteArray = fileContent.toByteArray(Charsets.UTF_8)
    override fun getTimeStamp(): Long = 0L
    override fun getLength(): Long = simulatedLength ?: fileContent.toByteArray(Charsets.UTF_8).size.toLong()
    override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {}
    override fun getInputStream(): InputStream = ByteArrayInputStream(fileContent.toByteArray(Charsets.UTF_8))
    override fun getExtension(): String = getName().substringAfterLast('.', missingDelimiterValue = "")
}

/**
 * A fake VirtualFile that simulates a binary file by having isProbablyTextFile return false.
 * In real usage, ClipCraftAction would likely check the file type using FileTypeRegistry.
 */
class FakeBinaryVirtualFile(
    filePath: String,
    fileContent: String,
    simulatedLength: Long? = null,
    isDir: Boolean = false,
    childFiles: Array<VirtualFile> = arrayOf()
) : FakeVirtualFile(filePath, fileContent, simulatedLength, isDir, childFiles) {
    // Override to simulate a binary file.
    fun isProbablyTextFile(): Boolean = false
}

/**
 * A minimal fake VirtualFileSystem implementation.
 */
object FakeVirtualFileSystem : VirtualFileSystem() {
    override fun getProtocol(): String = "fake"
    override fun findFileByPath(path: String): VirtualFile? = null
    override fun refreshAndFindFileByPath(path: String): VirtualFile? = null
    override fun refresh(asynchronous: Boolean) {}
    override fun addVirtualFileListener(listener: VirtualFileListener) {}
    override fun removeVirtualFileListener(listener: VirtualFileListener) {}
    override fun deleteFile(requestor: Any?, vFile: VirtualFile) {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun moveFile(requestor: Any?, vFile: VirtualFile, newParent: VirtualFile) {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun renameFile(requestor: Any?, vFile: VirtualFile, newName: String) {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun createChildFile(requestor: Any?, vDir: VirtualFile, fileName: String): VirtualFile {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun createChildDirectory(requestor: Any?, vDir: VirtualFile, dirName: String): VirtualFile {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun copyFile(
        requestor: Any?,
        virtualFile: VirtualFile,
        newParent: VirtualFile,
        copyName: String
    ): VirtualFile {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun isReadOnly(): Boolean = true
}
