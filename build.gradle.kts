
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    java
    id("org.jetbrains.kotlin.jvm") version "1.9.23"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.21"
    id("org.jetbrains.intellij.platform") version "2.2.1"
    id("pl.allegro.tech.build.axion-release") version "1.18.16"
    id("com.diffplug.spotless") version "6.20.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.7"
    id("com.github.ben-manes.versions") version "0.48.0"
    jacoco
}

group = "com.clipcraft"

// SCM-based versioning configuration
scmVersion {
    tag {
        prefix.set("v")
        versionSeparator.set("")
    }
    useHighestVersion.set(true)
    ignoreUncommittedChanges.set(true)
    branchVersionIncrementer.putAll(
        mapOf("main" to pl.allegro.tech.build.axion.release.domain.properties.VersionProperties.Incrementer { ctx ->
            val process = Runtime.getRuntime().exec("git log -1 --pretty=%B")
            val message = process.inputStream.bufferedReader().readText().trim()
            when {
                "BREAKING CHANGE" in message || Regex("^.*!:").containsMatchIn(message) ->
                    ctx.currentVersion.incrementMajorVersion()
                message.startsWith("feat", ignoreCase = true) ->
                    ctx.currentVersion.incrementMinorVersion()
                message.startsWith("fix", ignoreCase = true) ->
                    ctx.currentVersion.incrementPatchVersion()
                else -> ctx.currentVersion
            }
        })
    )
    versionIncrementer("incrementPatch")
    createReleaseCommit.set(false)
}
version = scmVersion.version

// Version variables
val ktorVersion = "2.2.4"
val kotlinxSerializationVersion = "1.5.1"

// Repositories (grouped into one block)
repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Kotlin configuration
kotlin {
    jvmToolchain(17)
}

// Task configurations (grouped together)
tasks {
    test {
        useJUnitPlatform()
    }
    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }
    publishPlugin {
        channels = listOf("beta")
        token = providers.gradleProperty("intellijPlatformPublishingToken")
    }
    jacocoTestReport {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
}

// Plugin-specific configuration blocks
spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint("0.48.2")
    }
    java {
        target("src/**/*.java")
        googleJavaFormat("1.16.0")
    }
    format("misc") {
        target("**/*.gradle", "**/*.md", "**/*.gitignore")
        trimTrailingWhitespace()
        indentWithSpaces()
        endWithNewline()
    }
}


detekt.config.setFrom("detekt-config.yml")
detekt.buildUponDefaultConfig = true

jacoco {
    toolVersion = "0.8.12"
}

// Dependencies configuration
dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.3.2.2")
        bundledPlugin("com.intellij.java")
        pluginVerifier()
        zipSigner()
        instrumentationTools()
        testFramework(TestFrameworkType.Platform)
    }
    testImplementation("junit:junit:4.13.2")
}
