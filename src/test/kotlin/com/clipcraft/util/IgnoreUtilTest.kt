package com.clipcraft.util

import com.clipcraft.FakeProject
import com.clipcraft.FakeVirtualFile
import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.util.IgnoreUtil.mergeGitIgnoreRules
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.util.stream.Stream

class ClipCraftIgnoreUtilTest {

    @Test
    fun `negative pattern overrides positive match`() {
        val opts = ClipCraftOptions(ignorePatterns = mutableListOf(".*\\.txt", "!important\\.txt"))
        val fakeFile = FakeVirtualFile("/project/important.txt", "data", isDir = false)
        // Convert FakeVirtualFile to java.io.File using its path.
        val file = File(fakeFile.path)
        val project = FakeProject("/project")
        assertFalse(IgnoreUtil.shouldIgnore(file, opts, project.basePath))
    }

    @Test
    fun `ignore based on gitignore when useGitIgnore is enabled`() {
        val tempProjectDir = createTempDir()
        val gitIgnore = File(tempProjectDir, ".gitignore")
        gitIgnore.writeText("secret/\n*.bak")
        val opts = ClipCraftOptions(useGitIgnore = true)
        val project = FakeProject(tempProjectDir.absolutePath)

        val secretFake = FakeVirtualFile(File(tempProjectDir, "secret/info.txt").absolutePath, "data")
        val secretFile = File(secretFake.path)
        assertTrue(IgnoreUtil.shouldIgnore(secretFile, opts, project.basePath))

        val bakFake = FakeVirtualFile(File(tempProjectDir, "backup.bak").absolutePath, "data")
        val bakFile = File(bakFake.path)
        assertTrue(IgnoreUtil.shouldIgnore(bakFile, opts, project.basePath))

        gitIgnore.delete()
        tempProjectDir.deleteRecursively()
    }

    @ParameterizedTest(name = "File \"{0}\" with patterns {1} should be ignored: {2}")
    @MethodSource("ignorePatternTestProvider")
    fun `shouldIgnore based on ignorePatterns`(fileName: String, patterns: List<String>, expected: Boolean) {
        val opts =
            ClipCraftOptions(ignorePatterns = mutableListOf<String>().apply { addAll(patterns) }, useGitIgnore = false)
        val fakeFile = FakeVirtualFile("/project/$fileName", "dummy", isDir = false)
        val file = File(fakeFile.path)
        val project = FakeProject("/project")
        assertEquals(
            expected,
            IgnoreUtil.shouldIgnore(file, opts, project.basePath),
            "File: $fileName, Patterns: $patterns"
        )
    }

    companion object {
        @JvmStatic
        fun ignorePatternTestProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("log.txt", listOf("*.txt"), true),
            Arguments.of("important.txt", listOf("*.txt", "!important.txt"), false),
            Arguments.of("readme.md", listOf("*.txt"), false),
            Arguments.of("error.log", listOf("e*or.???"), true),
            Arguments.of("debug.txt", listOf("*.txt", "!debug.txt"), false)
        )
    }

    @Test
    fun `shouldIgnore returns true if file is in ignoreFiles list`() {
        val opts = ClipCraftOptions(
            ignorePatterns = mutableListOf(),
            useGitIgnore = false,
            ignoreFiles = listOf("ignoreme.txt")
        )
        val fakeFile = FakeVirtualFile("/project/ignoreme.txt", "dummy", isDir = false)
        val file = File(fakeFile.path)
        val project = FakeProject("/project")
        assertTrue(IgnoreUtil.shouldIgnore(file, opts, project.basePath))
    }

    @Test
    fun `shouldIgnore returns true if directory is in ignoreFolders list`() {
        val opts =
            ClipCraftOptions(ignorePatterns = mutableListOf(), useGitIgnore = false, ignoreFolders = listOf("build"))
        val fakeFolder = FakeVirtualFile("/project/build", "dummy", isDir = true)
        val folder = File(fakeFolder.path)
        val project = FakeProject("/project")
        assertTrue(IgnoreUtil.shouldIgnore(folder, opts, project.basePath))
    }

    @Test
    fun `mergeGitIgnoreRules adds gitignore patterns to options`() {
        val tempDir = createTempDir()
        val gitIgnoreFile = File(tempDir, ".gitignore")
        gitIgnoreFile.writeText("secret/\n*.bak")
        val opts = ClipCraftOptions(ignorePatterns = mutableListOf("initial"), useGitIgnore = true)
        val project = FakeProject(tempDir.absolutePath)
        mergeGitIgnoreRules(opts, project)
        assertTrue(opts.ignorePatterns.contains("secret/"))
        assertTrue(opts.ignorePatterns.contains("*.bak"))
        gitIgnoreFile.delete()
        tempDir.deleteRecursively()
    }

    @Test
    fun `complex scenario with mixed ignore patterns and gitignore merging`() {
        val tempProjectDir = createTempDir()
        val gitIgnoreFile = File(tempProjectDir, ".gitignore")
        gitIgnoreFile.writeText("private/\n*.secret\n!public.secret")
        val opts = ClipCraftOptions(
            ignorePatterns = mutableListOf("*.log", "!debug.log"),
            useGitIgnore = true
        )
        val project = FakeProject(tempProjectDir.absolutePath)
        mergeGitIgnoreRules(opts, project)

        val file1 = File(FakeVirtualFile(File(tempProjectDir, "private/data.txt").absolutePath, "data").path)
        val file2 = File(FakeVirtualFile(File(tempProjectDir, "notes.secret").absolutePath, "data").path)
        val file3 = File(FakeVirtualFile(File(tempProjectDir, "public.secret").absolutePath, "data").path)
        val file4 = File(FakeVirtualFile(File(tempProjectDir, "app.log").absolutePath, "data").path)
        val file5 = File(FakeVirtualFile(File(tempProjectDir, "debug.log").absolutePath, "data").path)
        val file6 = File(FakeVirtualFile(File(tempProjectDir, "readme.txt").absolutePath, "data").path)

        assertTrue(
            IgnoreUtil.shouldIgnore(file1, opts, project.basePath),
            "File in private folder should be ignored"
        )
        assertTrue(
            IgnoreUtil.shouldIgnore(file2, opts, project.basePath),
            "File with .secret extension should be ignored"
        )
        assertFalse(
            IgnoreUtil.shouldIgnore(file3, opts, project.basePath),
            "File public.secret should not be ignored"
        )
        assertTrue(
            IgnoreUtil.shouldIgnore(file4, opts, project.basePath),
            "File with .log extension should be ignored"
        )
        assertFalse(IgnoreUtil.shouldIgnore(file5, opts, project.basePath), "debug.log should not be ignored")
        assertFalse(IgnoreUtil.shouldIgnore(file6, opts, project.basePath), "readme.txt should not be ignored")

        gitIgnoreFile.delete()
        tempProjectDir.deleteRecursively()
    }

    @Test
    fun `complex scenario with relative paths and windows style separators`() {
        val tempProjectDir = createTempDir()
        val gitIgnoreFile = File(tempProjectDir, ".gitignore")
        gitIgnoreFile.writeText("bin/")
        val opts = ClipCraftOptions(ignorePatterns = mutableListOf("*.tmp"), useGitIgnore = true)
        val fakeProject = FakeProject(tempProjectDir.absolutePath)
        val file = File(FakeVirtualFile(File(tempProjectDir, "bin\\helper.tmp").absolutePath, "data").path)
        assertTrue(IgnoreUtil.shouldIgnore(file, opts, fakeProject.basePath))
        gitIgnoreFile.delete()
        tempProjectDir.deleteRecursively()
    }
}
