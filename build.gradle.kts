import pl.allegro.tech.build.axion.release.domain.VersionIncrementerContext
import pl.allegro.tech.build.axion.release.domain.properties.VersionProperties

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.21"
    id("org.jetbrains.intellij") version "1.17.4"
    id("pl.allegro.tech.build.axion-release") version "1.18.16"
}

group = "com.clipcraft"

scmVersion {
    tag {
        prefix.set("v")            // Tag prefix "v" (e.g., v1.2.3)
        versionSeparator.set("")   // No separator between prefix and version
    }
    useHighestVersion.set(true)    // Consider highest tag across all branches
    ignoreUncommittedChanges.set(true)  // Allow uncommitted files (for CI-generated artifacts)
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
    // Default incrementer (used for other branches)
    versionIncrementer("incrementPatch")
    createReleaseCommit.set(false)  // Do not create an extra commit for tagging
}

// Set project version dynamically from Git (must come after scmVersion configuration)
version = scmVersion.version

// Disable the verifyRelease task so that uncommitted (generated) files do not block the release.
tasks.named("verifyRelease") {
    enabled = false
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}

intellij {
    version.set("2023.2") // Target IntelliJ IDEA version
    type.set("IC")        // IntelliJ Community Edition
    // Add dependency on the Java module to satisfy com.intellij.modules.java requirement.
    plugins.set(listOf("Git4Idea", "java"))
}

kotlin {
    jvmToolchain(17)  // Use Java 17 for compilation
}

tasks.test {
    useJUnitPlatform()
}

tasks.patchPluginXml {
    sinceBuild.set("232")
    untilBuild.set("232.*")
}

tasks.publishPlugin {
    token = providers.gradleProperty("intellijPlatformPublishingToken")
}