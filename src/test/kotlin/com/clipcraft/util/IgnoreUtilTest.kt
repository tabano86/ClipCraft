package com.clipcraft.util

import com.clipcraft.FakeProject
import com.clipcraft.FakeVirtualFile
import com.clipcraft.model.ClipCraftOptions
import java.io.File
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class IgnoreUtilTest {

    @Test
    fun negativePatternOverridesPositiveMatch() {
        val opts = ClipCraftOptions(ignorePatterns = mutableListOf(".*\\.txt", "!special\\.txt"))
        val file = File(FakeVirtualFile("/project/special.txt", "data", false).path)
        val proj = FakeProject("/project")
        assertFalse(IgnoreUtil.shouldIgnore(file, opts, proj.basePath))
    }

    @Test
    fun ignoreBasedOnGitignoreEnabled() {
        val d = createTempDir()
        val f = File(d, ".gitignore")
        f.writeText("secrets/\n*.bak")
        val opts = ClipCraftOptions(useGitIgnore = true)
        val proj = FakeProject(d.absolutePath)
        val secretFile = File(FakeVirtualFile(File(d, "secrets/info.txt").absolutePath, "data").path)
        val bakFile = File(FakeVirtualFile(File(d, "notmp.bak").absolutePath, "data").path)
        assertTrue(IgnoreUtil.shouldIgnore(secretFile, opts, proj.basePath))
        assertTrue(IgnoreUtil.shouldIgnore(bakFile, opts, proj.basePath))
        f.delete()
        d.deleteRecursively()
    }

    @ParameterizedTest
    @MethodSource("ignorePatternData")
    fun shouldIgnoreBasedOnIgnorePatterns(name: String, patterns: List<String>, expected: Boolean) {
        val opts = ClipCraftOptions(ignorePatterns = patterns.toMutableList())
        val file = File(FakeVirtualFile("/project/$name", "dummy", false).path)
        val proj = FakeProject("/project")
        assertEquals(expected, IgnoreUtil.shouldIgnore(file, opts, proj.basePath))
    }

    companion object {
        @JvmStatic
        fun ignorePatternData(): Stream<Arguments> = Stream.of(
            Arguments.of("log.txt", listOf("*.txt"), true),
            Arguments.of("focus.txt", listOf("*.txt", "!focus.txt"), false),
            Arguments.of("readme.md", listOf("*.txt"), false),
            Arguments.of("error.log", listOf("er*or.???"), true),
            Arguments.of("misc.txt", listOf("*.txt", "!misc.txt"), false),
        )
    }

    @Test
    fun ignoreBasedOnIgnoreFiles() {
        val opts = ClipCraftOptions(ignorePatterns = mutableListOf(), ignoreFiles = listOf("killme.txt"))
        val file = File(FakeVirtualFile("/project/killme.txt", "dummy", false).path)
        val proj = FakeProject("/project")
        assertTrue(IgnoreUtil.shouldIgnore(file, opts, proj.basePath))
    }

    @Test
    fun ignoreBasedOnIgnoreFolders() {
        val opts = ClipCraftOptions(ignorePatterns = mutableListOf(), ignoreFolders = listOf("bin"))
        val folder = File(FakeVirtualFile("/project/bin", "dummy", true).path)
        val proj = FakeProject("/project")
        assertTrue(IgnoreUtil.shouldIgnore(folder, opts, proj.basePath))
    }

    @Test
    fun mergeGitignoreRulesAddsPatterns() {
        val dir = createTempDir()
        val gf = File(dir, ".gitignore")
        gf.writeText("vendor/\n*.cache")
        val opts = ClipCraftOptions(ignorePatterns = mutableListOf("init"), useGitIgnore = true)
        val pr = FakeProject(dir.absolutePath)
        IgnoreUtil.mergeGitIgnoreRules(opts, pr)
        assertTrue("vendor/" in opts.ignorePatterns)
        assertTrue("*.cache" in opts.ignorePatterns)
        gf.delete()
        dir.deleteRecursively()
    }

    @Test
    fun complexScenarioGitignore() {
        val d = createTempDir()
        val gf = File(d, ".gitignore")
        gf.writeText("private/\n*.secret\n!public.secret")
        val opts = ClipCraftOptions(ignorePatterns = mutableListOf("*.log", "!debug.log"), useGitIgnore = true)
        val proj = FakeProject(d.absolutePath)
        IgnoreUtil.mergeGitIgnoreRules(opts, proj)

        val f1 = File(FakeVirtualFile(File(d, "private/a.txt").absolutePath, "data").path)
        val f2 = File(FakeVirtualFile(File(d, "notes.secret").absolutePath, "data").path)
        val f3 = File(FakeVirtualFile(File(d, "public.secret").absolutePath, "data").path)
        val f4 = File(FakeVirtualFile(File(d, "doom.log").absolutePath, "data").path)
        val f5 = File(FakeVirtualFile(File(d, "debug.log").absolutePath, "data").path)
        val f6 = File(FakeVirtualFile(File(d, "readme.txt").absolutePath, "data").path)

        assertTrue(IgnoreUtil.shouldIgnore(f1, opts, proj.basePath))
        assertTrue(IgnoreUtil.shouldIgnore(f2, opts, proj.basePath))
        assertFalse(IgnoreUtil.shouldIgnore(f3, opts, proj.basePath))
        assertTrue(IgnoreUtil.shouldIgnore(f4, opts, proj.basePath))
        assertFalse(IgnoreUtil.shouldIgnore(f5, opts, proj.basePath))
        assertFalse(IgnoreUtil.shouldIgnore(f6, opts, proj.basePath))

        gf.delete()
        d.deleteRecursively()
    }

    @Test
    fun windowsStyleSeparators() {
        val d = createTempDir()
        val f = File(d, ".gitignore")
        f.writeText("dist/")
        val opts = ClipCraftOptions(ignorePatterns = mutableListOf("*.temp"), useGitIgnore = true)
        val pr = FakeProject(d.absolutePath)
        val file = File(FakeVirtualFile(File(d, "dist\\subdir\\helper.temp").absolutePath, "data").path)
        assertTrue(IgnoreUtil.shouldIgnore(file, opts, pr.basePath))
        f.delete()
        d.deleteRecursively()
    }

    @Test
    fun invertIgnorePattern() {
        val p = FakeProject("/project")
        val opts = ClipCraftOptions(ignorePatterns = mutableListOf("*.txt"), invertIgnorePatterns = true)
        val file = File(FakeVirtualFile("/project/test.txt", "data").path)
        // Without inversion, a txt would be ignored
        // With inversion, it's the opposite
        assertFalse(IgnoreUtil.shouldIgnore(file, opts, p.basePath))
    }

    @Test
    fun noProjectBasePath() {
        val file = File(FakeVirtualFile("/something/bin/test.log", "data", false).path)
        val opts = ClipCraftOptions(ignorePatterns = mutableListOf("*.log"))
        assertTrue(IgnoreUtil.shouldIgnore(file, opts, ""))
    }
}
