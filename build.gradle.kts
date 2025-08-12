import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.Task
import org.jetbrains.intellij.tasks.PublishPluginTask

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.24"
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "com.clipcraft"
version = "1.0.1"

repositories {
    mavenCentral()
}

intellij {
    // Update the platform version to a more recent one to fix the internal error
    version.set("2024.1.4")
    type.set("IC") // IntelliJ Community Edition
    plugins.set(listOf("com.intellij.java"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "17"
}

tasks.patchPluginXml {
    sinceBuild.set("241")   // Corresponds to 2024.1
    untilBuild.set("241.*") // Up to and including 2024.1.x
}

tasks.runIde {
    jvmArgs("-Didea.is.internal=true")
}

tasks.register<Task>("printVersion") {
    doLast {
        println(project.version)
    }
}

// Configure the publishPlugin task directly and explicitly
tasks.named<PublishPluginTask>("publishPlugin") {
    token.set(System.getenv("ORG_GRADLE_PROJECT_marketplaceToken"))
}