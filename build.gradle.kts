import pl.allegro.tech.build.axion.release.domain.properties.VersionProperties

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.21"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.21"
    id("org.jetbrains.intellij") version "1.17.4"
    id("pl.allegro.tech.build.axion-release") version "1.18.16"
    id("com.diffplug.spotless") version "6.20.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.7"
    id("com.github.ben-manes.versions") version "0.48.0"
    id("jacoco")
    // Note: The Git Hooks plugin is applied in settings.gradle.kts.
}

group = "com.clipcraft"

// Axion release and versioning configuration
scmVersion {
    tag {
        prefix.set("v")
        versionSeparator.set("")
    }
    useHighestVersion.set(true)
    ignoreUncommittedChanges.set(true)
    branchVersionIncrementer.putAll(
        mapOf(
            "main" to VersionProperties.Incrementer { ctx ->
                val message = Runtime.getRuntime()
                    .exec("git log -1 --pretty=%B")
                    .inputStream
                    .bufferedReader()
                    .readText()
                    .trim()

                when {
                    "BREAKING CHANGE" in message ||
                            Regex("^.*!:").containsMatchIn(message) ->
                        ctx.currentVersion.incrementMajorVersion()
                    message.startsWith("feat", ignoreCase = true) ->
                        ctx.currentVersion.incrementMinorVersion()
                    message.startsWith("fix", ignoreCase = true) ->
                        ctx.currentVersion.incrementPatchVersion()
                    else -> ctx.currentVersion
                }
            }
        )
    )
    versionIncrementer("incrementPatch")
    createReleaseCommit.set(false)
}
version = scmVersion.version

// Define versions for Ktor and Serialization dependencies
val ktorVersion = "2.2.4"
val kotlinxSerializationVersion = "1.5.1"

repositories {
    mavenCentral()
}

dependencies {
    // Ktor dependencies for HTTP client, JSON serialization, and logging
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-json:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    // Kotlinx Serialization dependency for JSON processing
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")

    // IntelliJ Plugin dependencies (if any additional ones are needed, add them here)

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}

intellij {
    version.set("2023.2")
    type.set("IC")
    plugins.set(listOf("Git4Idea", "java"))
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}

tasks.patchPluginXml {
    sinceBuild.set("*")
    untilBuild.set("232.*")
}

tasks.publishPlugin {
    token = providers.gradleProperty("intellijPlatformPublishingToken")
}

spotless {
    // Kotlin Formatting
    kotlin {
        target("src/**/*.kt")
        ktlint("0.48.2")
    }
    // Java Formatting
    java {
        target("src/**/*.java")
        googleJavaFormat("1.16.0")
    }
    // YAML Formatting
    yaml {
        target("**/*.yml", "**/*.yaml")
        jackson()
    }
    // Miscellaneous Formatting
    format("misc") {
        target("**/*.gradle", "**/*.md", "**/*.gitignore")
        trimTrailingWhitespace()
        indentWithSpaces()
        endWithNewline()
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