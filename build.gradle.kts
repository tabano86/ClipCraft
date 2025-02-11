import pl.allegro.tech.build.axion.release.domain.VersionIncrementerContext
import pl.allegro.tech.build.axion.release.domain.properties.VersionProperties

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.21"
    id("org.jetbrains.intellij") version "1.17.4"
    id("pl.allegro.tech.build.axion-release") version "1.18.16"
    id("com.diffplug.spotless") version "6.20.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.7"
    id("com.github.ben-manes.versions") version "0.48.0"
    id("jacoco")
    // The pre-commit git hooks plugin is now applied via settings.gradle.kts.
}

group = "com.clipcraft"

scmVersion {
    tag {
        prefix.set("v")            // e.g. v1.2.3
        versionSeparator.set("")   // No separator between prefix and version
    }
    useHighestVersion.set(true)    // Consider highest tag across branches
    ignoreUncommittedChanges.set(true)  // Allow uncommitted files (e.g. CI artifacts)
    branchVersionIncrementer.putAll(
        mapOf(
            "main" to VersionProperties.Incrementer { ctx: VersionIncrementerContext ->
                val message = Runtime.getRuntime()
                    .exec("git log -1 --pretty=%B")
                    .inputStream.bufferedReader().readText().trim()
                when {
                    "BREAKING CHANGE" in message || Regex("^.*!:").containsMatchIn(message) -> {
                        println("Conventional Commit detected BREAKING change -> incrementing MAJOR version")
                        ctx.currentVersion.incrementMajorVersion()
                    }
                    message.startsWith("feat", ignoreCase = true) -> {
                        println("Conventional Commit 'feat:' -> incrementing MINOR version")
                        ctx.currentVersion.incrementMinorVersion()
                    }
                    message.startsWith("fix", ignoreCase = true) -> {
                        println("Conventional Commit 'fix:' -> incrementing PATCH version")
                        ctx.currentVersion.incrementPatchVersion()
                    }
                    else -> ctx.currentVersion
                }
            }
        )
    )
    versionIncrementer("incrementPatch")
    createReleaseCommit.set(false)  // Do not create an extra commit for tagging
}

// Set the project version from SCM
version = scmVersion.version

// Disable verifyRelease to allow generated files to pass in CI
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
    type.set("IC")        // Community Edition
    // Include dependencies to satisfy module requirements.
    plugins.set(listOf("Git4Idea", "java"))
}

kotlin {
    jvmToolchain(17)  // Use Java 17 for compilation
}

tasks.test {
    useJUnitPlatform()
}

tasks.patchPluginXml {
    sinceBuild.set("*")
    untilBuild.set("232.*")
}

tasks.publishPlugin {
    // The Gradle IntelliJ Plugin will use the property intellijPlatformPublishingToken.
    // Supply it at runtime via the environment variable ORG_GRADLE_PROJECT_intellijPlatformPublishingToken.
    token = providers.gradleProperty("intellijPlatformPublishingToken")
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "17"
}

spotless {
    kotlin {
        target("**/*.kt")
        ktlint("0.48.2")
    }
    java {
        target("**/*.java")
        googleJavaFormat("1.16.0")
    }
}

detekt {
    config = files("detekt-config.yml")
    buildUponDefaultConfig = true
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
