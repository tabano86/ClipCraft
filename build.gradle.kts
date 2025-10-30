import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    java
    id("org.jetbrains.kotlin.jvm") version "1.9.23"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23"
    id("org.jetbrains.intellij.platform") version "2.3.0"
    id("pl.allegro.tech.build.axion-release") version "1.18.16"
    id("com.diffplug.spotless") version "6.20.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.7"
    id("com.github.ben-manes.versions") version "0.48.0"
    jacoco
}

group = "com.clipcraft"

// Axion Release Config
scmVersion {
    tag {
        prefix.set("v")
        versionSeparator.set("")
    }
    useHighestVersion.set(true)
    ignoreUncommittedChanges.set(true)
    // Example incremental logic
    branchVersionIncrementer.putAll(
        mapOf("main" to pl.allegro.tech.build.axion.release.domain.properties.VersionProperties.Incrementer { ctx ->
            val process = Runtime.getRuntime().exec("git log -1 --pretty=%B")
            val msg = process.inputStream.bufferedReader().readText().trim()
            when {
                "BREAKING CHANGE" in msg -> ctx.currentVersion.incrementMajorVersion()
                msg.startsWith("feat", ignoreCase = true) -> ctx.currentVersion.incrementMinorVersion()
                msg.startsWith("fix", ignoreCase = true) -> ctx.currentVersion.incrementPatchVersion()
                else -> ctx.currentVersion
            }
        })
    )
    versionIncrementer("incrementPatch")
    createReleaseCommit.set(false)
}

version = scmVersion.version
val computedVersion = project.version.toString()

// Dynamic Change Notes from last commit
val dynamicChangeNotes = providers.provider {
    val process = Runtime.getRuntime().exec("git log -1 --pretty=%B")
    val rawMsg = process.inputStream.bufferedReader().readText().trim()
    rawMsg.replace("<![CDATA[", "<<_CDATA_START_").replace("]]>", "<<_CDATA_END_")
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2023.3")
        bundledPlugin("com.intellij.java")
        bundledPlugin("Git4Idea")
        testFramework(TestFrameworkType.Platform)
        pluginVerifier()
        zipSigner()
    }

    // JSON/XML/YAML Processing - Industry Standard
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.18.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")

    // Markdown Processing - Industry Standard
    implementation("org.commonmark:commonmark:0.22.0")
    implementation("org.commonmark:commonmark-ext-gfm-tables:0.22.0")
    implementation("org.commonmark:commonmark-ext-heading-anchor:0.22.0")
    implementation("org.commonmark:commonmark-ext-yaml-front-matter:0.22.0")

    // AI Token Counting - Use tiktoken
    implementation("com.knuddels:jtokkit:1.1.0")

    // Utilities - Google Guava (battle-tested)
    implementation("com.google.guava:guava:33.3.1-jre")

    // Path Matching - Use Spring's PathMatcher (rock-solid)
    implementation("org.springframework:spring-core:6.2.1") {
        exclude(group = "org.springframework", module = "spring-jcl")
    }

    // YAML Processing
    implementation("org.yaml:snakeyaml:2.3")

    // Git Integration - JGit (don't reinvent git)
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.1.0.202411261347-r")

    // Gitignore Parsing
    implementation("org.eclipse.jgit:org.eclipse.jgit.pgm:7.1.0.202411261347-r")

    // Compression for large exports
    implementation("org.apache.commons:commons-compress:1.27.1")

    // Fast JSON for simple cases
    implementation("com.google.code.gson:gson:2.11.0")

    // HTML generation
    implementation("org.jsoup:jsoup:1.18.3")

    // Modern UI - FlatLaf themes
    implementation("com.formdev:flatlaf:3.5.2")
    implementation("com.formdev:flatlaf-extras:3.5.2")
    implementation("com.formdev:flatlaf-intellij-themes:3.5.2")

    // Kotlin coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")

    // Testing - Modern stack
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.4")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("io.mockk:mockk:1.13.14")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("com.google.truth:truth:1.4.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
}

kotlin {
    jvmToolchain(21)
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "21"
    }
    test {
        useJUnitPlatform()
    }
    patchPluginXml {
        changeNotes.set(dynamicChangeNotes)
        version = computedVersion
        sinceBuild.set("233")
        untilBuild.set("299.*")
    }
    jacocoTestReport {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
    register<DefaultTask>("printVersion") {
        doLast {
            println("Computed plugin version: $computedVersion")
        }
    }
    register<DefaultTask>("releasePlugin") {
        group = "release"
        description = "Publish plugin to Marketplace."
        dependsOn("patchPluginXml", "publishPlugin")
        doLast {
            println("Release complete. Version: $computedVersion")
        }
    }

    publishPlugin {
        token.set(project.findProperty("intellijPlatformPublishingToken") as String?)
    }
}

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

gradle.startParameter.isParallelProjectExecutionEnabled = true
gradle.startParameter.isBuildCacheEnabled = true
