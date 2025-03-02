import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    java
    id("org.jetbrains.kotlin.jvm") version "1.9.23"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23"
    id("org.jetbrains.intellij.platform") version "2.2.1"
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
        // Example: Target IntelliJ IDEA Community 2023.3
        intellijIdeaCommunity("2023.3")
        bundledPlugin("com.intellij.java")
        testFramework(TestFrameworkType.Platform)
        pluginVerifier()
        zipSigner()
    }

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("org.apache.commons:commons-text:1.13.0")
    implementation("org.apache.commons:commons-lang3:3.17.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
}

kotlin {
    jvmToolchain(17)
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "17"
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
    register("printVersion") {
        doLast {
            println("Computed plugin version: $computedVersion")
        }
    }
    register("releasePlugin") {
        group = "release"
        description = "Publish plugin to Marketplace."
        dependsOn("patchPluginXml", "publishPlugin")
        doLast {
            println("Release complete. Version: $computedVersion")
        }
    }

    publishPlugin {
        channels = listOf("beta")
        token.set(providers.gradleProperty("intellijPublishingToken"))
        doFirst {
            val tokenValue = providers.gradleProperty("intellijPlatformPublishingToken").getOrElse("")
            if (tokenValue.isEmpty()) {
                throw GradleException("Token is empty!")
            }
        }
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
