package com.clipcraft.util

import com.clipcraft.model.ClipCraftOptions
import com.intellij.openapi.extensions.PluginDescriptor
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.CoroutineScope
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.picocontainer.PicoContainer
import java.io.File
import java.util.stream.Stream

// Minimal fake implementations.
class FakeProject(private val basePath: String) : Project {
    override fun getBasePath(): String = basePath
    override fun getName(): String = "FakeProject"
    override fun getBaseDir(): VirtualFile {
        TODO("Not yet implemented")
    }

    override fun isInitialized(): Boolean = true
    override fun getCoroutineScope(): CoroutineScope {
        TODO("Not yet implemented")
    }

    override fun isOpen(): Boolean = true
    override fun isDisposed(): Boolean = false
    override fun getProjectFile(): VirtualFile = throw UnsupportedOperationException()
    override fun getProjectFilePath(): String = throw UnsupportedOperationException()
    override fun getWorkspaceFile(): VirtualFile = throw UnsupportedOperationException()
    override fun getLocationHash(): String = throw UnsupportedOperationException()
    override fun save() {
        throw UnsupportedOperationException()
    }

    override fun getDisposed() = throw UnsupportedOperationException()
    override fun <T : Any?> getService(serviceClass: Class<T>): T = throw UnsupportedOperationException()
    override fun <T : Any?> instantiateClass(className: String, pluginDescriptor: PluginDescriptor): T & Any {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> instantiateClassWithConstructorInjection(
        aClass: Class<T>,
        key: Any,
        pluginId: PluginId
    ): T {
        TODO("Not yet implemented")
    }

    override fun createError(error: Throwable, pluginId: PluginId): RuntimeException {
        TODO("Not yet implemented")
    }

    override fun createError(message: String, pluginId: PluginId): RuntimeException {
        TODO("Not yet implemented")
    }

    override fun createError(
        message: String,
        error: Throwable?,
        pluginId: PluginId,
        attachments: MutableMap<String, String>?
    ): RuntimeException =
        throw UnsupportedOperationException()

    override fun <T : Any?> loadClass(className: String, pluginDescriptor: PluginDescriptor): Class<T> {
        TODO("Not yet implemented")
    }

    override fun getActivityCategory(isExtension: Boolean): com.intellij.diagnostic.ActivityCategory =
        throw UnsupportedOperationException()

    override fun <T : Any?> getComponent(interfaceClass: Class<T>): T = throw UnsupportedOperationException()
    override fun hasComponent(interfaceClass: Class<*>): Boolean = throw UnsupportedOperationException()
    override fun getPicoContainer(): PicoContainer {
        TODO("Not yet implemented")
    }

    override fun isInjectionForExtensionSupported(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getMessageBus(): com.intellij.util.messages.MessageBus = throw UnsupportedOperationException()
    override fun dispose() {}
    override fun getExtensionArea(): com.intellij.openapi.extensions.ExtensionsArea =
        throw UnsupportedOperationException()

    override fun <T : Any?> getUserData(key: com.intellij.openapi.util.Key<T>): T? = null
    override fun <T : Any?> putUserData(key: com.intellij.openapi.util.Key<T>, value: T?) {}
}

open class FakeVirtualFile(
    private val filePath: String,
    private val fileContent: String,
    private val isDir: Boolean = false
) : VirtualFile() {
    override fun getName(): String = File(filePath).name
    override fun getFileSystem() = FakeVirtualFileSystem
    override fun getPath(): String = filePath
    override fun isWritable(): Boolean = false
    override fun isDirectory(): Boolean = isDir
    override fun isValid(): Boolean = true
    override fun getParent(): VirtualFile? = null
    override fun getChildren(): Array<VirtualFile> = emptyArray()
    override fun getOutputStream(
        requestor: Any?,
        newModificationStamp: Long,
        newTimeStamp: Long
    ): java.io.OutputStream =
        throw UnsupportedOperationException("Not supported")

    override fun contentsToByteArray(): ByteArray = fileContent.toByteArray(Charsets.UTF_8)
    override fun getTimeStamp(): Long = 0L
    override fun getLength(): Long = fileContent.length.toLong()
    override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {}
    override fun getInputStream(): java.io.InputStream = fileContent.byteInputStream()
    override fun getExtension(): String = File(filePath).extension
}

object FakeVirtualFileSystem : com.intellij.openapi.vfs.VirtualFileSystem() {
    override fun getProtocol(): String = "fake"
    override fun findFileByPath(path: String): VirtualFile? = null
    override fun refreshAndFindFileByPath(path: String): VirtualFile? = null
    override fun refresh(asynchronous: Boolean) {}
    override fun addVirtualFileListener(listener: com.intellij.openapi.vfs.VirtualFileListener) {}
    override fun removeVirtualFileListener(listener: com.intellij.openapi.vfs.VirtualFileListener) {}
    override fun deleteFile(requestor: Any?, vFile: VirtualFile) = throw UnsupportedOperationException()
    override fun moveFile(requestor: Any?, vFile: VirtualFile, newParent: VirtualFile) =
        throw UnsupportedOperationException()

    override fun renameFile(requestor: Any?, vFile: VirtualFile, newName: String) =
        throw UnsupportedOperationException()

    override fun createChildFile(requestor: Any?, vDir: VirtualFile, fileName: String): VirtualFile =
        throw UnsupportedOperationException()

    override fun createChildDirectory(requestor: Any?, vDir: VirtualFile, dirName: String): VirtualFile =
        throw UnsupportedOperationException()

    override fun copyFile(
        requestor: Any?,
        virtualFile: VirtualFile,
        newParent: VirtualFile,
        copyName: String
    ): VirtualFile =
        throw UnsupportedOperationException()

    override fun isReadOnly(): Boolean = true
}

class ClipCraftIgnoreUtilTest {

    @Test
    fun `negative pattern overrides positive match`() {
        val opts = ClipCraftOptions(ignorePatterns = listOf(".*\\.txt", "!important\\.txt"))
        val file = FakeVirtualFile("/project/important.txt", "data", isDir = false)
        val project = FakeProject("/project")
        assertFalse(ClipCraftIgnoreUtil.shouldIgnore(file, opts, project))
    }

    @Test
    fun `ignore based on gitignore when useGitIgnore is enabled`() {
        val tempProjectDir = createTempDir()
        val gitIgnore = File(tempProjectDir, ".gitignore")
        gitIgnore.writeText("secret/\n*.bak")
        val opts = ClipCraftOptions(useGitIgnore = true)
        val project = FakeProject(tempProjectDir.absolutePath)

        // A file inside "secret" should be ignored.
        val secretFile = FakeVirtualFile(
            File(tempProjectDir, "secret/info.txt").absolutePath, "data"
        )
        assertTrue(ClipCraftIgnoreUtil.shouldIgnore(secretFile, opts, project))

        // A file with .bak extension should be ignored.
        val bakFile = FakeVirtualFile(
            File(tempProjectDir, "backup.bak").absolutePath, "data"
        )
        assertTrue(ClipCraftIgnoreUtil.shouldIgnore(bakFile, opts, project))

        // Clean up
        gitIgnore.delete()
        tempProjectDir.deleteRecursively()
    }

    @ParameterizedTest(name = "File \"{0}\" with patterns {1} should be ignored: {2}")
    @MethodSource("ignorePatternTestProvider")
    fun `shouldIgnore based on ignorePatterns`(fileName: String, patterns: List<String>, expected: Boolean) {
        val opts = ClipCraftOptions(ignorePatterns = patterns, useGitIgnore = false)
        val file = FakeVirtualFile("/project/$fileName", "dummy", isDir = false)
        val project = FakeProject("/project")
        assertEquals(
            expected,
            ClipCraftIgnoreUtil.shouldIgnore(file, opts, project),
            "File: $fileName, Patterns: $patterns"
        )
    }

    companion object {
        @JvmStatic
        fun ignorePatternTestProvider(): Stream<Arguments> = Stream.of(
            // File name matches positive pattern, no negative override -> ignore.
            Arguments.of("log.txt", listOf("*.txt"), true),
            // File name matches positive but also negative override -> do not ignore.
            Arguments.of("important.txt", listOf("*.txt", "!important.txt"), false),
            // File name does not match positive pattern -> not ignored.
            Arguments.of("readme.md", listOf("*.txt"), false),
            // File name matches a more complex wildcard pattern.
            Arguments.of("error.log", listOf("e*or.???"), true),
            // Negative override applied.
            Arguments.of("debug.txt", listOf("*.txt", "!debug.txt"), false)
        )
    }

    @Test
    fun `shouldIgnore returns true if file is in ignoreFiles list`() {
        val opts =
            ClipCraftOptions(ignorePatterns = emptyList(), useGitIgnore = false, ignoreFiles = listOf("ignoreme.txt"))
        val file = FakeVirtualFile("/project/ignoreme.txt", "dummy", isDir = false)
        val project = FakeProject("/project")
        assertTrue(ClipCraftIgnoreUtil.shouldIgnore(file, opts, project))
    }

    @Test
    fun `shouldIgnore returns true if directory is in ignoreFolders list`() {
        val opts = ClipCraftOptions(ignorePatterns = emptyList(), useGitIgnore = false, ignoreFolders = listOf("build"))
        val folder = FakeVirtualFile("/project/build", "dummy", isDir = true)
        val project = FakeProject("/project")
        assertTrue(ClipCraftIgnoreUtil.shouldIgnore(folder, opts, project))
    }

    @Test
    fun `mergeGitIgnoreRules adds gitignore patterns to options`() {
        val tempDir = createTempDir()
        val gitIgnoreFile = File(tempDir, ".gitignore")
        gitIgnoreFile.writeText("secret/\n*.bak")
        val opts = ClipCraftOptions(ignorePatterns = listOf("initial"), useGitIgnore = true)
        val project = FakeProject(tempDir.absolutePath)
        ClipCraftIgnoreUtil.mergeGitIgnoreRules(opts, project)
        assertTrue(opts.ignorePatterns.contains("secret/"))
        assertTrue(opts.ignorePatterns.contains("*.bak"))
        // Clean up
        gitIgnoreFile.delete()
        tempDir.deleteRecursively()
    }

    // Additional complex use cases:

    @Test
    fun `complex scenario with mixed ignore patterns and gitignore merging`() {
        // Set up a temporary project directory with a .gitignore file
        val tempProjectDir = createTempDir()
        val gitIgnoreFile = File(tempProjectDir, ".gitignore")
        // .gitignore ignores folder 'private' and any file ending with .secret, but does not ignore files starting with 'public'
        gitIgnoreFile.writeText("private/\n*.secret\n!public.secret")

        // Options include additional ignore patterns for any log files, but not debug logs.
        val opts = ClipCraftOptions(
            ignorePatterns = listOf("*.log", "!debug.log"),
            useGitIgnore = true
        )
        val project = FakeProject(tempProjectDir.absolutePath)
        // Merge .gitignore patterns.
        ClipCraftIgnoreUtil.mergeGitIgnoreRules(opts, project)

        // Files to test:
        // 1. A file in the private folder should be ignored.
        val file1 = FakeVirtualFile(File(tempProjectDir, "private/data.txt").absolutePath, "data")
        // 2. A file with .secret extension should be ignored.
        val file2 = FakeVirtualFile(File(tempProjectDir, "notes.secret").absolutePath, "data")
        // 3. A file with .secret extension that starts with public should NOT be ignored.
        val file3 = FakeVirtualFile(File(tempProjectDir, "public.secret").absolutePath, "data")
        // 4. A file with .log extension should be ignored.
        val file4 = FakeVirtualFile(File(tempProjectDir, "app.log").absolutePath, "data")
        // 5. A file with .log extension that is debug log should NOT be ignored.
        val file5 = FakeVirtualFile(File(tempProjectDir, "debug.log").absolutePath, "data")
        // 6. A file that doesn't match any pattern should not be ignored.
        val file6 = FakeVirtualFile(File(tempProjectDir, "readme.txt").absolutePath, "data")

        assertTrue(ClipCraftIgnoreUtil.shouldIgnore(file1, opts, project), "File in private folder should be ignored")
        assertTrue(
            ClipCraftIgnoreUtil.shouldIgnore(file2, opts, project),
            "File with .secret extension should be ignored"
        )
        assertFalse(ClipCraftIgnoreUtil.shouldIgnore(file3, opts, project), "File public.secret should not be ignored")
        assertTrue(ClipCraftIgnoreUtil.shouldIgnore(file4, opts, project), "File with .log extension should be ignored")
        assertFalse(ClipCraftIgnoreUtil.shouldIgnore(file5, opts, project), "debug.log should not be ignored")
        assertFalse(ClipCraftIgnoreUtil.shouldIgnore(file6, opts, project), "readme.txt should not be ignored")

        // Clean up
        gitIgnoreFile.delete()
        tempProjectDir.deleteRecursively()
    }

    @Test
    fun `complex scenario with relative paths and windows style separators`() {
        // Simulate a project with a Windows-style base path.
        val basePath = "C:/Projects/MyApp"
        // Create a .gitignore file in the project directory.
        val tempProjectDir = createTempDir()
        val gitIgnoreFile = File(tempProjectDir, ".gitignore")
        // Ignore all files in the "bin" folder.
        gitIgnoreFile.writeText("bin/")

        val opts = ClipCraftOptions(
            ignorePatterns = listOf("*.tmp"),
            useGitIgnore = true
        )
        // For this test, set the project basePath to the temp directory.
        val fakeProject = FakeProject(tempProjectDir.absolutePath)

        // File with Windows style separator in its relative path.
        val file = FakeVirtualFile(File(tempProjectDir, "bin\\helper.tmp").absolutePath, "data")
        // Even though pattern is "*.tmp", gitignore should trigger ignore because of "bin/"
        assertTrue(ClipCraftIgnoreUtil.shouldIgnore(file, opts, fakeProject))

        // Clean up
        gitIgnoreFile.delete()
        tempProjectDir.deleteRecursively()
    }
}
