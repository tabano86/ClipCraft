import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    java
    id("org.jetbrains.kotlin.jvm") version "1.9.23"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23"
    // This is the NEW IntelliJ Platform Gradle Plugin 2.x
    id("org.jetbrains.intellij.platform") version "2.2.1"
    // Axion for semantic versioning
    id("pl.allegro.tech.build.axion-release") version "1.18.16"
    // Optional code-formatting, static analysis, dependency checks
    id("com.diffplug.spotless") version "6.20.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.7"
    id("com.github.ben-manes.versions") version "0.48.0"
    jacoco
}

group = "com.clipcraft"

// -------------------------------
// Axion Release Configuration
// -------------------------------
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
            val msg = process.inputStream.bufferedReader().readText().trim()
            when {
                "BREAKING CHANGE" in msg ||
                        Regex("^.*!:").containsMatchIn(msg) -> ctx.currentVersion.incrementMajorVersion()
                msg.startsWith("feat", ignoreCase = true) -> ctx.currentVersion.incrementMinorVersion()
                msg.startsWith("fix", ignoreCase = true) -> ctx.currentVersion.incrementPatchVersion()
                else -> ctx.currentVersion
            }
        })
    )
    versionIncrementer("incrementPatch")
    createReleaseCommit.set(
        project.findProperty("axion.createReleaseCommit")?.toString()?.toBoolean() ?: false
    )
}
version = scmVersion.version
val computedVersion = project.version.toString()

// -------------------------------
// Dynamic Change-Notes (from last commit)
// -------------------------------
val dynamicChangeNotes = providers.provider {
    val process = Runtime.getRuntime().exec("git log -1 --pretty=%B")
    val rawMsg = process.inputStream.bufferedReader().readText().trim()
    rawMsg
        .replace("<![CDATA[", "<<_CDATA_START_")
        .replace("]]>", "<<_CDATA_END_")
}

// -------------------------------
// Repositories
// -------------------------------
repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()  // Adds JetBrains' release/snapshot repos automatically
    }
}

// -------------------------------
// IntelliJ Platform Dependencies
// (This is the NEW 2.x approach.)
// -------------------------------
dependencies {
    intellijPlatform {
        // Target IntelliJ IDEA Community 2023.3
        // (You can substitute 2024.1 if it is published.)
        intellijIdeaCommunity("2023.3")

        // Example: Add a bundled plugin
        bundledPlugin("com.intellij.java")

        // Example: Additional plugin from Marketplace
        // plugin("org.intellij.scala", "2023.3.1")

        // For test support:
        testFramework(TestFrameworkType.Platform)
        pluginVerifier()
        zipSigner()
    }

    // Example: Additional libraries
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
    implementation("org.apache.commons:commons-text:1.13.0")
    implementation("org.apache.commons:commons-lang3:3.17.0")
}

// -------------------------------
// Kotlin JVM
// -------------------------------
kotlin {
    jvmToolchain(17)
}

// -------------------------------
// Task Configuration
// -------------------------------
tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }
    test {
        useJUnitPlatform()
    }
    patchPluginXml {
        // Patch plugin.xml with the last commit message
        changeNotes.set(dynamicChangeNotes)
        // Inject Axion version
        version = computedVersion
        // Example build range
        sinceBuild.set("233")
        untilBuild.set("299.*")
    }
    // Provided by plugin:
    //   buildPlugin, publishPlugin, runIde, buildSearchableOptions, etc.

    publishPlugin {
        channels = listOf("beta")
        token = providers.gradleProperty("intellijPublishingToken")
        doFirst {
            if (
                project.hasProperty("failOnDuplicateVersion")
                && project.property("failOnDuplicateVersion") == "true"
            ) {
                throw GradleException("Duplicate version: $computedVersion. Bump version before publishing.")
            }
        }
    }
    jacocoTestReport {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
    register("printVersion") {
        doLast {
            println("Computed plugin version: $computedVersion")
        }
    }
    register("releasePlugin") {
        group = "release"
        description = "Bumps version, tags, then publish plugin to Marketplace."
        dependsOn("patchPluginXml", "publishPlugin")
        doLast {
            println("Release complete. Version: $computedVersion")
        }
    }
}

// -------------------------------
// Code Quality
// -------------------------------
spotless {
    kotlin {
        target("**/*.kt")
        ktlint("0.48.2").userData(mapOf("indent_size" to "4", "max_line_length" to "120"))
    }
}
detekt {
    config.setFrom(files("detekt-config.yml"))
    buildUponDefaultConfig = true
    autoCorrect = true
}
jacoco {
    toolVersion = "0.8.12"
}

// -------------------------------
// Performance Optimizations
// -------------------------------
gradle.startParameter.isParallelProjectExecutionEnabled = true
gradle.startParameter.isBuildCacheEnabled = true
