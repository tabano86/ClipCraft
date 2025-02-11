import pl.allegro.tech.build.axion.release.domain.VersionIncrementerContext
import pl.allegro.tech.build.axion.release.domain.properties.VersionProperties

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.21"
    id("org.jetbrains.intellij") version "1.17.4"
    id("pl.allegro.tech.build.axion-release") version "1.18.16"
}

group = "com.clipcraft"  // Set your plugin's group

scmVersion {
    tag {
        prefix.set("v")            // Tag prefix "v" (e.g., v1.2.3)
        versionSeparator.set("")   // No separator between prefix and version
    }
    useHighestVersion.set(true)    // Consider highest tag across all branches
    ignoreUncommittedChanges.set(true)  // Do not require a version bump for local changes
    // Automated version increment based on Conventional Commits on main branch
    branchVersionIncrementer.putAll(mapOf(
        "main" to VersionProperties.Incrementer { ctx: VersionIncrementerContext ->
            // Analyze the latest commit message for version keywords
            val message = Runtime.getRuntime()
                .exec("git log -1 --pretty=%B").inputStream
                .bufferedReader().readText().trim()
            when {
                // Major bump for breaking changes (explicit or indicated with "!")
                "BREAKING CHANGE" in message || Regex("^.*!:").containsMatchIn(message) -> {
                    println("Conventional Commit detected BREAKING change -> incrementing MAJOR version")
                    ctx.currentVersion.incrementMajorVersion()
                }
                // Minor bump for new features
                message.startsWith("feat", ignoreCase = true) -> {
                    println("Conventional Commit 'feat:' -> incrementing MINOR version")
                    ctx.currentVersion.incrementMinorVersion()
                }
                // Patch bump for fixes
                message.startsWith("fix", ignoreCase = true) -> {
                    println("Conventional Commit 'fix:' -> incrementing PATCH version")
                    ctx.currentVersion.incrementPatchVersion()
                }
                // Default: no bump (retains current version)
                else -> ctx.currentVersion
            }
        }
    ))
    // Default incrementer (used for other branches if above not applied)
    versionIncrementer("incrementPatch")  // Default to patch increments
    createReleaseCommit.set(false)        // Do not create an extra commit for tagging
}

// Set project version dynamically from Git (must come after scmVersion configuration)
version = scmVersion.version  // e.g., "1.2.3-SNAPSHOT" or "1.2.3" based on tags

repositories {
    mavenCentral()
    // Gradle Plugin Portal is used implicitly for plugin coordinates.
}

dependencies {
    // Kotlin testing library (provides Kotlin assertions and compatibility with JUnit)
    testImplementation(kotlin("test"))
    // JUnit 5 API and engine for unit tests
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}

intellij {
    version.set("2023.2") // Target IntelliJ IDEA version
    type.set("IC")        // Target IDE edition: IC = IntelliJ Community, IU = Ultimate
    // Declare dependency on Java module to satisfy 'com.intellij.modules.java'
    plugins.set(listOf("Git4Idea", "java"))
    // Other IntelliJ plugin options can be set here if needed
}

kotlin {
    jvmToolchain(17)  // Use Java 17 for compilation (required for IntelliJ 2022.3+ plugins)
}

tasks.test {
    useJUnitPlatform()  // Enable JUnit 5 platform for tests
}

// Configure IntelliJ plugin XML patching – set since/until build for compatibility
tasks.patchPluginXml {
    sinceBuild.set("232")    // Minimum IDE build version (e.g., 232 corresponds to IntelliJ 2023.2)
    untilBuild.set("232.*")  // Compatible with all 2023.2 builds (adjust if needed)
}
