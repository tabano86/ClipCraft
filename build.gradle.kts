import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    java
    // Kotlin plugin
    id("org.jetbrains.kotlin.jvm") version "1.9.23"
    // (Optional) Kotlin serialization if you use kotlinx-serialization in code
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.21"
    // IntelliJ Platform plugin (instead of the older org.jetbrains.intellij)
    id("org.jetbrains.intellij.platform") version "2.2.1"
    // Release versioning (axion-release)
    id("pl.allegro.tech.build.axion-release") version "1.18.16"
    // Spotless for code formatting
    id("com.diffplug.spotless") version "6.20.0"
    // Detekt for static analysis
    id("io.gitlab.arturbosch.detekt") version "1.23.7"
    // Gradle Versions plugin for dependency updates
    id("com.github.ben-manes.versions") version "0.48.0"
    // Jacoco for coverage
    jacoco
}

group = "com.clipcraft"

// Configure axion-release for semantic versioning
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

val ktorVersion = "2.2.4"
val kotlinxSerializationVersion = "1.5.1"

repositories {
    mavenCentral()
    // Use IntelliJ’s platform repository from the new plugin
    intellijPlatform {
        defaultRepositories()
    }
}

kotlin {
    jvmToolchain(17)
}

// Configure tasks
tasks {
    test {
        useJUnitPlatform()
    }
    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }
    // Publish to JetBrains Marketplace if desired
    publishPlugin {
        channels = listOf("beta")
        token = providers.gradleProperty("intellijPlatformPublishingToken")
    }
    // Jacoco test report
    jacocoTestReport {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
}

// Spotless config
spotless {
    kotlin {
        target("**/*.kt")
        ktlint("0.48.2").userData(mapOf("indent_size" to "4", "max_line_length" to "120"))
    }
}

// Detekt config
detekt {
    config.setFrom(files("detekt-config.yml"))
    buildUponDefaultConfig = true
    autoCorrect = true
}

// Jacoco config
jacoco {
    toolVersion = "0.8.12"
}

// Patch plugin.xml to set since/until build versions
tasks.patchPluginXml {
    sinceBuild.set("239.*")
    untilBuild.set("299.*")
}

dependencies {
    // IntelliJ platform + Java plugin
    intellijPlatform {
        intellijIdeaCommunity("2024.1")
        bundledPlugin("com.intellij.java")
        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.Platform)
    }
    // If you use kotlinx-serialization in code
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")

    // For testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.0")

    // If you rely on org.apache.commons.text.StringEscapeUtils in code:
    implementation("org.apache.commons:commons-text:1.10.0")
}
