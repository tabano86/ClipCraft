plugins {
    java
    id("org.jetbrains.kotlin.jvm") version "1.9.21"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.21"
    id("org.jetbrains.intellij") version "1.17.4"
    id("pl.allegro.tech.build.axion-release") version "1.18.16"
    id("com.diffplug.spotless") version "6.20.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.7"
    id("com.github.ben-manes.versions") version "0.48.0"
    jacoco
}

group = "com.clipcraft"

// Configure SCM-based versioning.
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
                "BREAKING CHANGE" in message || Regex("^.*!:").containsMatchIn(message) -> ctx.currentVersion.incrementMajorVersion()
                message.startsWith("feat", ignoreCase = true) -> ctx.currentVersion.incrementMinorVersion()
                message.startsWith("fix", ignoreCase = true) -> ctx.currentVersion.incrementPatchVersion()
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
}

dependencies {
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-json:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
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
    kotlin {
        target("src/**/*.kt")
        ktlint("0.48.2")
    }
    java {
        target("src/**/*.java")
        googleJavaFormat("1.16.0")
    }
    yaml {
        target("**/*.yml", "**/*.yaml")
        jackson()
    }
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
