import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    java
    id("org.jetbrains.kotlin.jvm") version "1.9.23"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23"
    id("org.jetbrains.intellij.platform") version "2.2.1"

    // Optional: Axion Release for semantic versioning
    id("pl.allegro.tech.build.axion-release") version "1.18.16"

    // Optional: Spotless for code formatting
    id("com.diffplug.spotless") version "6.20.0"

    // Optional: Detekt for static analysis
    id("io.gitlab.arturbosch.detekt") version "1.23.7"

    // Optional: Gradle Versions plugin for dependency updates
    id("com.github.ben-manes.versions") version "0.48.0"

    // Optional: Code coverage
    jacoco
}

group = "com.clipcraft"

// --- Axion Release Configuration (Optional) ---
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
                msg.startsWith("feat", ignoreCase = true) ->
                    ctx.currentVersion.incrementMinorVersion()
                msg.startsWith("fix", ignoreCase = true) ->
                    ctx.currentVersion.incrementPatchVersion()
                else -> ctx.currentVersion
            }
        })
    )
    versionIncrementer("incrementPatch")
    createReleaseCommit.set(false)
}
version = scmVersion.version  // or set manually if not using Axion

// --- Dynamic change-notes (plain text; patchPluginXml wraps it in CDATA) ---
val dynamicChangeNotes = providers.provider {
    val process = Runtime.getRuntime().exec("git log -1 --pretty=%B")
    val rawMsg = process.inputStream.bufferedReader().readText().trim()
    // Remove any problematic tokens (do not include literal <![CDATA[ or ]]>)
    rawMsg
        .replace("<![CDATA[", "<<_CDATA_START_")
        .replace("]]>", "<<_CDATA_END_")
}

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

kotlin {
    jvmToolchain(17)
}

// --- Dependencies ---
dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.1")
        bundledPlugin("com.intellij.java")
        testFramework(TestFrameworkType.Platform)
        pluginVerifier()
        zipSigner()
    }
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
    implementation("org.apache.commons:commons-text:1.13.0")
    implementation("org.apache.commons:commons-lang3:3.17.0")
}

// --- Tasks ---
tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }
    test {
        useJUnitPlatform()
    }

    patchPluginXml {
        // Provide the plain sanitized text; patchPluginXml will wrap it in CDATA.
        changeNotes.set(dynamicChangeNotes)
        // Adjust the compatibility range so the current build (e.g. 241.14494.240) is accepted.
        sinceBuild.set("241.0")
        untilBuild.set("999.*")
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

// --- Spotless (Optional) ---
spotless {
    kotlin {
        target("**/*.kt")
        ktlint("0.48.2").userData(
            mapOf("indent_size" to "4", "max_line_length" to "120")
        )
    }
}

// --- Detekt (Optional) ---
detekt {
    config.setFrom(files("detekt-config.yml"))
    buildUponDefaultConfig = true
    autoCorrect = true
}

// --- Jacoco (Optional) ---
jacoco {
    toolVersion = "0.8.12"
}